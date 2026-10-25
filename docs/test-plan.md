# Test plan

Status on 13 October 2026: first version. This document fixes how the team records its own face captures, how the liveness check is measured on them, and which functional tests the complete system goes through. Results go to [test-results.md](test-results.md).

## 1. Scope and references

Two levels of evaluation are planned, as in ISO/IEC 30107-3, clause 7 (see [iso-30107-alignment.md](iso-30107-alignment.md), section 3.1):

| Level | What is tested | How | Issue |
| --- | --- | --- | --- |
| PAD subsystem | The passive score alone | Recorded sequences of section 5, scored by the Python reference pipeline | #7 |
| Full system | The Registration Client with the liveness engine: passive check, active check, captured image check, attempts | Live presentations and the simulated device, functional matrix of section 9 | #27, #28 |

Terms follow ISO/IEC 30107-1 and 30107-3:

| Term | Meaning in this plan |
| --- | --- |
| Bona fide presentation | A live subject presents their own face to the camera, as intended |
| Attack presentation | An artefact, or a person wearing one, is presented to the camera |
| Presentation attack instrument (PAI) | The object used in an attack presentation: a print, a screen, a mask |
| PAI species | A class of instruments made the same way, for example "glossy print". Error rates are computed per species |
| Subject | The person whose face is shown, live or in an artefact. Identified by a pseudonym |
| Attack presenter | The person who holds or wears the artefact |

Decisions, thresholds and reason codes come from [decision-flow.md](decision-flow.md). Personal data rules come from [security-privacy.md](security-privacy.md), section 4.2.

## 2. Subjects

- **Who.** The four team members, plus volunteers who agree to take part. Target: 8 subjects. Minimum: the 4 team members.
- **Consent.** Every subject signs the [consent form](consent/consent-form.md) before the first capture. Recordings of a subject who withdraws are deleted from every machine within 7 days.
- **Pseudonyms.** Each subject receives a pseudonym `S01`, `S02`... in the order of signature. The pseudonym is the only identifier in folder names, metadata, scripts and results. The link between name and pseudonym appears only on the signed form.
- **Skin tone.** Each subject may report a category on the Monk Skin Tone scale (MST 1 to 10), chosen by the subject from the printed scale. It is optional. Results are grouped in three bands, MST 1 to 3, 4 to 7 and 8 to 10, so that no band points to one person when it has at least two subjects. A band with a single subject is reported as a count only.
- **Variety.** Volunteers are chosen to widen the range of skin tones, ages and the presence of glasses or a beard. Glasses and beard are noted per subject, by pseudonym.

The subject register (pseudonym, skin tone band, glasses, beard) holds no name. It is kept with the captures, outside the repository, and its aggregated counts are copied into [test-results.md](test-results.md).

## 3. Presentation types

The capture type code is the folder name used by `training/scripts/capture_session.py` (section 5.3).

### 3.1 Bona fide presentations

| Code | Presentation | Instruction to the subject |
| --- | --- | --- |
| `bona_fide` | Live face, looking at the camera | Sit naturally, look at the screen, small natural movements are fine |
| `action_blink` | Live face, then one or more blinks | Hold still 2 s, then blink when the preview says so |
| `action_smile` | Live face, then a smile | Hold still 2 s, then smile, then relax |
| `action_turn_left` | Live face, then a head turn to the subject's left and back | Hold still 2 s, then turn to your left and come back |
| `action_turn_right` | Live face, then a head turn to the subject's right and back | Hold still 2 s, then turn to your right and come back |

