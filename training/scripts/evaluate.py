# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Evaluate the passive check on the team captures and propose the pass and attack thresholds.

Reads the sequences written by ``capture_session.py`` (``captures/<subject>/<type>/<sequence>/``),
scores every frame with the reference pipeline, then:

1. splits the subjects into a calibration set and a test set, with no subject in both;
2. on the calibration set, proposes the attack threshold (largest value that keeps the bona fide
   presentations rejected as attacks at or below ``--target-bpcer``) and the pass threshold (smallest
   value that keeps the attacks accepted at or below ``--target-apcer``);
3. on the test set, replays each sequence through the passive window of section 4 of
   ``docs/decision-flow.md`` and reports APCER per attack type and its maximum, BPCER, ACER, the share
   of sequences sent to the active check, the equal error rate and the mean time per frame;
4. writes a DET curve to ``training/results/chart_det.png`` and prints a Markdown table for
   ``docs/test-results.md``.

Frame scores are cached in a CSV file (numbers only, no image), so the thresholds can be explored
again without running the models. Each team member scores the captures of their own machine and shares
only this file; ``--scores`` merges the files of several machines, so that no face image is moved.

Usage::

    python training/scripts/evaluate.py --captures captures [--test-subjects S03,S04] [--markdown out.md]
    python training/scripts/evaluate.py --score-only
    python training/scripts/evaluate.py --scores prince.csv elisee.csv tobi.csv
