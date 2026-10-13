# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Download the MockMDS test keys and sample face files into ``mock-sbi/``.

These files are not stored in the repository: the keys are private keys, even if public test
material, and the samples contain a face image. They are taken from ``mosip/mosip-mock-services``
at tag ``v1.2.0.2`` and checked against the SHA-256 below. Existing files with the right checksum
are kept.

Usage::

    python mock-sbi/scripts/fetch_assets.py
"""

import hashlib
import sys
import urllib.parse
import urllib.request
from pathlib import Path

BASE_URL = "https://raw.githubusercontent.com/mosip/mosip-mock-services/v1.2.0.2/MockMDS/"
MOCK_SBI_DIR = Path(__file__).resolve().parents[1]

# Path relative to MockMDS and mock-sbi, SHA-256
ASSETS = [
    ("Biometric Devices/Face/Keys/mosipface.p12",
     "92275a1b67e62da4c68970825bdc1860682fe4d1632b7de7e7dc031fa868b673"),
    ("Biometric Devices/rootca.cer",
     "9ed8aa314c2c5ce0a344c82dc8fb27adb8cfbdfdca39e798cc2337cc8d7d6061"),
    ("Biometric Devices/Face/Stream Image/0.jpeg",
     "244e8d97f4abfebb7cf96f39ba1f3b48add8d2f056090c76e252cf753e5b0d57"),
    ("Profile/Default/Registration/Face.iso",
     "51d487d39d4fde9b4ae68f0b3fe971e6c8e732bc25e596b21c46db2707bc63af"),
    ("Profile/Default/Registration/Exception_Photo.iso",
     "ae641661df27f618d2c35127dc457b84386b929407de39e73115b363c7d4bd66"),
    ("Profile/Automatic/Registration/0001Face.iso",
     "51d487d39d4fde9b4ae68f0b3fe971e6c8e732bc25e596b21c46db2707bc63af"),
    ("Profile/Automatic/Registration/0001Exception_Photo.iso",
     "ae641661df27f618d2c35127dc457b84386b929407de39e73115b363c7d4bd66"),
]


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def main():
    failures = 0
    for relative_path, expected in ASSETS:
        target = MOCK_SBI_DIR / relative_path
        if target.is_file() and sha256(target.read_bytes()) == expected:
            print(f"ok      {relative_path} (already present)")
            continue
        url = BASE_URL + urllib.parse.quote(relative_path)
        with urllib.request.urlopen(url, timeout=60) as response:
            data = response.read()
        if sha256(data) != expected:
            print(f"FAILED  {relative_path}: checksum mismatch")
            failures += 1
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        print(f"ok      {relative_path}")
    if failures:
        sys.exit(1)
    print("all assets present")


if __name__ == "__main__":
    main()
