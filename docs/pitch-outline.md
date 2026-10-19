# Pitch outline

Plan and content of the pitch deck of Best_Team for MOSIP Decode 2026, problem statement 04 (#37). The deck itself is built from this outline.

- Audience: the MOSIP Decode jury, who know MOSIP and the Registration Client but not our code.
- Length: 8 to 10 minutes, 14 slides, about 30 seconds per slide, with the live demo on slide 12 (2 to 3 minutes; the video of #36 is the fallback).
- Rules: every figure comes from a file of this repository or a comment of an issue, listed in "Sources" at the end, with its sample size. The measured results come from one subject and are preliminary. No face image appears on a slide.

## Slide 1: Face liveness and PAD for the MOSIP Registration Client

On the slide:

- Title: Face liveness and presentation attack detection for registration and biometric authentication
- MOSIP Decode 2026, problem statement 04
- Best_Team: Elisée Atonde (coordination and documentation), Temitayo Gbolahan (tests and evaluation), Tobi Obassandjo (engine and Registration Client integration), Silverio Mensah (simulated device)
- Code: `BestTeam-MosipDecode/face-liveness-pad` and the `feature/face-liveness` branch of the team's fork of `registration-client`, MPL-2.0

Speaker notes:

We are Best_Team, a team of four who worked on problem statement 04. We added a face liveness and presentation attack detection gate to the desktop Registration Client, release 1.2.0.2. It covers the three workflows of the problem statement: resident face capture, operator authentication and supervisor authentication. Everything we show is open source under MPL-2.0, in our repository and in our fork of the client.

## Slide 2: The problem

On the slide:

- Release 1.2.0.2 shows the device stream as a preview and requests the capture at the same moment
- Nothing checks that the face in front of the device belongs to a live person
- A printed photo, a phone screen or a replayed video can be enrolled, or used to sign in as an operator or a supervisor
- Registration centres may work offline: the check cannot depend on a server

Speaker notes:

In the current client, the face stream from the SBI device is only a preview, and the capture request leaves at the same time. No step looks at whether the face is live. This leaves room for presentation attacks at enrolment, which would put a wrong face in the identity record, and at operator or supervisor sign-in, which would let someone act under another person's account. The problem statement also asks for the same behaviour online and offline.

## Slide 3: Who it serves

On the slide:

- Residents: guided by plain instructions, no action needed in most cases
- Registration operators: a capture is accepted only for a live face; their own face sign-in is protected
- Supervisors: physically present when they approve or authenticate
- Countries and integrators running MOSIP: one engine, configured per workflow, any SBI-compliant L0/L1 device

Speaker notes:

The feature serves the people in a registration centre and the organisations that run MOSIP. The resident sees a short message and usually has nothing to do. Operators and supervisors get the same check on their own face authentication, so a printed photo of a supervisor is refused before the face is even matched. For an integrator, the check works with any device that speaks SBI, and every setting comes from the existing client configuration.

## Slide 4: Our solution in one view

On the slide:

- Passive check first: two MiniFASNet models score the frames of the stream, no action from the user
- Ambiguous score: an active check starts on its own, with facial actions drawn at random (blink, head turn left or right; smile available by configuration)
- Capture or authentication only after `PASSED`, then the image returned by `RCAPTURE` is checked again
- Retry policy, then the MOSIP exception process for a resident, another sign-in method for an operator or supervisor
- Runs inside the client, offline included

Speaker notes:

The decision follows the hybrid approach of the problem statement. The passive check scores the stream frames and decides alone when the score is clearly high or clearly low. In between, the client asks for actions drawn with `SecureRandom`, so a recorded video cannot anticipate them. The capture request leaves only after the session has passed, and the signed image that comes back must match the face that was followed. After the last attempt, a resident goes to the existing MOSIP exception process with a supervisor, while an operator or a supervisor has to use another sign-in method.

## Slide 5: Architecture

On the slide:

- Image: `docs/diagrams/components.svg`
- Device, real or simulated, over SBI (`STREAM`, `RCAPTURE`) through the existing SBI providers of the client
- `liveness-engine`: a separate Java 11 module with its own API, no dependency on the client or JavaFX
- Client side: frame source adapter, liveness service, configuration provider, status pane, audit listener

Speaker notes:

The device side is unchanged: the client keeps its SBI providers and their signature validation, which keeps the solution independent of the device vendor. A small change in the stream reader hands each frame to our adapter, which feeds the engine with a monotonic clock. The engine is its own Maven module and JAR, with a public API documented in `docs/engine-api.md`, so it can be reused outside the desktop client. On the client side, new classes read the configuration, run one session per scan, show the status pane and write the audit events on a thread of their own.

## Slide 6: Passive to active decision flow

On the slide:

- Image: `docs/diagrams/session-states.svg`
- Score of each frame aggregated by the median over the window; pass threshold 0.85, attack threshold 0.30 (provisional defaults)
- Quality checks before scoring: one face, size, head angle, lighting, sharpness
- During the active check the passive score is still computed and the face is followed from frame to frame
- Every failure after the first analysed frame counts as an attempt, device errors included

Speaker notes:

A session goes from the passive check to the active check only when the median score falls between the two thresholds. Frames that cannot be used, for example with two faces or a turned head, get a guidance message instead of a score. While the user performs an action, the engine keeps scoring the frames and checks that the face is the same one, so a photo slipped in after a live start is caught. A device error also counts as an attempt, so unplugging the camera does not reset the counter. The thresholds are the defaults of our decision flow and stay provisional until more subjects are measured.

## Slide 7: Three workflows in the Registration Client

On the slide:

- Image: `docs/diagrams/sequence-resident.svg` (and `sequence-authentication.svg` as a second image if space allows)
- Resident face capture: `RCAPTURE` only after `PASSED`; after the last attempt, capture under exception, audited (`REG-EVT-112`), marked in the packet, supervisor required at submission
- Operator sign-in and packet authentication: liveness before the face is matched; failed liveness attempts count as failed logins
- Supervisor authentication and end of day approval: same check, supervisor role required, face not offered when liveness is disabled for the supervisor
- 17 configuration keys, any of them can be set per workflow

Speaker notes:

The three workflows of the problem statement are wired in our fork of release 1.2.0.2. For the resident, the existing capture request now waits for the liveness result. If the resident fails every attempt, the operator can continue under the exception process: the capture is audited, marked in the packet, and a different user with the supervisor role must authenticate at submission. Operator and supervisor authentication have no exception path: the face is refused and another sign-in method is offered. All of it is driven by 17 configuration keys, which can differ per workflow.

## Slide 8: What the user sees

On the slide:

- Images: three renders of the status pane, for example `docs/ui/pane/en/p04-challenge-blink.png`, `docs/ui/pane/fr/p05-challenge-turn.png`, `docs/ui/pane/ar/p07-failed-retry.png`
- Large instruction band for the resident, progress, "Challenge 1 of 2", liveness status in words as well as colours
- 32 messages and 14 labels in English, French and Arabic, right to left in Arabic
- A detected attack shows the same generic message as an engine error
- Scores appear only in the diagnostic mode, off by default

Speaker notes:

The status pane is the component we added to the capture and authentication screens. These images are rendered from that real component with the message files of the client, in 18 states and three languages, without any camera preview, so no face is shown. The instruction is large because a resident sits further from the screen than the operator. When an attack is detected, the screen gives no hint about what was detected: the reason goes only to the audit trail. Text always accompanies colour, which helps colour-blind users and low-quality screens.

## Slide 9: Security and privacy

On the slide:

- All processing on the workstation: no frame, landmark or score leaves the machine
- No image written by the engine; logs and audit events carry codes, durations and the model set version only
- Challenges drawn with `SecureRandom`; the signature validation of SBI responses is untouched
- Capture response accepted only with exactly one face record, which must pass the capture check
- Four models checked against their SHA-256 in `models.json` before loading; the engine JAR signature is verified at client startup

Speaker notes:

We treated the liveness gate as a security control. Nothing biometric is stored beyond what the client already stores, and no score appears in a log unless the diagnostic mode is turned on. The engine refuses to start if any model file differs from its recorded checksum, and the client verifies the signature of the engine JAR. A capture response with several face records, or with an image that does not match the face followed on the stream, is refused. The limits of this model, such as stream injection before the client, are written in `docs/security-privacy.md`.

## Slide 10: Technology stack

On the slide:

- Java 11 engine, ONNX Runtime 1.19.2 on CPU, plain Java image processing
- Models: MiniFASNetV2 and MiniFASNetV1SE (Apache-2.0), YuNet face detector (MIT), MediaPipe Face Mesh V2 landmarks (Apache-2.0), 10.4 MB in total
- Registration Client 1.2.0.2 (Java 11, JavaFX, Spring), SBI 0.9.5 and 1.0
- `mock-sbi`: fork of MockMDS 1.2.0.2 with `static`, `webcam` and `replay` modes
- Python reference pipeline for ONNX export, parity tests and evaluation; PlantUML diagrams

Speaker notes:

We kept the stack of the client: Java 11, so the engine loads in the JVM of release 1.2.0.2. All four models run with one runtime, ONNX Runtime, after export from PyTorch and TFLite, with parity checked against the reference implementation in Python. The core of the engine avoids desktop imaging classes, so the same code can move to the Android client, which our design note `docs/android.md` discusses. The simulated device lets anyone run the SBI protocol end to end without hardware, including replayed attacks and device failures.

## Slide 11: Measured results (one subject, preliminary)

On the slide:

| Measure | Result |
| --- | --- |
| Live face, passive check | 6 of 6 sequences pass, no challenge needed (BPCER 0 %) |
| Prints, phone and laptop screens, replayed video | 15 of 15 sequences detected as attacks (APCER 0 %), in 0.8 to 1.5 s |
| Paper mask worn by another person | 1 of 3 accepted (backlight), 2 of 3 without a decision |
| Analysis time | about 56 ms per frame, under the 100 ms target (Intel Core i7-8550U, CPU only) |
| Simulated device scenarios | 24 of 24 stream runs pass, SBI 0.9.5 and 1.0 |

- Footnote: one subject, three lighting conditions, 36 sequences; no held-out subject
- Engine tests: 186; liveness tests of the client services: 89

Speaker notes:

These figures come from one team member recorded under normal light, low light and backlight, so they are preliminary and we present them with their sample size. Every live sequence passed on the passive check alone, and every print, screen and replayed video was rejected in about one second. The weak point is the cut-out paper mask worn by another person, which passed once under backlight. On a laptop processor without a graphics card, the analysis takes about 56 ms per frame. The full tables are in `docs/test-results.md` and on issues #7 and #29.

## Slide 12: Live demo

On the slide:

- Demo application: the real engine, the simulated device in webcam mode over SBI, the real status pane of the client
- Scene 1, live face: the passive check passes
- Scene 2, ambiguous case: an active challenge is drawn and verified
- Scene 3, printed photo then phone screen: rejected with the generic message
- Scene 4: operator and supervisor workflows, attempts exhausted, offline run, configuration

Speaker notes:

The demo runs the same engine and the same status pane as our fork, fed by the simulated device over SBI, as the Registration Client would feed them. We first show a live face accepted on the passive check, then raise the pass threshold to force the ambiguous case and the active challenge. Next, a printed photo and a phone screen are rejected with the generic message. We end with the operator and supervisor workflows, the end of the attempts, a run with the network turned off and the configuration keys. If a MOSIP environment is available on the day, we also show the modified Registration Client; otherwise the demo application stands for it, with the same components.

## Slide 13: Limits and next steps

On the slide:

- Paper mask worn by another person: known limitation; next step, a dedicated check of mask edges and paper texture
- One subject so far: record more subjects, then confirm the thresholds on a held-out subject
- Blink challenge recognised in 1 of 3 recorded sequences: check the timing of the recordings, then calibrate
- No on-screen check yet on a MOSIP environment; Android covered by a design note only
- Stream injection before the client depends on device integrity (L1 devices), outside PAD

Speaker notes:

We list our limits because the jury will meet them anyway. The cut-out paper mask is the main one: real eyes and a real mouth show through the holes, so the passive classifier scores it like a live face. Our results rest on one subject, so the next step is more subjects and a held-out evaluation before any threshold is called final. The blink challenge needs work on its calibration. Finally, the client changes have not yet been checked on screen against a MOSIP server, and the Android client is covered by a design note only.

## Slide 14: Impact

On the slide:

- A liveness gate in the three workflows of the problem statement, without new hardware
- Works offline with the same rules as online
- Vendor-neutral through SBI; settings through the existing client configuration
- Reusable engine module with a documented API, simulated device and test tools for other teams
- Open source under MPL-2.0, the licence of MOSIP

Speaker notes:

Our work gives MOSIP deployments a liveness and PAD gate that runs on the registration workstation, with the devices they already use. Because the check is local, it protects offline centres as well as connected ones. The engine, the simulated device and the evaluation tools are separate pieces that other teams and vendors can reuse and test against. Thank you; we are happy to take your questions.

## Sources

| Figure or fact | Source |
| --- | --- |
| Behaviour of release 1.2.0.2 (preview and capture at the same moment) | `docs/architecture.md`, section 1 |
| Thresholds 0.85 and 0.30, kept provisional | `docs/decision-flow.md`, section 4; decision of 19 October 2026 on #7 |
| Default challenges BLINK, TURN_LEFT, TURN_RIGHT; SMILE by configuration | `docs/configuration.md`; `docs/decision-flow.md`, section 5.1 |
| 17 configuration keys with per-workflow overrides | `docs/configuration.md`; `docs/architecture.md`, section 2.1 |
| Exception process, `REG-EVT-112`, supervisor rules | `docs/decision-flow.md`, section 7.1; `docs/security-privacy.md`, section 2.2 |
| 32 messages and 14 labels per language | `messages_*.properties` and `labels_*.properties` of the fork, counted on 19 October 2026 (32 after registration-client#14) |
| 18 pane states in three languages | `docs/ui/README.md`, "Status pane of the client"; `docs/ui/pane/` |
| Model licences and sizes, 10.4 MB in total | `docs/architecture.md`, section 9; `THIRD_PARTY_NOTICES.md`; `docs/PROGRESS.md` (issue n° 4) |
| ONNX Runtime 1.19.2 | `docs/architecture.md`, section 9 |
| 186 engine tests | `README.md`, status table |
| 89 liveness tests of the client services | registration-client#14 description, run of 19 October 2026 |
| 6 of 6 live sequences pass (BPCER 0 %), attacks 0 % accepted, paper mask 1 of 3 | PrinceSpecial's comment on #7 (19 October 2026), preliminary, one subject |
| 15 of 15 attacks in 0.8 to 1.5 s, about 56 ms per frame, machine | Comment of Magloire04 on #29 (19 October 2026) |
| Challenges: turn right 3 of 3, turn left 2 of 3, blink 1 of 3 | Comment on #29 |
| 24 of 24 device stream runs, SBI 0.9.5 and 1.0 | Comment on #29 |
| Paper mask as a known limitation | `docs/security-privacy.md`, section 3, item 6 |
| One subject, 36 sequences, three lighting conditions | `docs/iso-30107-alignment.md`, sections 4.1 and 5; PrinceSpecial's comment on #7 |
| Team and roles | `docs/PROGRESS.md`, "Équipe" |
