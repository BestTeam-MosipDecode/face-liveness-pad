# Face liveness demo

A desktop application that runs the face liveness check of the project end to end, on one Windows workstation, without a MOSIP server:

- the frames come from the simulated SBI face device (`mock-sbi`, webcam mode), with the calls of the Registration Client: `MOSIPDISC`, `STREAM`, then `RCAPTURE` once liveness is verified;
- the liveness engine (`liveness-engine`) analyses them, with the policy chosen on screen;
- the status pane, its model and the messages of the modified Registration Client show each update, in English, French or Arabic, for the resident, operator or supervisor workflow.

The modified Registration Client needs a MOSIP server and a registered machine to start (`docs/dev-setup-windows.md`, sections 5 to 7). This demo shows the same components working together when no MOSIP environment is available, and serves for the video and the live demonstration of the submission.

## What it shows

| Scene | How |
| --- | --- |
| Live face | Resident workflow, default settings, look at the camera, then follow the challenges: since 21 October every session goes through them (#7) |
| Passive pass, for comparison | Tick "Allow a passive pass (0.85)": a clear live face passes on the passive score alone, the former default |
| Photo or screen attack | Present a printed photo or a phone screen to the camera: the generic failure message, then a new attempt |
| Attempts exhausted | Resident: "Continue under exception", then the capture without a liveness decision. Operator or supervisor: "Use another sign-in method" |
| Face device unavailable | Start without the simulated device, or stop it during an attempt |
| Configuration | The settings and the configuration keys of the client they correspond to |
| Diagnostic mode | Tick "Diagnostic mode": the score appears on the pane |
| Offline | Disconnect the network: nothing changes, all processing is local |
| Languages | Change the language during an attempt, Arabic is shown right to left |
| Audit | The audit events of each session, with no image and no score |

## Differences with the Registration Client

- No sign-in, no registration and no packet. The exception process is shown on the pane and the face is captured, but no registration is marked.
- `RCAPTURE` returns a JPEG 2000 image inside an ISO/IEC 19794-5 record, which the client decodes with the MOSIP `FaceDecoder` before the check of the captured image (`checkCapture`, in `LivenessCapture` of the fork). The demo embeds no JPEG 2000 decoder: it passes to `checkCapture` the last frame of the stream, which is the frame the simulated device encodes into the record. It shows the size of the record, then discards it.
- The signature of the `RCAPTURE` response is not verified. The client verifies it against the device trust (`MosipDeviceSpecificationHelper.validateJWTResponse`).
- Audit events are listed on screen instead of being written to the audit trail of the client.
- The settings apply to every workflow; the client also reads per-workflow overrides (`docs/configuration.md`).

## Requirements

JDK 11 and Maven 3.9, as for the rest of the project, and, in the local Maven repository:

1. the engine: `mvn clean install` in `liveness-engine/` (`docs/dev-setup-windows.md`, section 3);
2. the modified client: `mvn clean install "-Dgpg.skip" -DskipTests` in `registration-client/registration` of the fork (section 4);
3. the simulated device, built in `mock-sbi/target` (section 8).

## Run

Start the simulated device in webcam mode: set `mosip.mock.sbi.face.source=webcam` in `mock-sbi/target/application.properties`, then, in a terminal, from `mock-sbi/target`:

```powershell
.\run_reg.bat
```

Then build and start the demo, from this folder:

```powershell
mvn clean package
.\run_demo.ps1
```

Choose the workflow and the language, adjust the settings if needed, then press `Start`. `Stop` ends the session. The device is found on the first port from 4501 to 4510 that answers, as the client scans them.

## Privacy

Frames stay in memory: the preview shows the latest one and the engine analyses them, then they are dropped. The capture record is held only to show its size. The demo writes no file. The terminal of the simulated device prints its responses, including the captured face record: close it after the demonstration and do not save its output.