"""

import argparse
import csv
import json
import random
import sys
import time
from collections import defaultdict
from pathlib import Path

import cv2
import numpy as np

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "training"))

from liveness import quality  # noqa: E402
from liveness.minifas import MODELS_DIR, MiniFAS, load_manifest  # noqa: E402
from liveness.passive import (ATTACK, ATTACK_THRESHOLD, CHALLENGE, PASS_THRESHOLD, PASSED, PENDING,  # noqa: E402
                              PassiveWindow)
from liveness.yunet import YuNet, classifier_box  # noqa: E402

BONA_FIDE_TYPES = ("bona_fide", "action_blink", "action_smile", "action_turn_left", "action_turn_right")
RESULTS_DIR = REPO_ROOT / "training" / "results"
DEFAULT_CACHE = RESULTS_DIR / "frame_scores.csv"
CACHE_FIELDS = ["subject", "type", "sequence", "frame", "t_ms", "faces", "tip", "score", "ms"]


def is_bona_fide(capture_type):
    return capture_type in BONA_FIDE_TYPES


def sequences(captures):
    """``(subject, type, sequence dir)`` for every sequence folder that holds frames."""
    for subject_dir in sorted(p for p in captures.iterdir() if p.is_dir()):
        for type_dir in sorted(p for p in subject_dir.iterdir() if p.is_dir()):
            for sequence_dir in sorted(p for p in type_dir.iterdir() if p.is_dir()):
                if any(sequence_dir.glob("*.jpg")):
                    yield subject_dir.name, type_dir.name, sequence_dir


def frame_times(sequence_dir, frames):
    """Timestamps in ms from ``metadata.json``, or 100 ms apart when it has none."""
    metadata = sequence_dir / "metadata.json"
    if metadata.exists():
        times = json.loads(metadata.read_text(encoding="utf-8")).get("timestamps_ms")
        if times and len(times) == len(frames):
            return times
    return [100 * i for i in range(len(frames))]


def score_frames(captures, cache, step, max_yaw_deg):
    """Score every ``step``-th frame of every sequence and write the cache."""
    manifest = load_manifest()
    detector = YuNet(MODELS_DIR / manifest["yunet"]["file"])
    classifier = MiniFAS()
    rows = []
    for subject, capture_type, sequence_dir in sequences(captures):
        frames = sorted(sequence_dir.glob("*.jpg"))
        times = frame_times(sequence_dir, frames)
        for index in range(0, len(frames), step):
            image = cv2.imread(str(frames[index]))
            begin = time.perf_counter()
            row = {"subject": subject, "type": capture_type, "sequence": sequence_dir.name,
                   "frame": frames[index].name, "t_ms": times[index], "faces": 0, "tip": "", "score": ""}
            if image is None:
                row["tip"] = "INVALID_FRAME"
            else:
                faces = detector.detect(image)
                row["faces"] = len(faces)
                if len(faces) == 1:
                    result = quality.check(image, faces[0], max_yaw_deg=max_yaw_deg)
                    if result.passed:
                        row["score"] = "{:.6f}".format(classifier.score(image, classifier_box(faces[0].box)))
                    else:
                        row["tip"] = result.tip
            row["ms"] = "{:.2f}".format((time.perf_counter() - begin) * 1000)
            rows.append(row)
        print("{} / {} / {}: {} frames scored".format(subject, capture_type, sequence_dir.name,
                                                       len(range(0, len(frames), step))))
    cache.parent.mkdir(parents=True, exist_ok=True)
    with open(cache, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=CACHE_FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    return rows


def read_cache(cache):
    with open(cache, newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def check_unique_subjects(paths):
    """Stop when two score files use the same subject pseudonym: each machine has its own range."""
    owner = {}
    for path in paths:
        for subject in {row["subject"] for row in read_cache(path)}:
            if subject in owner and owner[subject] != path:
                sys.exit("subject {} appears in {} and {}: use distinct pseudonym ranges".format(
                    subject, owner[subject], path))
            owner[subject] = path


def group_sequences(rows):
    """Sequences as dictionaries: subject, type, frame times and scores (``None`` when not scored)."""
    grouped = defaultdict(list)
    for row in rows:
        grouped[(row["subject"], row["type"], row["sequence"])].append(row)
    result = []
    for (subject, capture_type, name), items in sorted(grouped.items()):
        items.sort(key=lambda r: float(r["t_ms"]))
        result.append({"subject": subject, "type": capture_type, "name": name,
                       "times": [float(r["t_ms"]) for r in items],
                       "scores": [float(r["score"]) if r["score"] else None for r in items],
                       "ms": [float(r["ms"]) for r in items]})
    return result


def first_decision(sequence, pass_threshold, attack_threshold):
    """Replay the sequence through the passive window. Returns the first decision, or ``PENDING``."""
    window = PassiveWindow(pass_threshold, attack_threshold)
    for t, score in zip(sequence["times"], sequence["scores"]):
        if score is None:
            continue
        window.add(t, score)
        decision = window.decision()
        if decision != PENDING:
            return decision
    return PENDING


def sequence_score(sequence):
    """Median of the scored frames of a sequence, for the DET curve and the threshold search."""
    scores = [s for s in sequence["scores"] if s is not None]
    return float(np.median(scores)) if scores else None


def propose_thresholds(calibration, target_apcer, target_bpcer):
    bona = np.array([sequence_score(s) for s in calibration if is_bona_fide(s["type"]) and sequence_score(s) is not None])
    attacks = np.array([sequence_score(s) for s in calibration
                        if not is_bona_fide(s["type"]) and sequence_score(s) is not None])
    if not len(bona) or not len(attacks):
        print("warning: the calibration set has no scored bona fide or attack sequence, default thresholds kept")
        return PASS_THRESHOLD, ATTACK_THRESHOLD
    candidates = np.round(np.arange(0.0, 1.0001, 0.01), 2)
    attack_threshold = max((t for t in candidates if np.mean(bona <= t) <= target_bpcer), default=0.0)
    pass_threshold = min((t for t in candidates if np.mean(attacks >= t) <= target_apcer), default=1.0)
    if pass_threshold <= attack_threshold:
        pass_threshold = min(1.0, round(attack_threshold + 0.01, 2))
    return float(pass_threshold), float(attack_threshold)


def metrics(test, pass_threshold, attack_threshold):
    """APCER per attack type, BPCER, ACER and the share of sequences sent to the active check."""
    outcomes = defaultdict(list)
    for sequence in test:
        outcomes[sequence["type"]].append(first_decision(sequence, pass_threshold, attack_threshold))
    bona = [d for t, items in outcomes.items() if is_bona_fide(t) for d in items]
    attacks = {t: items for t, items in outcomes.items() if not is_bona_fide(t)}
    apcer = {t: float(np.mean([d == PASSED for d in items])) for t, items in sorted(attacks.items())}
    bpcer = float(np.mean([d == ATTACK for d in bona])) if bona else float("nan")
    max_apcer = max(apcer.values()) if apcer else float("nan")
    return {
        "apcer": apcer,
        "max_apcer": max_apcer,
        "bpcer": bpcer,
        "acer": (max_apcer + bpcer) / 2,
        "bona_challenge": float(np.mean([d == CHALLENGE for d in bona])) if bona else float("nan"),
        "bona_pending": float(np.mean([d == PENDING for d in bona])) if bona else float("nan"),
        "attack_challenge": {t: float(np.mean([d == CHALLENGE for d in items])) for t, items in sorted(attacks.items())},
        "counts": {t: len(items) for t, items in sorted(outcomes.items())},
    }


def det_points(bona, attacks):
    """APCER and BPCER for every threshold, for a single-threshold classifier on the sequence scores."""
    thresholds = np.unique(np.concatenate([bona, attacks, [0.0, 1.0]]))
    apcer = np.array([np.mean(attacks >= t) for t in thresholds])
    bpcer = np.array([np.mean(bona < t) for t in thresholds])
    return thresholds, apcer, bpcer


def equal_error_rate(apcer, bpcer):
    index = int(np.argmin(np.abs(apcer - bpcer)))
    return float((apcer[index] + bpcer[index]) / 2)


def plot_det(apcer, bpcer, path):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from scipy.stats import norm

    clip = 1e-3
    figure, axis = plt.subplots(figsize=(5, 5))
    axis.plot(norm.ppf(np.clip(apcer, clip, 1 - clip)), norm.ppf(np.clip(bpcer, clip, 1 - clip)))
    ticks = [0.001, 0.01, 0.05, 0.2, 0.5, 0.8]
    axis.set_xticks(norm.ppf(ticks), ["{:g}%".format(t * 100) for t in ticks])
    axis.set_yticks(norm.ppf(ticks), ["{:g}%".format(t * 100) for t in ticks])
    axis.set_xlabel("APCER")
    axis.set_ylabel("BPCER")
    axis.set_title("DET curve, passive check, test subjects")
    axis.grid(True, alpha=0.3)
    figure.tight_layout()
    path.parent.mkdir(parents=True, exist_ok=True)
    figure.savefig(path, dpi=120)
    plt.close(figure)


def split_subjects(subjects, test_subjects, seed):
    if test_subjects:
        test = sorted(set(test_subjects) & set(subjects))
    else:
        shuffled = sorted(subjects)
        random.Random(seed).shuffle(shuffled)
        test = sorted(shuffled[:len(shuffled) // 2])
    calibration = sorted(set(subjects) - set(test))
    if not test or not calibration:
        sys.exit("need at least one calibration subject and one test subject, got {}".format(sorted(subjects)))
    return calibration, test


def percent(value):
    return "n/a" if value != value else "{:.1f} %".format(100 * value)


def markdown(report):
    m = report["metrics"]
    lines = [
        "Calibration subjects: {}. Test subjects: {}.".format(", ".join(report["calibration"]), ", ".join(report["test"])),
        "",
        "Proposed thresholds: pass {:.2f}, attack {:.2f} (targets on calibration: APCER {}, BPCER {}).".format(
            report["pass_threshold"], report["attack_threshold"], percent(report["target_apcer"]),
            percent(report["target_bpcer"])),
        "",
        "| Presentation type | Sequences | APCER (accepted as live) | Sent to the active check |",
        "| --- | --- | --- | --- |",
    ]
    for capture_type, value in m["apcer"].items():
        lines.append("| `{}` | {} | {} | {} |".format(capture_type, m["counts"][capture_type], percent(value),
                                                    percent(m["attack_challenge"][capture_type])))
    bona_count = sum(n for t, n in m["counts"].items() if is_bona_fide(t))
    lines += [
        "",
        "| Metric | Value |",
        "| --- | --- |",
        "| Bona fide sequences | {} |".format(bona_count),
        "| Maximum APCER | {} |".format(percent(m["max_apcer"])),
        "| BPCER (rejected as attack) | {} |".format(percent(m["bpcer"])),
        "| ACER | {} |".format(percent(m["acer"])),
        "| Bona fide sent to the active check | {} |".format(percent(m["bona_challenge"])),
        "| Bona fide without decision | {} |".format(percent(m["bona_pending"])),
        "| Equal error rate, single threshold | {} |".format(percent(report["eer"])),
        "| Mean time per frame (detection, quality, two classifiers) | {:.1f} ms |".format(report["mean_ms"]),
    ]
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--captures", type=Path, default=REPO_ROOT / "captures")
    parser.add_argument("--cache", type=Path, default=DEFAULT_CACHE)
    parser.add_argument("--rescore", action="store_true", help="ignore the cache and run the models again")
    parser.add_argument("--score-only", action="store_true",
                        help="score the local captures, write the cache and stop: the file to share with the team")
    parser.add_argument("--scores", type=Path, nargs="+",
                        help="merge these score files (from several machines) instead of reading local captures")
    parser.add_argument("--step", type=int, default=1, help="score one frame out of this number")
    parser.add_argument("--test-subjects", help="comma-separated pseudonyms of the test subjects")
    parser.add_argument("--seed", type=int, default=2026, help="seed of the subject split when none is given")
    parser.add_argument("--target-apcer", type=float, default=0.0)
    parser.add_argument("--target-bpcer", type=float, default=0.01)
    parser.add_argument("--max-yaw-deg", type=float, default=quality.MAX_YAW_DEG)
    parser.add_argument("--markdown", type=Path, help="also write the Markdown table to this file")
    parser.add_argument("--det", type=Path, default=RESULTS_DIR / "chart_det.png")
    args = parser.parse_args()

    if args.scores:
        rows = []
        for path in args.scores:
            rows += read_cache(path)
            print("frame scores read from {}".format(path))
        check_unique_subjects(args.scores)
    elif args.cache.exists() and not args.rescore and not args.score_only:
        rows = read_cache(args.cache)
        print("frame scores read from {}".format(args.cache))
    else:
        rows = score_frames(args.captures, args.cache, args.step, args.max_yaw_deg)
        if args.score_only:
            print("{} frames scored, written to {}: share this file, it holds no image".format(len(rows), args.cache))
            return
    all_sequences = group_sequences(rows)

    calibration_subjects, test_subjects = split_subjects(
        {s["subject"] for s in all_sequences},
        args.test_subjects.split(",") if args.test_subjects else None, args.seed)
    calibration = [s for s in all_sequences if s["subject"] in calibration_subjects]
    test = [s for s in all_sequences if s["subject"] in test_subjects]

    pass_threshold, attack_threshold = propose_thresholds(calibration, args.target_apcer, args.target_bpcer)
    bona = np.array([sequence_score(s) for s in test if is_bona_fide(s["type"]) and sequence_score(s) is not None])
    attacks = np.array([sequence_score(s) for s in test if not is_bona_fide(s["type"]) and sequence_score(s) is not None])
    eer = float("nan")
    if len(bona) and len(attacks):
        _, apcer_curve, bpcer_curve = det_points(bona, attacks)
        eer = equal_error_rate(apcer_curve, bpcer_curve)
        plot_det(apcer_curve, bpcer_curve, args.det)
        print("DET curve written to {}".format(args.det))

    report = {
        "calibration": calibration_subjects, "test": test_subjects,
        "pass_threshold": pass_threshold, "attack_threshold": attack_threshold,
        "target_apcer": args.target_apcer, "target_bpcer": args.target_bpcer,
        "metrics": metrics(test, pass_threshold, attack_threshold), "eer": eer,
        "mean_ms": float(np.mean([float(r["ms"]) for r in rows])) if rows else float("nan"),
    }
    table = markdown(report)
    print("\n" + table)
    if args.markdown:
        args.markdown.write_text(table + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
