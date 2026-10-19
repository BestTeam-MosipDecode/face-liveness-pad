# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Run the replay scenarios of the simulated device without manual action (issue #30).

For each scenario the script writes ``application.properties`` in the device folder, starts the device
in ``replay`` mode on a recorded sequence, calls it as the Registration Client does (MOSIPDINFO, MOSIPDISC,
STREAM, RCAPTURE with the requested SBI specification version), compares what it sees with the expected
behaviour, stops the device and prints one Markdown row. Recorded sequences hold faces and stay outside
the repository; the script reads them and writes nothing but the properties file and the report.

Scenarios come from a JSON file::

    [{"name": "live face", "folder": "captures/S01/bona_fide/normal_20261019-101500", "expect": "stream"},
     {"name": "disconnection", "folder": "...", "end": "stop", "expect": "stream-ends"},
     {"name": "corrupted frames", "folder": "...", "corrupt_every": 3, "expect": "corrupted"},
     {"name": "frozen image", "folder": "...", "end": "hold", "expect": "frozen"}]

``expect`` is ``stream`` (frames change and RCAPTURE succeeds), ``frozen`` (frames stop changing),
``stream-ends`` (the stream closes before the requested time) or ``corrupted`` (some frames are not complete
JPEG files). Each scenario runs once per SBI specification version given with ``--spec`` (default 0.9.5 and
1.0, the two versions supported by Registration Client 1.2.0.2) and once per device configuration given
with ``--fps`` (default: recorded timestamps, then a fixed 5 frames per second).

Usage::

    python mock-sbi/scripts/replay_scenarios.py --scenarios scenarios.json [--device mock-sbi/target]
        [--port 4501] [--seconds 4] [--markdown docs/replay-results.md]
