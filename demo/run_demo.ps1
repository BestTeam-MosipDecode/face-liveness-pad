# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

# Start the face liveness demo built by "mvn clean package" in this folder.
# The simulated face device must already run in webcam mode (README.md of this folder).
# Arguments are passed on: --workflow=RESIDENT|OPERATOR|SUPERVISOR, --language=en|fr|ar, --start
$ErrorActionPreference = "Stop"
$jar = Join-Path $PSScriptRoot "target\liveness-demo-0.1.0-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
    throw "Build the demo first: mvn clean package, in $PSScriptRoot"
}
$lib = Join-Path $PSScriptRoot "target\lib\*"
& java -cp "$jar;$lib" io.mosip.registration.liveness.demo.DemoLauncher @args
