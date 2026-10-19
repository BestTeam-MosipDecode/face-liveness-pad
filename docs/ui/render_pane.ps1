# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

# Render the face liveness status pane of the Registration Client in each state, in English, French and Arabic.
# The pane, its status model and the message files are taken from the client sources of the fork; the images are
# written to docs/ui/pane/<language>/. Needs JDK 11, the engine installed in the local Maven repository (mvn install in
# liveness-engine), the client built once (section 4 of docs/dev-setup-windows.md), and the JavaFX 11.0.2 jars that
# the client build downloads. The sources compiled here take precedence over the classes of the built jars.
#
# Usage, from the root of face-liveness-pad:
#   .\docs\ui\render_pane.ps1 [-Client ..\registration-client\registration]
param([string]$Client = (Join-Path $PSScriptRoot "..\..\..\registration-client\registration"))

$ErrorActionPreference = "Stop"
$Client = (Resolve-Path $Client).Path
$m2 = Join-Path $env:USERPROFILE ".m2\repository"
$jars = @(
    "$m2\io\mosip\registration\liveness-engine\0.1.0-SNAPSHOT\liveness-engine-0.1.0-SNAPSHOT.jar",
    "$m2\org\openjfx\javafx-base\11.0.2\javafx-base-11.0.2-win.jar",
    "$m2\org\openjfx\javafx-graphics\11.0.2\javafx-graphics-11.0.2-win.jar",
    "$m2\org\openjfx\javafx-controls\11.0.2\javafx-controls-11.0.2-win.jar",
    "$Client\registration-services\target\registration-services-1.2.0.2.jar",
    "$Client\registration-client\target\registration-client-1.2.0.2.jar"
)
foreach ($jar in $jars) {
    if (-not (Test-Path $jar)) { throw "missing: $jar" }
}
$classpath = $jars -join ";"
$services = "$Client\registration-services\src\main\java\io\mosip\registration\liveness"
$sources = @(
    "$services\LivenessStatusModel.java",
    "$services\LivenessMessages.java",
    "$Client\registration-client\src\main\java\io\mosip\registration\controller\liveness\LivenessStatusPane.java",
    (Join-Path $PSScriptRoot "RenderPane.java")
)
$resources = "$Client\registration-client\src\main\resources"
$build = Join-Path ([IO.Path]::GetTempPath()) ("render-pane-" + [Guid]::NewGuid())
New-Item -ItemType Directory $build | Out-Null
try {
    & javac -nowarn -d $build -cp $classpath @sources
    if ($LASTEXITCODE -ne 0) { throw "javac failed" }
    Copy-Item "$resources\liveness.css" $build
    & java -cp "$build;$classpath" io.mosip.registration.controller.liveness.RenderPane $resources (Join-Path $PSScriptRoot "pane")
    if ($LASTEXITCODE -ne 0) { throw "rendering failed" }
} finally {
    Remove-Item -Recurse -Force $build
}
