# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Check a running simulated face device the way Registration Client 1.2.0.2 calls it.

The script sends MOSIPDINFO and MOSIPDISC, reads the STREAM with the same parsing as
``Streamer.retrieveNextImage`` of the client, then sends RCAPTURE and checks the response
fields that the client checks. It does not verify the signatures, which needs the trust chain
of a MOSIP server.

Usage::

    python mock-sbi/scripts/check_device.py [--port 4501] [--seconds 3] [--save-frame frame.jpg]
"""

import argparse
import base64
import hashlib
import http.client
import json
import socket
import sys
import time
import uuid
from datetime import datetime, timezone

HOST = "127.0.0.1"


def b64url_decode(text):
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def jws_payload(token):
    return json.loads(b64url_decode(token.split(".")[1]))


def call(port, method, path, body=None, timeout=30):
    conn = http.client.HTTPConnection(HOST, port, timeout=timeout)
    conn.request(method, path, body=json.dumps(body) if body is not None else None)
    response = conn.getresponse()
    data = response.read()
    conn.close()
    return response.status, json.loads(data)


def read_frame(stream):
    """Same steps as Streamer.retrieveNextImage: Content-Length, first 0xFF, then n + 1 bytes."""
    header = bytearray()
    while b"Content-Length:" not in header[1:]:
        byte = stream.read(1)
        if not byte:
            return None
        header += byte
    digits = bytearray()
    while True:
        byte = stream.read(1)
        if not byte:
            return None
        if byte in (b"\r", b"\n"):
            break
        digits += byte
    length = int(digits.decode().replace(" ", ""))
    while True:
        byte = stream.read(1)
        if not byte:
            return None
        if byte == b"\xff":
            break
    rest = stream.read(length)
    if len(rest) < length:
        return None
    return b"\xff" + rest[:length - 1]


def check_stream(port, device_id, seconds, save_frame):
    body = json.dumps({"deviceId": device_id, "deviceSubId": "0", "timeout": "10000"}).encode()
    sock = socket.create_connection((HOST, port), timeout=10)
    sock.sendall(b"STREAM /stream HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: "
                 + str(len(body)).encode() + b"\r\n\r\n" + body)
    stream = sock.makefile("rb")
    frames, first, last = 0, None, None
    start = time.monotonic()
    while time.monotonic() - start < seconds:
        frame = read_frame(stream)
        if frame is None:
            break
        frames += 1
        first = first or frame
        last = frame
    elapsed = time.monotonic() - start
    sock.close()
    if not frames:
        raise AssertionError("no frame received")
    if not (last.startswith(b"\xff\xd8") and last.rstrip(b"\r\n").endswith(b"\xff\xd9")):
        raise AssertionError("last frame is not a complete JPEG")
    if save_frame:
        with open(save_frame, "wb") as f:
            f.write(last)
    changing = "frames change" if first != last else "frames identical"
    return f"{frames} frames in {elapsed:.1f} s ({frames / elapsed:.0f} per second), {changing}"


def check_capture(port, device_id):
    transaction_id = str(uuid.uuid4())
    request = {
        "env": "Developer", "purpose": "Registration", "specVersion": "0.9.5", "timeout": 10000,
        "captureTime": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "transactionId": transaction_id,
        "bio": [{"type": "Face", "count": 1, "exception": [], "requestedScore": 40,
                 "deviceId": device_id, "deviceSubId": "0", "previousHash": ""}],
    }
    _, response = call(port, "RCAPTURE", "/capture", request)
    biometric = response["biometrics"][0]
    if biometric["error"]["errorCode"] != "0":
        raise AssertionError(f"capture error {biometric['error']}")
    data = jws_payload(biometric["data"])
    iso = b64url_decode(data["bioValue"])
    checks = {
        "ISO 19794-5 header": iso[:8] == b"FAC\x00030\x00",
        "transactionId echoed": data["transactionId"] == transaction_id,
        "specVersion echoed": biometric["specVersion"] == "0.9.5",
        "qualityScore present": bool(data.get("qualityScore")),
        "timestamp within 5 minutes": abs((datetime.now(timezone.utc) - datetime.strptime(
            data["timestamp"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)).total_seconds()) < 300,
        "hash chain": biometric["hash"] == hashlib.sha256(
            hashlib.sha256(b"").digest() + hashlib.sha256(iso).digest()).hexdigest().upper(),
    }
    failed = [name for name, ok in checks.items() if not ok]
    if failed:
        raise AssertionError("failed checks: " + ", ".join(failed))
    return f"ISO record of {len(iso)} bytes, quality {data['qualityScore']}, {len(checks)} checks passed"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--port", type=int, default=4501)
    parser.add_argument("--seconds", type=float, default=3)
    parser.add_argument("--save-frame", help="write the last streamed frame to this JPEG file")
    args = parser.parse_args()

    _, info = call(args.port, "MOSIPDINFO", "/info")
    device_info = jws_payload(info[0]["deviceInfo"])
    print(f"ok      MOSIPDINFO: {device_info['deviceStatus']}, specVersion {device_info['specVersion']}")

    _, devices = call(args.port, "MOSIPDISC", "/device", {"type": "Face"})
    device = devices[0]
    if device["deviceStatus"] != "Ready":
        raise SystemExit(f"FAILED  MOSIPDISC: device is {device['deviceStatus']}")
    print(f"ok      MOSIPDISC: device {device['deviceId']}, callbackId {device['callbackId']}")

    try:
        print(f"ok      STREAM: {check_stream(args.port, device['deviceId'], args.seconds, args.save_frame)}")
        time.sleep(1)
        print(f"ok      RCAPTURE: {check_capture(args.port, device['deviceId'])}")
    except AssertionError as error:
        print(f"FAILED  {error}")
        sys.exit(1)
    print("all checks passed")


if __name__ == "__main__":
    main()
