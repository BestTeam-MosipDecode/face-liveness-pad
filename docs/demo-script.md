# Demo script

Script of the demonstration video of the submission (#36), about 4 minutes 30, and plan of the live demonstration at the pitch (slide 12 of [pitch-outline.md](pitch-outline.md)). Every scene runs in the demo application ([../demo/README.md](../demo/README.md)): the liveness engine, the status pane and messages of the modified Registration Client, and the simulated SBI face device in webcam mode. If a MOSIP environment becomes available, a shot of the modified client can replace the corresponding scene.

Narration is in English. The presenter appears on camera in the preview: the video goes to the shared storage of the submission and MOSIP may publish the submitted work, so the presenter must agree to appear. No other person appears without their consent.

## Before recording

- Build as in [../demo/README.md](../demo/README.md): engine, fork of the client, `mock-sbi`, then `mvn clean package` in `demo/`.
- Start the simulated device in webcam mode (`mosip.mock.sbi.face.source=webcam`, then `.\run_reg.bat` in `mock-sbi\target`), then the demo with `.\run_demo.ps1`. The window opens maximised.
- Artefacts: a printed photo of the presenter (A4 print from the capture kit) and a phone showing a photo of the presenter, screen brightness at maximum. A cut-out paper mask is not shown: it is a known limitation, stated in scene 9.
- Light the face from the front. Close the notifications, the mail and the chat applications.
- Screen recording: the Windows 11 recorder (Snipping Tool, video mode) or the Xbox Game Bar (`Win` + `Alt` + `R`), full screen, with the microphone.
- After the recording: close the terminal of the simulated device without saving its output, which holds the captured face records.

## Timeline

| Time | Scene | On screen | Narration |
| --- | --- | --- | --- |
| 0:00 to 0:25 | 1. The problem | Title card: "Face liveness and PAD for the MOSIP Registration Client, Best_Team, problem statement 04". Then the demo window, idle | Registration Client 1.2.0.2 shows the face stream as a preview and captures at the same moment. Nothing checks that the face belongs to a live person in front of the camera: a printed photo or a screen can be enrolled, or used to sign in as an operator. We added a liveness gate to the three workflows. |
| 0:25 to 0:45 | 2. What runs | Point at the preview, the status pane, the audit list and the status bar | On the left, the stream of an SBI face device, here the simulated device with the webcam. On the right, the status pane of our modified client, with its real messages. Everything runs on this laptop: no frame, score or image leaves it. |
| 0:45 to 1:10 | 3. Resident, live face | Workflow "Resident face capture", `Start`, look at the camera | The passive check scores the frames with two anti-spoofing models. A clear live face passes in a second or two, without asking anything. Only then does the client send the capture request: the signed image is checked against the face followed on the stream, and the audit trail records the result, with no image and no score. |
| 1:10 to 1:55 | 4. Ambiguous case, challenges | Tick "Always ask for a challenge", `Start`, follow the instructions: hold still, blink, turn the head | When the score is ambiguous, the active check starts on its own. For the demo we force it. The actions are drawn at random at each attempt, so a recorded video cannot anticipate them. The engine keeps scoring the frames during the actions and checks that the face stays the same. |
| 1:55 to 2:20 | 5. Printed photo | Untick "Always ask for a challenge", `Start`, hold the printed photo in front of the camera | A printed photo is refused in about one second. The screen says only that the verification could not be completed: it gives no hint about what was detected. The reason goes to the audit trail. The resident can try again. |
| 2:20 to 2:40 | 6. Phone screen | `Try again`, show the phone screen | A photo on a phone screen is refused the same way. |
| 2:40 to 3:10 | 7. Attempts exhausted, exception process | Attempts set to 1, `Start`, show the photo, then `Continue under exception` | After the last attempt, the resident is not blocked: the operator continues under the MOSIP exception process. The face is captured without a liveness decision, the registration is marked and audited, and a supervisor must authenticate before submission. |
| 3:10 to 3:45 | 8. Operator and supervisor | Workflow "Operator sign-in", language "Français", `Start`, live face. Then workflow "Supervisor authentication", attempts still at 1, show the photo | The same check protects the operator and supervisor authentication, here in French. There is no exception path for them: after the last attempt, they must use another sign-in method. |
| 3:45 to 4:05 | 9. Offline, configuration, diagnostic | Turn off Wi-Fi in the taskbar, `Start`, live face. Show the configuration keys, tick "Diagnostic mode" | The check works offline with the same rules. Each setting is a configuration key of the client, set on the server and synchronised, with overrides per workflow. The diagnostic mode, off by default, shows the score for troubleshooting. |
| 4:05 to 4:30 | 10. Languages and close | Language "العربية" during a session, then the end card: repository links, "MPL-2.0" | Messages exist in English, French and Arabic, written right to left. The engine, the client changes, the simulated device and the test tools are open source in our repositories. Our measured results are preliminary, on one subject, and the paper mask worn by another person is a known limitation. Thank you. |

## Live demonstration at the pitch

Three minutes at most, in this order: scene 3 (live face), scene 4 (one challenge, with "Challenges per attempt" at 1), scene 5 (printed photo), then scene 8 in French if time allows. The video is the fallback if the camera or the laptop fails. The images of [ui/pane/](ui/pane/) show the states that cannot be reproduced on stage.

## Checks after recording

- Length between 2 and 5 minutes, sound audible, text of the pane readable at 1080p.
- No face other than the presenter's, no notification, no personal window.
- Upload to the shared storage of the submission (#38) and check that the link opens without signing in to the team's accounts.
