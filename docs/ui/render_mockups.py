# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Render each screen of mockups.html to a PNG file, with Chrome in headless mode.

Usage::

    python docs/ui/render_mockups.py --chrome "C:/Program Files/Google/Chrome/Application/chrome.exe"

The screen identifiers are read from mockups.html. Images are written next to it, as <id>.png.
A temporary browser profile is used, so that an open browser session is left untouched.
"""

import argparse
import re
import subprocess
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
PAGE = HERE / "mockups.html"
WIDTH, HEIGHT = 1280, 800


def screen_ids():
    return re.findall(r"\{ id: '([a-z0-9-]+)'", PAGE.read_text(encoding="utf-8"))


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--chrome", required=True, help="path to chrome.exe or msedge.exe")
    parser.add_argument("--only", help="render a single screen")
    args = parser.parse_args()

    ids = [args.only] if args.only else screen_ids()
    with tempfile.TemporaryDirectory() as profile:
        for screen in ids:
            target = HERE / "{}.png".format(screen)
            subprocess.run([
                args.chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                "--user-data-dir={}".format(profile), "--force-device-scale-factor=1",
                "--window-size={},{}".format(WIDTH, HEIGHT), "--virtual-time-budget=4000",
                "--screenshot={}".format(target), "{}?screen={}".format(PAGE.as_uri(), screen),
            ], check=True, capture_output=True)
            print("{} ({} bytes)".format(target.name, target.stat().st_size))


if __name__ == "__main__":
    main()
