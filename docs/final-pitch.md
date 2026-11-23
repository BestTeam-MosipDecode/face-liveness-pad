# Final pitch

Plan of the live presentation of Level 2, the final round of MOSIP Decode 2026, after the team cleared Level 1. A panel of MOSIP experts and industry professionals evaluates it: ten minutes of presentation, then five minutes of questions. MOSIP prefers a working prototype or a demo video to a deck alone.

- Slot: Thursday 12 November 2026, 6:45 PM to 7:00 PM IST, confirmed by MOSIP on 11 November: 13:15 to 13:30 UTC, 14:15 to 14:30 in Cotonou.
- Session: a Zoom meeting of the panel, which starts at 5:30 PM IST (13:00 in Cotonou); each team joins at its slot. The link was sent by email to the members listed in the reply of 10 November and stays out of this repository.
- Camera: the reference workstation has one camera, and Windows gives it to one application at a time. Zoom keeps its video off while the demo runs, so that the simulated device can open the camera; the panel sees the presenter in the preview of the demo. Video can be turned on for the questions, once the demo is stopped.
- Presenter: Temitayo Gbolahan, at the reference workstation, who also runs the demo with their own face. Elisée Atonde, coordinator, prepares the workstation and stays in support.
- The deck, the demo and the scenes are those of [pitch-outline.md](pitch-outline.md) and [demo-script.md](demo-script.md).

## Running order

| Time | Slide or scene | What to say |
| --- | --- | --- |
| 0:00 to 0:30 | Slide 1, cover | Team, problem statement 04, Registration Client 1.2.0.2, the three workflows, open source under MPL-2.0. |
| 0:30 to 1:15 | Slide 2, the problem | The client shows the face stream as a preview and captures at the same moment: nothing checks that the face is live. A print, a screen or a video can be enrolled or open an operator session. Centres can be offline. |
| 1:15 to 2:15 | Slide 4, the solution | The device scores each frame, as MOSIP asked, and clear attacks are refused at once. Random challenges in every session. Capture only after `PASSED`, and the signed image must match the face followed. After the last attempt: the exception process for a resident, another sign-in method for an operator or a supervisor. |
| 2:15 to 2:45 | Slide 6, decision flow | Median of the frame scores, attack threshold 0.30, pass threshold 1.00, 15 s per challenge, the face followed from frame to frame. |
| 2:45 to 3:15 | Slide 7, workflows | Resident capture, operator and supervisor authentication, onboarding. The packet carries the check of each face, with no image and no score. 17 configuration keys, per workflow. |
| 3:15 to 3:45 | Slide 11, results | Two subjects, 67 sequences, preliminary. No live sequence refused. Glossy prints scored like live faces on the second subject, hence the challenges in every session. About 56 ms per frame on a laptop processor. |
| 3:45 to 4:45 | Slide 12, demo: scene 3 | Live face in the resident workflow, "Challenges per attempt" at 1: the device score, the challenge, then the captured image checked. |
| 4:45 to 5:15 | Demo: scene 5 | Printed photo: refused with the generic message, the reason goes to the audit trail. |
| 5:15 to 5:35 | Demo: scene 6 | Phone screen: refused the same way. |
| 5:35 to 6:15 | Demo: scene 8 | Operator sign-in in French: the same check protects operators and supervisors, with no exception path. |
| 6:15 to 6:55 | Clip of scene 9 | The modified Registration Client on MOSIP Collab, operator onboarding: the reference face goes through the same check. Saving the onboarding then fails in IDA with an internal error that MOSIP is checking. |
| 6:55 to 7:15 | Spare | Scene 4, the passive pass at 0.85 for comparison, if the demo is on time. Otherwise go on. |
| 7:15 to 8:15 | Slide 13, limits | Paper mask worn by another person, glossy prints, two subjects, the Collab onboarding, stream injection outside PAD. |
| 8:15 to 8:45 | Slide 14, impact | The gate in the three workflows with no new hardware, offline, vendor-neutral through SBI, reusable engine, MPL-2.0. Thank you. |
| 8:45 to 10:00 | Margin | Switching between windows, one demo retry. Stop at 10:00 even if a scene was skipped. |

If the camera, the device or the laptop fails during the demo, play the demo video from 0:35 and comment over it.

## Demo controls

| Scene | In the demo window |
| --- | --- |
| Before the session | "Challenges per attempt" at 1, "Attempts" at 3, "Allow a passive pass (0.85)" and "Diagnostic mode" unticked, language English |
| 3, live face | Workflow "Resident face capture", `Start`, look at the camera, then follow the instructions: hold still, blink or turn the head to the side of the arrow and look back at the camera |
| 5, printed photo | `Start`, then hold the printed photo in front of the camera |
| 6, phone screen | `Try again`, then show the phone screen, brightness at maximum |
| 8, operator in French | Workflow "Operator sign-in", language "Français", `Start`, live face |
| 4, comparison, if time allows | Tick "Allow a passive pass (0.85)", `Start`, look at the camera, then untick it |
| Clip of scene 9 | `Stop` if a session runs, then play `scene9_collab.mp4` |

## Material

Kept outside the repository, in `captures/final_pitch/`, ignored by Git:

- `Face Liveness Pitch - final.pdf`: the nine slides of the running order, in that order (1, 2, 4, 6, 7, 11, 12, 13, 14), for a full-screen presentation without skipping pages.
- `scene9_collab.mp4`: scene 9 of the demo video, 28 s, with its subtitles.

Beside it:

- The full deck, 14 slides, open in a second window for the questions: slide 5 (architecture), 8 (what the user sees), 9 (security and privacy), 10 (stack).
- The demo video of the submission, 4 min 35 s, as the fallback.
- A printed photo of the presenter and a phone showing a photo of the presenter, brightness at maximum.