"""

import argparse
import json
import os
import socket
import subprocess
import sys
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import check_device  # noqa: E402

REPO_ROOT = Path(__file__).resolve().parents[2]
DEVICE_DIR = REPO_ROOT / "mock-sbi" / "target"
JAR = "mock-mds-1.2.0.2.jar"
MAIN = "io.mosip.mock.sbi.test.TestMockSBI"
START_TIMEOUT_S = 60
REPLAY_KEYS = ("mosip.mock.sbi.face.source", "mosip.mock.sbi.face.replay.folder", "mosip.mock.sbi.face.replay.fps",
               "mosip.mock.sbi.face.replay.end", "mosip.mock.sbi.face.replay.corrupt.every")


def write_properties(device_dir, folder, fps, end, corrupt_every):
    """Rewrite application.properties of the device with the replay keys of the scenario."""
    path = device_dir / "application.properties"
    lines = [line for line in path.read_text(encoding="utf-8").splitlines()
             if not line.split("=")[0].strip() in REPLAY_KEYS]
    lines += ["mosip.mock.sbi.face.source=replay",
              "mosip.mock.sbi.face.replay.folder={}".format(Path(folder).resolve().as_posix()),
              "mosip.mock.sbi.face.replay.fps={}".format(fps),
              "mosip.mock.sbi.face.replay.end={}".format(end),
              "mosip.mock.sbi.face.replay.corrupt.every={}".format(corrupt_every)]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def start_device(device_dir, port, log):
    classpath = JAR + (";" if os.name == "nt" else ":") + "lib" + os.sep + "*"
    process = subprocess.Popen(
        ["java", "-cp", classpath, MAIN, "mosip.mock.sbi.device.purpose=Registration", "mosip.mock.sbi.biometric.type=Face"],
        cwd=str(device_dir), stdout=log, stderr=subprocess.STDOUT)
    deadline = time.monotonic() + START_TIMEOUT_S
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError("the device stopped at startup, see its log")
        try:
            status, _ = check_device.call(port, "MOSIPDINFO", "/info", timeout=2)
            if status == 200:
                return process
        except (OSError, ValueError):
            time.sleep(0.5)
    process.kill()
    raise RuntimeError("the device did not answer on port {} within {} s".format(port, START_TIMEOUT_S))


def stop_device(process):
    process.terminate()
    try:
        process.wait(5)
    except subprocess.TimeoutExpired:
        process.kill()


def stream_observation(port, device_id, seconds):
    """Frames seen on the stream for ``seconds``: count, distinct frames, incomplete JPEGs, early end."""
    body = json.dumps({"deviceId": device_id, "deviceSubId": "0", "timeout": "10000"}).encode()
    sock = socket.create_connection((check_device.HOST, port), timeout=10)
    sock.sendall(b"STREAM /stream HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: "
                 + str(len(body)).encode() + b"\r\n\r\n" + body)
    stream = sock.makefile("rb")
    frames, distinct, incomplete, ended = 0, set(), 0, False
    start = time.monotonic()
    while time.monotonic() - start < seconds:
        try:
            frame = check_device.read_frame(stream)
        except (OSError, ValueError):
            frame = None
        if frame is None:
            ended = True
            break
        frames += 1
        distinct.add(hash(frame))
        if not (frame.startswith(b"\xff\xd8") and frame.rstrip(b"\r\n").endswith(b"\xff\xd9")):
            incomplete += 1
    sock.close()
    return {"frames": frames, "distinct": len(distinct), "incomplete": incomplete, "ended": ended,
            "seconds": time.monotonic() - start}


def capture_result(port, device_id, spec_version):
    """RCAPTURE with the given specification version: the checks of check_device, or the error."""
    request_spec = spec_version
    try:
        # check_device builds a 0.9.5 request; replay the same checks with the requested version.
        transaction_id = str(uuid.uuid4())
        request = {
            "env": "Developer", "purpose": "Registration", "specVersion": request_spec, "timeout": 10000,
            "captureTime": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
            "transactionId": transaction_id,
            "bio": [{"type": "Face", "count": 1, "exception": [], "requestedScore": 40,
                     "deviceId": device_id, "deviceSubId": "0", "previousHash": ""}],
        }
        _, response = check_device.call(port, "RCAPTURE", "/capture", request)
        biometric = response["biometrics"][0]
        if biometric["error"]["errorCode"] != "0":
            return "error {}".format(biometric["error"]["errorCode"])
        data = check_device.jws_payload(biometric["data"])
        iso = check_device.b64url_decode(data["bioValue"])
        ok = (iso[:8] == b"FAC\x00030\x00" and data["transactionId"] == transaction_id
              and biometric["specVersion"] == request_spec and bool(data.get("qualityScore")))
        return "ISO record, spec {} echoed".format(request_spec) if ok else "fields differ from the request"
    except (OSError, ValueError, KeyError) as error:
        return "failed: {}".format(error)


def verdict(expect, observation, capture):
    if observation["frames"] == 0:
        return expect == "stream-ends"
    if expect == "stream":
        return observation["distinct"] > 1 and observation["incomplete"] == 0 and not observation["ended"] \
            and capture.startswith("ISO record")
    if expect == "frozen":
        return observation["distinct"] == 1 and not observation["ended"]
    if expect == "stream-ends":
        return observation["ended"]
    if expect == "corrupted":
        return observation["incomplete"] > 0
    raise ValueError("unknown expectation {}".format(expect))


def run_scenario(scenario, spec_version, fps, args, log):
    write_properties(args.device, scenario["folder"], fps, scenario.get("end", "loop"), scenario.get("corrupt_every", 0))
    process = start_device(args.device, args.port, log)
    try:
        _, devices = check_device.call(args.port, "MOSIPDISC", "/device", {"type": "Face"})
        device_id = devices[0]["deviceId"]
        observation = stream_observation(args.port, device_id, args.seconds)
        time.sleep(0.5)
        capture = capture_result(args.port, device_id, spec_version)
    finally:
        stop_device(process)
    ok = verdict(scenario["expect"], observation, capture)
    seen = "{} frames, {} distinct, {} incomplete{}".format(
        observation["frames"], observation["distinct"], observation["incomplete"],
        ", stream ended after {:.1f} s".format(observation["seconds"]) if observation["ended"] else "")
    return ok, seen, capture


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--scenarios", type=Path, required=True, help="JSON file describing the scenarios")
    parser.add_argument("--device", type=Path, default=DEVICE_DIR, help="folder of the built device")
    parser.add_argument("--port", type=int, default=4501)
    parser.add_argument("--seconds", type=float, default=4, help="length of the STREAM observation")
    parser.add_argument("--spec", nargs="+", default=["0.9.5", "1.0"], help="SBI specification versions")
    parser.add_argument("--fps", nargs="+", default=["0", "5"],
                        help="device configurations: replay rates, 0 for the recorded timestamps")
    parser.add_argument("--markdown", type=Path, help="write the table to this file")
    args = parser.parse_args()

    scenarios = json.loads(args.scenarios.read_text(encoding="utf-8"))
    if not (args.device / JAR).exists():
        sys.exit("device not built: {} not found".format(args.device / JAR))
    rows = ["| Scenario | SBI version | Device configuration | Expected | Observed on the stream | RCAPTURE | Result |",
            "| --- | --- | --- | --- | --- | --- | --- |"]
    failures = 0
    with open(args.device / "replay_scenarios.log", "w", encoding="utf-8") as log:
        for scenario in scenarios:
            for spec_version in args.spec:
                for fps in args.fps:
                    config = "recorded timestamps" if fps == "0" else "{} frames per second".format(fps)
                    try:
                        ok, seen, capture = run_scenario(scenario, spec_version, fps, args, log)
                    except (RuntimeError, OSError, ValueError, KeyError, IndexError) as error:
                        ok, seen, capture = False, "error: {}".format(error), "not run"
                    failures += not ok
                    row = "| {} | {} | {} | {} | {} | {} | {} |".format(
                        scenario["name"], spec_version, config, scenario["expect"], seen, capture, "pass" if ok else "FAIL")
                    rows.append(row)
                    print(row, flush=True)
    table = "\n".join(rows)
    if args.markdown:
        args.markdown.write_text(table + "\n", encoding="utf-8")
    print("\n{} of {} runs passed".format(len(rows) - 2 - failures, len(rows) - 2))
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
