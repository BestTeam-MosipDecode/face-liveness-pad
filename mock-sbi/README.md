# mock-sbi

Simulated L0/L1 face device for development and automated tests. It speaks the MOSIP Secure Biometric Interface (SBI), so the Registration Client uses it like a real device.

It is a fork of MockMDS from [mosip-mock-services](https://github.com/mosip/mosip-mock-services) `v1.2.0.2` (MPL 2.0), reduced to the face device. The study behind this choice is in [../docs/mock-sbi.md](../docs/mock-sbi.md).

Status: `static` and `webcam` modes available. `replay` and failure simulation come with #18.

## Modes

| Mode | Source of the frames | Captured image | Purpose |
| --- | --- | --- | --- |
| `static` | Fixed image of MockMDS | Prepared ISO record of MockMDS | Check that a frozen image is rejected |
| `webcam` | Webcam of the workstation | ISO 19794-5 record built from the last streamed frame | Live demonstration and manual tests |
| `replay` | Recorded video or image folder | Planned (#18) | Repeatable tests, including presentation attacks |

The mode is set by `mosip.mock.sbi.face.source` in `application.properties`, next to the jar.

| Key | Default | Role |
| --- | --- | --- |
| `mosip.mock.sbi.face.source` | `static` | `static` or `webcam` |
| `mosip.mock.sbi.face.webcam.index` | `0` | Webcam number, `0` for the default camera |
| `mosip.mock.sbi.face.webcam.width`, `...height` | `640`, `480` | Requested resolution. The camera may pick the closest one it supports |
| `mosip.mock.sbi.face.webcam.jpeg.quality` | `90` | JPEG quality of the streamed frames, 0 to 100 |

## Quick start

Requirements: JDK 11, Maven 3.9, Python 3.10.

```
python mock-sbi/scripts/fetch_assets.py
cd mock-sbi
mvn -DskipTests -Dgpg.skip clean install
cd target
```

Set the mode in `target/application.properties`, then start the device from `target/`:

- Windows: `run_reg.bat` in the command prompt, `.un_reg.bat` in PowerShell
- macOS and Linux: `java -cp "mock-mds-1.2.0.2.jar:lib/*" io.mosip.mock.sbi.test.TestMockSBI "mosip.mock.sbi.device.purpose=Registration" "mosip.mock.sbi.biometric.type=Face"`

The device listens on the first free port from 4501. Check it from the repository root with:

```
python mock-sbi/scripts/check_device.py
```

`--save-frame <file>` writes the last streamed frame to a JPEG file, to see what the client receives. Choose a path outside the repository: in `webcam` mode, the frame shows your face.

On macOS, the first use of the webcam asks for camera access for the terminal.

### Windows

Checked on Windows 11 with JDK 11 and Maven 3.9, in PowerShell from the repository root:

```powershell
python mock-sbi\scriptsetch_assets.py
cd mock-sbi
mvn -DskipTests "-Dgpg.skip" clean install
cd target
.un_reg.bat
```

- `java -version` must show 11 in the same terminal before `mvn` and `run_reg.bat`.
- PowerShell splits `-Dgpg.skip` at the dot, and Maven then stops with `Unknown lifecycle phase ".skip"`: keep the quotes.
- PowerShell runs a script of the current folder only with `.\` in front of its name.
- `webcam` mode: until #17 opens the camera with DirectShow, the camera takes about 20 s to open on Windows, and the first STREAM of each capture can time out.

The test keys and sample images fetched by `fetch_assets.py` are public MOSIP test material. They are kept out of Git and must never be used outside development and tests. Recorded sequences that contain faces are kept outside the repository too.