## Setup on the day

Times in Cotonou, for a presentation at 14:15.

| When | Check |
| --- | --- |
| 13:15 | Workstation on mains power. Notifications off, mail and chat closed. Face lit from the front. |
| 13:20 | Simulated device started with JDK 11 ([dev-setup-windows.md](dev-setup-windows.md), section 8) and checked with `python mock-sbi\scripts\check_device.py`. Demo started with `.\run_demo.ps1` ([../demo/README.md](../demo/README.md)) in a terminal where JDK 11 is selected, window maximised. |
| 13:30 | Scenes 3 and 5 played once. "Challenges per attempt" set to 1. |
| 13:45 | Final deck, full deck, clip and video opened. Screen sharing tested in Zoom with its video off: share the whole screen, so that the switches between deck, demo and clip show, then run scene 3 once while sharing, to check that the device gets the camera. |
| 14:05 | Zoom meeting joined with the video off, display name with the team name, microphone checked. |
| 14:15 | Presentation. |

After the presentation, stop the demo and close the terminal of the simulated device without saving its output, which holds the captured face records.

## Questions and answers

Short answers, each with its source in this repository.

| Question | Answer | Source |
| --- | --- | --- |
| Where does the passive check run? | In the face device, as the mentors asked on 23 October: the device scores each frame with two MiniFASNet models and sends the score with the frame (`X-Liveness-Score`). The client takes the median over a window and runs the challenges. | [decision-flow.md](decision-flow.md), section 4; [mock-sbi.md](mock-sbi.md) |
| What if a device sends no score? | The client computes the score itself, with the same models in the engine. The rest of the flow does not change. | [engine-api.md](engine-api.md); [PROGRESS.md](PROGRESS.md), decision of 23 October |
| Can the device score be forged? | It travels in the stream, which SBI does not sign. The client still checks the face, quality and head angle of each frame, and with the pass threshold at 1.00 no session passes without the challenges. The client itself scores the signed image returned by `RCAPTURE`. | [security-privacy.md](security-privacy.md), section 2 |
| Why challenges in every session? | At a pass threshold of 0.85, two glossy prints and one phone screen of the second subject scored like live faces. The pass threshold is 1.00 by default, so every session ends with the challenges, about ten seconds. A passive pass stays a setting, per workflow. | [security-privacy.md](security-privacy.md), section 3, item 4; [test-results.md](test-results.md), section 1.2.1 |
| Can a recorded video pass the challenges? | The challenges are drawn with `SecureRandom` at each attempt, so a video cannot anticipate them. The face is followed from frame to frame, and the signed captured image must match it. | [decision-flow.md](decision-flow.md), sections 3.4 and 6 |
| Deepfakes, virtual cameras, stream injection? | Outside presentation attack detection: SBI does not sign the stream, only the capture response, which the client still validates. Protection comes from certified L1 devices and the device trust chain. | [security-privacy.md](security-privacy.md), section 3, item 1 |
| Masks? | The cut-out paper mask worn by another person is a known limitation: 1 of 3 sequences passed the passive check, under backlight. 3D and silicone masks were not measured. For operators and supervisors, a deployment can chain a second sign-in method. | [security-privacy.md](security-privacy.md), section 3, items 2 and 6 |
| How reliable are the figures? | Two subjects, 67 sequences, three lighting conditions: preliminary, given with their sample size. No live sequence was refused. Next step: more subjects and thresholds confirmed on held-out subjects. | [test-results.md](test-results.md), section 1 |
| What if a resident cannot perform a challenge? | The challenge types are configurable per workflow, with 15 s each. After the last attempt, the operator continues under the MOSIP exception process with a supervisor, so the resident is never blocked. | [configuration.md](configuration.md); [decision-flow.md](decision-flow.md), section 7.1 |
| What happens to the images? | Everything runs on the workstation. The engine writes no image, and logs, audit events and the packet hold codes and results, never a score. The packet carries the decision and the challenges of each face. | [security-privacy.md](security-privacy.md), sections 1 and 2 |
| Does it work offline? | Yes, with the same rules: the models and the engine run locally. | [architecture.md](architecture.md) |
| Hardware needs? | About 56 ms per analysed frame on an Intel Core i7-8550U, CPU only, for a target of 100 ms. The four models weigh 10.4 MB. | [pitch-outline.md](pitch-outline.md), sources |
| How would MOSIP integrate it? | The engine is a separate Java 11 JAR with a documented API. The client changes are in the `feature/face-liveness` branch of our fork of release 1.2.0.2. The settings are 17 configuration keys synchronised from the server, and the client verifies the signature of the engine JAR at startup. | [engine-api.md](engine-api.md); [configuration.md](configuration.md) |
| Status on Collab? | The modified client signs in and synchronises, and the onboarding face passes liveness. Saving the onboarding fails in IDA (`RPR-RTS-002`, HTTP 500); MOSIP is reading its logs for the attempt of 31 October. | [workflows.md](workflows.md), section 2.4; [PROGRESS.md](PROGRESS.md) |
| ISO/IEC 30107? | The project follows the vocabulary and the reporting of ISO/IEC 30107-3, with error rates per attack type and their sample sizes. It is not a certified evaluation. | [iso-30107-alignment.md](iso-30107-alignment.md) |
| Android? | Covered by a design note: the engine core avoids desktop imaging classes, so the same code can move to the Android client. Not built. | [android.md](android.md) |
| Licences? | The code is under MPL-2.0. The models are under Apache-2.0 (MiniFASNet, MediaPipe Face Mesh) and MIT (YuNet). | [../THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) |
