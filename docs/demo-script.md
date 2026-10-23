# Demo script

Script of the demonstration video of the submission (#36), about 4 minutes 35, and plan of the live demonstration at the pitch (slide 12 of [pitch-outline.md](pitch-outline.md)). Every scene but one runs in the demo application ([../demo/README.md](../demo/README.md)): the liveness engine, the status pane and messages of the modified Registration Client, and the simulated SBI face device in webcam mode, which computes the passive score of each frame. Scene 9 shows the modified Registration Client itself on the MOSIP Collab environment, during the onboarding of the operator.

Narration is in English. The presenter appears on camera in the preview: the video goes to the shared storage of the submission and MOSIP may publish the submitted work, so the presenter must agree to appear. No other person appears without their consent.

## Before recording

- Build as in [../demo/README.md](../demo/README.md): engine, fork of the client, `mock-sbi`, then `mvn clean package` in `demo/`.
- Start the simulated device in webcam mode (`mosip.mock.sbi.face.source=webcam`, then `.\run_reg.bat` in `mock-sbi\target`), then the demo with `.\run_demo.ps1`. The window opens maximised.
- Artefacts: a printed photo of the presenter (A4 print from the capture kit) and a phone showing a photo of the presenter, screen brightness at maximum. A cut-out paper mask is not shown: it is a known limitation, stated in scene 9.
- Light the face from the front. Close the notifications, the mail and the chat applications.
- Screen recording: the Windows 11 recorder (Snipping Tool, video mode) or the Xbox Game Bar (`Win` + `Alt` + `R`), full screen, with the microphone.
- For scene 9, before recording: activate the WireGuard tunnel, start the client with `D:\mosip_decode\regclient-collab\run_client.ps1` ([dev-setup-windows.md](dev-setup-windows.md), sections 7, 8 and 11), sign in in English, open the onboarding and mark every fingerprint and both irises as exceptions. Leave the client on the face, behind the demo window. The password, the WireGuard window and its configuration never appear on screen.
- The demo and the client share the simulated device: press `Stop` in the demo before switching to the client. On Collab the device stream lasts about 10 s, so the check of scene 9 has to finish within it; if it fails, record the scene again.
- After the recording: close the terminal of the simulated device without saving its output, which holds the captured face records.

## Timeline

| Time | Scene | On screen | Narration |
| --- | --- | --- | --- |
| 0:00 to 0:20 | 1. The problem | Title card: "Face liveness and PAD for the MOSIP Registration Client, Best_Team, problem statement 04". Then the demo window, idle | Registration Client 1.2.0.2 shows the face stream as a preview and captures at the same moment. Nothing checks that the face belongs to a live person in front of the camera: a printed photo or a screen can be enrolled, or used to sign in as an operator. We added a liveness gate to the workflows of the client. |
| 0:20 to 0:35 | 2. What runs | Point at the preview, the status pane, the audit list and the status bar | On the left, the stream of an SBI face device, here our simulated device with the webcam: as MOSIP asked, the device itself scores each frame for liveness. On the right, the status pane of our modified client, with its real messages. Everything runs on this laptop. |
| 0:35 to 1:25 | 3. Resident, live face | Workflow "Resident face capture", `Start`, look at the camera, then follow the instructions: hold still, blink, turn the head to the side of the arrow and look back at the camera | The passive score of the device rejects clear attacks at once. Then come the random challenges, which every session goes through by default, with 15 seconds for each: a print or a still screen cannot perform them, and a recorded video cannot anticipate them. Only then does the client send the capture request: the signed image is checked against the face followed on the stream. The packet carries the challenges performed, and the audit trail records the result, with no image and no score. |
| 1:25 to 1:45 | 4. Why every session asks for challenges | Tick "Allow a passive pass (0.85)", `Start`, look at the camera, then untick it | With a passive pass allowed, a clear live face passes in a second or two without any action. On our second test subject, a glossy print scored as high as a live face, so the default requires the challenges in every session. It stays a configuration setting, per workflow. |
| 1:45 to 2:05 | 5. Printed photo | `Start`, hold the printed photo in front of the camera | A printed photo is refused. The screen says only that the verification could not be completed: it gives no hint about what was detected. The reason goes to the audit trail. The resident can try again. |
| 2:05 to 2:20 | 6. Phone screen | `Try again`, show the phone screen | A photo on a phone screen is refused the same way. |
| 2:20 to 2:50 | 7. Attempts exhausted, exception process | Attempts set to 1, `Start`, show the photo, then `Continue under exception` | After the last attempt, the resident is not blocked: the operator continues under the MOSIP exception process. The face is captured without a liveness decision, the registration is marked and audited, and a supervisor must authenticate before submission. |
| 2:50 to 3:20 | 8. Operator and supervisor | Workflow "Operator sign-in", language "Français", `Start`, live face. Then workflow "Supervisor authentication", attempts still at 1, show the photo | The same check protects the operator and supervisor authentication, here in French. There is no exception path for them: after the last attempt, they must use another sign-in method. |
| 3:20 to 3:55 | 9. On the MOSIP Collab environment | `Stop` in the demo, switch to the modified Registration Client, signed in on Collab, on the onboarding of the operator. `Scan` on the face: the compact pane, the challenges, then "Verified" and the captured face | Here is the modified Registration Client itself, connected to the MOSIP Collab environment, during the onboarding of an operator. The face the operator enrols, which later face sign-ins are compared with, goes through the same check, with the score of the device. The face is stored only once liveness is verified. |
| 3:55 to 4:15 | 10. Offline, configuration, diagnostic | Back to the demo. Turn off Wi-Fi in the taskbar, `Start`, live face. Show the configuration keys, tick "Diagnostic mode" | The check works offline with the same rules. Each setting is a configuration key of the client, set on the server and synchronised, with overrides per workflow. The diagnostic mode, off by default, shows the score that the device sent, for troubleshooting. |
| 4:15 to 4:35 | 11. Languages and close | Language "العربية" during a session, then the end card: repository links, "MPL-2.0" | Messages exist in English, French and Arabic, written right to left. The engine, the client changes, the simulated device and the test tools are open source in our repositories. Our measured results are preliminary, on two subjects, and the paper mask worn by another person is a known limitation. Thank you. |

## Live demonstration at the pitch

Three minutes at most, in this order: scene 3 (live face, with "Challenges per attempt" at 1), scene 5 (printed photo), scene 4 if time allows, then scene 8 in French. Scene 9 is added if the Collab environment answers on the day. The video is the fallback if the camera or the laptop fails. The images of [ui/pane/](ui/pane/) show the states that cannot be reproduced on stage.

## Checks after recording

- Length between 2 and 5 minutes, sound audible, text of the pane readable at 1080p.
- No face other than the presenter's, no notification, no personal window, no password and no WireGuard configuration.
- Upload to the shared storage of the submission (#38) and check that the link opens without signing in to the team's accounts.
