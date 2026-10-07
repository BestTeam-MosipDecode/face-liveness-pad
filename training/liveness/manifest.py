# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""Shared access to ``models.json``, the manifest of the models packaged in the Java engine.

Each script owns some entries and replaces only those, so that running one script never removes
the models written by another.
"""

import json
from pathlib import Path

SCHEMA_VERSION = 1
MODEL_SET_VERSION = "1.1.0"


def update_manifest(models_dir, entries, tools):
    """Insert or replace ``entries`` (matched by name) and record the ``tools`` used for them."""
    path = Path(models_dir) / "models.json"
    manifest = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"models": []}
    replaced = {entry["name"] for entry in entries}
    models = [model for model in manifest.get("models", []) if model["name"] not in replaced]
    for entry in entries:
        models.append(dict(entry, exportedWith=tools))
    manifest = {"schemaVersion": SCHEMA_VERSION, "modelSetVersion": MODEL_SET_VERSION, "models": models}
    path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return manifest