The action sequences serve to set the factors of the active check (decision flow, section 5.3, and #5). "Left" and "right" refer to the person. Frames are saved as the camera delivers them, without mirroring. Only the preview is mirrored.

### 3.2 Attack presentations

Each artefact reproduces the face of a subject from a frontal photo of that subject, taken with a phone in good light, face filling about half of the photo. The photo is the "source photo" of the subject. It is stored with the captures and deleted with them.

| Code | PAI species | How the artefact is made | How it is presented |
| --- | --- | --- | --- |
| `print_matte` | Printed photo, matte | Source photo printed on plain A4 office paper (80 g/m²), colour laser or inkjet, face about life size | Held flat by the attack presenter, face in front of the camera at bona fide distance, slow movements |
| `print_glossy` | Printed photo, glossy | Source photo printed on glossy photo paper, 10 x 15 cm or A4, face as large as the paper allows | Same as matte. Tilt slightly so that reflections appear in some frames |
| `screen_phone` | Photo on a phone screen | Source photo shown full screen on a smartphone, brightness at maximum, auto-rotation off | Phone held in front of the camera, screen parallel to the lens |
| `screen_laptop` | Photo on a laptop screen | Source photo shown full screen on a laptop, brightness at maximum | Laptop placed in front of the camera, screen facing it, face at bona fide size |
| `replay_video` | Replayed video | 15 s video of the subject recorded with a phone, in which the subject blinks, smiles and turns the head left and right. Played full screen on a phone or a laptop, sound off | Played in a loop in front of the camera |
| `paper_mask` | Cut-out paper mask | Source photo printed life size on plain A4 paper, eyes and mouth cut out, optionally glued on card, elastic or held by hand | Worn by the attack presenter, whose eyes and mouth show through the holes. The presenter blinks and opens the mouth during the sequence |

The artefact register lists, for each artefact: an identifier (`A01`, `A02`...), the species, the subject pseudonym, the printer and paper, or the screen device, its size and brightness, or the video device. It holds no image. It is copied into [test-results.md](test-results.md), section 3, as required by ISO/IEC 30107-3, clause 10.2.

Attack presenters are team members. Where possible, the presenter is not the subject of the artefact. For `paper_mask`, the presenter must be another person than the subject, otherwise the test shows the subject's own eyes and mouth.

## 4. Capture conditions

### 4.1 Lighting

| Code | Condition | How it is set up |
| --- | --- | --- |
| `normal` | Even frontal light | Indoor room light, plus daylight or a lamp in front of the subject. No strong shadow on the face |
| `low` | Low light | Main lights off, only indirect light, for example a lamp at the side of the room or light from a corridor. The face is still visible on the preview |
| `backlight` | Strong light behind the subject | Subject in front of a bright window or a lamp placed behind the head, no frontal light |

If a phone light meter is available, the illuminance at the face is noted for each condition. It is optional.

### 4.2 Distance and position

- Bona fide: face 40 to 70 cm from the camera, as in front of a registration workstation. The face box must be at least 120 pixels wide in the frame (`quality.min_face_px`), which holds at 1280 x 720 at these distances.
- Attacks: artefact placed so that the face has about the same size in the frame as the live face.
- Camera fixed on the screen or on a stand, at eye level. It does not move during a sequence.

### 4.3 Hardware

For each session, the following is noted in the metadata of the sequences or in the session log: webcam model, requested and actual resolution, capture rate, computer and operating system. The webcam model is passed to the script with `--camera-model`. The resolution and the rate are read from the camera by the script.

Default capture: 1280 x 720. A second webcam, of lower quality, is used for part of the subjects if one is available, so that results are not tied to one camera.

## 5. Captures

### 5.1 Sequences per subject

| Code | Sequences per lighting condition | Lighting conditions | Sequences per subject |
| --- | --- | --- | --- |
| `bona_fide` | 2 | 3 | 6 |
| `action_blink`, `action_smile`, `action_turn_left`, `action_turn_right` | 1 each | 3 | 12 |
| `print_matte`, `print_glossy`, `screen_phone`, `screen_laptop`, `replay_video`, `paper_mask` | 1 each | 3 | 18 |
| Total | | | 36 |

Each sequence lasts 10 s. The camera runs at its own rate, about 30 frames per second, and the script saves 10 frames per second as JPEG (quality 95), which matches the analysis rate of the engine (`frame.sampling_ms` = 100). A sequence therefore holds about 100 frames.

| | Per subject | 8 subjects | 4 subjects (minimum) |
| --- | --- | --- | --- |
| Sequences | 36 | 288 | 144 |
| Bona fide sequences (`bona_fide` only) | 6 | 48 | 24 |
| Attack sequences | 18 | 144 | 72 |
| Attack sequences per species | 3 | 24 | 12 |
| Frames | about 3 600 | about 28 800 | about 14 400 |
| Disk space, about 150 KB per frame | about 0.5 GB | about 4.3 GB | about 2.2 GB |

A session lasts about one hour per subject, artefacts prepared in advance. A failed sequence (subject out of frame, artefact dropped) is aborted with `q` and recorded again.

### 5.2 Order of a session

1. Check the signed consent form and the pseudonym.
2. Set up the camera and note the hardware (section 4.3).
3. For each lighting condition, in the order `normal`, `low`, `backlight`: the bona fide sequences, the action sequences, then the attack sequences.
4. Fill the session log: date, pseudonym, presenters, artefact identifiers, lighting notes, sequences aborted.

### 5.3 Capture script

`training/scripts/capture_session.py` records one sequence per run:

```powershell
training\.venv\Scripts\python.exe training\scripts\capture_session.py --subject S01 --type bona_fide --light normal --camera-model "Logitech C270"
```

It writes to `captures/<subject>/<type>/<light>_<YYYYmmdd-HHMMSS>/`: the frames `frame_00000.jpg`, `frame_00001.jpg`... and `metadata.json` (subject pseudonym, type, light, camera, actual resolution and rate, start time, frame count, timestamp of each frame, script version, operating system). The script refuses a subject identifier that is not a pseudonym, and an output folder inside the repository that Git does not ignore. Text and countdown appear on the preview only, never on the saved frames. Details in [training/README.md](../training/README.md).

## 6. Splits

- **By identity.** A subject belongs to one split only, with all their sequences, bona fide and attack. Since every artefact shows the face of its subject, attack sequences follow the subject they show, not the presenter.
- **Calibration split.** Used to choose the pass and attack thresholds, the lighting and sharpness bounds and the action factors.
- **Test split.** Used once, with the frozen thresholds, to compute the published error rates.
- With 8 subjects: 4 in each split. With 4 subjects: 2 in each. The splits are balanced by skin tone band as far as the subjects allow.
- The split is written as a list of pseudonyms in `training/configs/splits.json` before any threshold is chosen, and is not changed afterwards. If thresholds are changed after a look at the test split, the results are labelled as such.

With so few subjects, a leave-one-subject-out run on all subjects may be reported in addition, labelled as such. It never replaces the fixed test split.

## 7. Metrics

Computed by `training/scripts/evaluate.py` (#7) for the PAD subsystem, and from the session outcomes for the full system (#28). Terms of ISO/IEC 30107-3, clause 13 (see [iso-30107-alignment.md](iso-30107-alignment.md), section 3.4).

### 7.1 PAD subsystem

One presentation is one sequence. Its score is computed as the engine does: frame scores of the frames that pass the quality checks, median over the first window of 3 000 ms holding at least 5 scored frames (decision flow, section 4). A sequence that never reaches such a window has no decision and counts in the non-response rate.

| Metric | Definition |
| --- | --- |
| APCER per species | Share of the attack sequences of one species whose score is at or above the threshold |
| Highest APCER | The highest APCER over the six species. The headline attack figure |
| BPCER | Share of the `bona_fide` sequences whose score is below the threshold |
| ACER | Mean of the highest APCER and BPCER. Reported only next to the per-species APCER, as a benchmark figure (see the alignment document) |
| DET curve | APCER (highest species) against BPCER for all thresholds, on a log scale. One curve per species on the same chart |
| Non-response rate | Share of sequences without a decision, bona fide and attack separately |
| Mean time per frame | Detection, landmarks and classifiers, in milliseconds, with the machine described |

At the proposed thresholds, the three outcomes of the passive check are reported for each type: share of sequences at or above `passive.threshold.pass` (pass), at or below `passive.threshold.attack` (attack), and in between (challenge). A bona fide sequence in the challenge zone is not a rejection in the full system, but it costs the user a challenge.

The action sequences are used for the factors of the active check (#5): share of sequences in which the requested action is detected, and no other action detected.

Each figure is given with its sample size: number of subjects, of artefacts and of sequences. BPCER is also given per lighting condition and per skin tone band.

### 7.2 Full system

| Metric | Definition |
| --- | --- |
| APCER per species and highest APCER | Share of attack attempts that end in `PASSED` with an accepted capture |
| BPCER | Share of bona fide sessions that end in `MAX_ATTEMPTS_EXCEEDED` |
| Challenge rate | Share of bona fide attempts that go to `ACTIVE_CHALLENGE` |
| Retry rate | Share of bona fide sessions needing more than one attempt |
| Mean passive decision time | From the first frame to `PASSED` without challenge |
| Mean challenge time | From `ACTIVE_CHALLENGE` to `PASSED` |
| Resources | CPU and memory of the client during a session, on a described machine |

## 8. Thresholds

The pass and attack thresholds are proposed from the calibration split (#7): the attack threshold so that no calibration attack sequence of the print and screen species falls above it where possible, the pass threshold so that the BPCER at that threshold stays low enough for most live users to pass without a challenge. The coordinator validates them before they are written as defaults. The test split is then scored once with the frozen values.

## 9. Functional test matrix

Each case is run in the three workflows (resident capture, operator authentication, supervisor authentication) and in two network conditions:

- **online**: workstation connected, normal synchronisation;
- **offline**: network cable unplugged and Wi-Fi off after the last synchronisation.

That gives six runs per case. Liveness runs locally, so the expected result is the same online and offline ([workflows.md](workflows.md), section 3). Default settings apply (decision flow, section 10), unless the case says otherwise. Observed results are recorded in [test-results.md](test-results.md), section 2.

| ID | Case | How | Expected | Observed |
| --- | --- | --- | --- | --- |
| F01 | Live face, good light | Team member, `normal` light, facing the camera | `PASSED` after the random challenges, which the default pass threshold of 1.00 requires in every session, captured image accepted, `LIVENESS_SUCCESS`. Resident: face added to the registration. Operator, supervisor: the comparison runs as before. Audit `LIVENESS_SESSION_STARTED`, `LIVENESS_PASSED` | |
| F02 | Ambiguous score leads to a challenge | Live face in `low` light, or test configuration with `passive.threshold.pass=0.99` | `ACTIVE_CHALLENGE`, audit `LIVENESS_ACTIVE_TRIGGERED`. Two challenges (three for the supervisor with the recommended setting), drawn at random, each detected. Then `PASSED` and capture accepted | |
| F03 | Action not performed | As F02, the subject does not perform the requested action, or turns the wrong way | Attempt `FAILED` with `CHALLENGE_TIMEOUT` after 15 s, or `CHALLENGE_FAILED` at once for a wrong turn. Messages `LIVENESS_FAILED`, `LIVENESS_TIP_FOLLOW_ACTION`, `LIVENESS_RETRY_AVAILABLE`. Attempt counted | |
| F04 | Printed photo | Matte and glossy prints of a team member | Never `PASSED`. `FAILED` with `PAD_ATTACK_DETECTED` (generic message `LIVENESS_GENERIC_FAILURE`), or a challenge that fails. Attempt counted | |
| F05 | Photo on a screen | Phone and laptop | As F04 | |
| F06 | Replayed video | Video of a team member blinking, smiling and turning | Never `PASSED`. `PAD_ATTACK_DETECTED`, or challenge fails with `CHALLENGE_TIMEOUT`, `CHALLENGE_FAILED` or `PAD_ATTACK_DETECTED` during the challenge | |
| F07 | No face | Camera facing an empty chair | Guidance `NO_FACE`, `LIVENESS_NO_FACE`. After 20 s (`passive.timeout_ms`), attempt `FAILED` with reason `NO_FACE` | |
| F08 | Two faces | Two people in the frame | Passive check: guidance `MULTIPLE_FACES`, no decision while it lasts. During a challenge: `FAILED` with `FACE_CONTINUITY_LOST` | |
| F09 | Face changed during a challenge | As F02, another person replaces the subject after the first challenge | `FAILED` with `FACE_CONTINUITY_LOST`. Attempt counted | |
| F10 | Device unplugged | Unplug the camera, or stop the simulated device, before the first frame and during a challenge | `DEVICE_ERROR`, `LIVENESS_DEVICE_UNAVAILABLE`, audit `LIVENESS_DEVICE_ERROR`. Before the first frame: no attempt used. After the first analysed frame: attempt counted | |
| F11 | Corrupted frame | Simulated device sends frames that do not decode in the middle of a stream | Frame ignored and counted as `INVALID_FRAME` in the diagnostics. State unchanged, the session continues and can pass | |
| F12 | Attempts exhausted | Three failed attempts in a row (for example F04 three times) | `MAX_ATTEMPTS_EXCEEDED`, audit `LIVENESS_MAX_ATTEMPTS`. Resident: `LIVENESS_MAX_ATTEMPTS`, exception process offered. Operator, supervisor: `LIVENESS_MAX_ATTEMPTS_AUTH`, authentication refused (`BLOCK`), no exception path | |
| F13 | Liveness disabled | `mosip.registration.face.liveness.enabled=false` | No liveness session, no liveness message or audit event. The client behaves as before the feature, scan button included | |
| F14 | Supervisor override | Resident: after F12, the operator continues under exception | Resident: face captured once more without a liveness decision, registration marked as a face liveness exception, supervisor authentication required at submission, with liveness on the supervisor's face. The operator's own account is refused as reviewer. Operator, supervisor workflows: no override offered | |

## 10. Data handling

- Captures are written only under `captures/`, ignored by Git, or a folder outside the repository. The capture script checks it.
- No frame, source photo, video or artefact image is committed, attached to an issue or a pull request, or shown in the video or the pitch. Before each commit, `git status` must show no file under `captures/` or `datasets/`.
- Folder names, metadata, logs and results use pseudonyms only. No name, no date of birth, no contact details.
- Captures are copied between team members only on encrypted storage or by hand on a USB drive kept by a team member, never through a public sharing service.
- Results published in the repository are aggregated figures and charts without faces (`chart_*.png` under `results/`).
- At the latest on 31 December 2026, or within 7 days of a withdrawal, the captures, source photos and replay videos of the subject are deleted from every machine and the printed artefacts are destroyed. The deletion is recorded in the work log, by pseudonym.
