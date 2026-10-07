# Security and privacy

Status on 7 October 2026: design, first version. This document lists what the liveness feature protects, the attacks it addresses, the controls in place or planned, the limits that remain, and how personal data is handled. It relies on [decision-flow.md](decision-flow.md) and [workflows.md](workflows.md).

## 1. What is protected

| Asset | Why it matters |
| --- | --- |
| The face stored in a resident's registration | A face that does not belong to a live person present at the centre would create a false identity record |
| Operator and supervisor face authentication | A photo of an operator must not open a session or approve packets |
| The liveness decision and the attempt counter | Lowering the threshold or resetting the counter would weaken every control below |
| The models and the liveness configuration | A replaced model or threshold changes every decision |
| Biometric data handled during the check | Frames and landmarks are personal data of the resident or of the staff |

## 2. Attacks and controls

### 2.1 Presentation attacks

| Attack | Controls | Where |
| --- | --- | --- |
| Printed photo, matte or glossy | Passive classifier (two MiniFASNet models), aggregated over several frames | Decision flow, sections 3.6 and 4 |
| Photo or video shown on a phone, tablet or laptop screen | Same classifier. Active challenges when the score is ambiguous | Sections 4 and 5 |
| Replayed video in which the person blinks, smiles or turns | Challenges drawn with `SecureRandom` at each attempt, action required after the instruction and before the deadline, turn in the wrong direction rejected | Sections 5.1 to 5.3 |
| Cut-out paper mask | Classifier, plus challenges that need a moving mouth or eyelids | Sections 4 and 5 |
| Photo used during a challenge, after a live face passed the start | Passive score still computed during the challenges, face continuity checked from frame to frame | Section 5.4 |
| A second person or a swapped face during the session | Only one face accepted. A second face or a jump of the face box ends the attempt | Sections 3.3 and 3.4 |

### 2.2 Attacks on the capture and on the client

| Attack | Controls | Where |
| --- | --- | --- |
| Liveness passed on the stream, another image returned by the capture | The image returned by `RCAPTURE` is checked: one face, quality, score outside the attack zone, consistent position and size with the face followed on the stream | Decision flow, section 6 |
| Forged image returned by a fake device | The client already validates the signature of `RCAPTURE` responses against the device trust domain (`MosipDeviceSpecificationHelper.validateJWTResponse`). This validation stays untouched in production | Existing client |
| Many attempts until one passes by chance | Maximum number of attempts per capture or authentication. Every failure after the first analysed frame counts, device errors included, so unplugging the device does not reset the counter | Section 7 |
| Operator using the resident exception process to bypass liveness | A supervisor, another user, must authenticate at submission, and the supervisor's own face goes through liveness. The exception is recorded in the audit trail and marked in the packet for review on the server side | Section 7.1 |
| Learning from the screen how close a photo came to passing | A detected attack shows the same generic message as an engine error. No score, measure or model data on screen unless the diagnostic mode is enabled | Section 8 |
| Liveness bypassed for authentication after failed attempts | No exception path for authentication: the face is refused | Section 7.1 |

### 2.3 Integrity of models and configuration

- **Models.** Each model file has its SHA-256 in `models.json`. The engine will check them at startup and refuse to start on a mismatch (#31). The model set version is written in every audit event.
- **Client files.** At startup, the client verifies the signature of its JAR files (`ClientIntegrityValidator`), but only for files named `registration-client*` and `registration-services*`, and not when `environment=LOCAL`. A separate `liveness-engine` JAR would not be covered. Either the check is extended to it, or the engine is packaged inside one of the two verified JARs. To decide with the integration (#26).
- **Configuration.** Liveness settings come from the server configuration, synchronised like the other client settings and read through `ApplicationContext`. When a key is missing, the engine applies its packaged defaults, never a weaker value. Changing the local database of a client is outside the scope of this feature.
- **Randomness.** Challenges are drawn with `java.security.SecureRandom`. A fixed seed is only possible in tests, through injection.

## 3. Limits

These attacks are not, or only partly, covered. They are stated in the submission.

1. **Injection attacks.** The video stream is not signed by the SBI specification: only the capture response is. A forged stream injected between the camera and the client (virtual camera, modified device service) bypasses presentation attack detection by nature. The capture check ties the decision to a signed image, but a compromised device service could sign forged images. Protection comes from certified L1 devices and from the device trust chain, not from the liveness engine.
2. **Unknown training data.** The training data of the pretrained classifier is not published. Its documentation lists printed photos, screens, silicone masks and 3D figures as fake faces, but says nothing of their share. The effect on each attack type is measured on the team captures (#7). High-quality 3D and silicone masks cannot be measured, since the team has none: challenges make them harder to use, not impossible.
3. **RGB cameras only.** L0 and L1 face devices provide colour images. No depth or infrared signal is available to the engine.
4. **Small calibration set.** Thresholds are set on the captures of four team members. The error rates published with the submission come with their sample size and should not be read as a certification result.
5. **Head roll.** The landmark region is not rotated, so a strongly tilted head lowers the accuracy of the challenges (decision flow, section 3.5).

## 4. Personal data

### 4.1 During the check

- All processing happens on the registration workstation. No frame, landmark or score leaves the machine for liveness.
- Frames are decoded in memory, analysed and dropped. The engine keeps only numbers between frames: scores of the current window, measures of the current challenge, the box of the tracked face.
- The engine writes no image to disk. The only face image kept is the one the client already stores in the registration or uses for matching, as it does today.
- Logs, audit events and error messages never contain an image, a template, a landmark or a score. Audit events contain the workflow, the attempt number, the reason code, the duration and the model set version. Scores go only to the diagnostic log, when `diagnostic.enabled` is true, which is not the default.
- The packet receives no additional biometric data. The only addition is the liveness exception mark, when the exception process is used.

### 4.2 Data used to build and test the feature

- No face image, video or public dataset is stored in the repository. `datasets/` and `captures/` are ignored by Git, and images under any `results/` folder are ignored too. Golden test vectors are synthetic.
- Captures of team members are made with written consent, under pseudonyms (`S01`, `S02`...), kept outside the repository, deleted on request, and never published (#6). Only aggregated metrics are published.
- Public datasets with research-only licences are not redistributed. The current plan uses none of them.

### 4.3 Fairness

A liveness check that fails more often for some people would push them more often towards the exception process. Mitigations:

- the landmark model was chosen partly because its model card reports similar accuracy across regions and skin tones;
- the team captures cover several skin tones and lighting conditions (normal, low light, backlight);
- the bona fide rejection rate is reported per capture condition, with the sample size, so that a gap can be seen even on a small set.

## 5. Recommended production settings

| Key | Value | Reason |
| --- | --- | --- |
| `enabled` | `true` | |
| `active.enabled` | `true` | Ambiguous cases go to challenges instead of failing |
| `diagnostic.enabled` | `false` | No score on screen |
| `max_attempts` | `3` | |
| `on_max_attempts` | `BLOCK`, and `EXCEPTION` for residents | Section 7.1 of the decision flow |
| `supervisor.active.min_challenges` | `3` | Supervisors approve packets and exceptions: a stricter policy |

Devices: certified L1 devices where available. The simulated device of this project is for development and tests only, and its test certificates must never be trusted in production.
