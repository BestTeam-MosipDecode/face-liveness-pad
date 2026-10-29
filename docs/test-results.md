# Test results

Status on 25 October 2026: section 1 holds the measures made on the captures of two team subjects (S10 on 15 October, S30 on 20 October), sections 3 to 5 those of S10, with the scores and replays run on the machines that recorded them; only aggregated figures leave those machines. Section 2.1 records the cases run on screen in the demo application and on the MOSIP Collab environment. The full matrix of section 2 needs resident registrations in the Registration Client signed in to Collab, which wait for the onboarding to be saved: the server refuses it (`RPR-RCT-001`), an issue of the Collab environment under review by MOSIP (section 2). Values marked "not run" are filled in when the test is done, following [test-plan.md](test-plan.md). No face image appears in this document.

**Section 1 covers two subjects (S10 on 15 October, S30 on 20 October), the other sections one (S10).** They show that the chain works and where it fails; they are not a statistically meaningful error rate. ISO/IEC 30107-3 asks for many subjects and presentation attack instruments before an APCER or a BPCER can be compared with other systems.

## 1. Passive model on the team dataset

PAD subsystem evaluation (ISO/IEC 30107-3, clause 7.3), computed by `training/scripts/evaluate.py` (#7) on the test split of [test-plan.md](test-plan.md), section 6.

### 1.1 Context

| Item | Value |
| --- | --- |
| Date | Captures 15 October 2026, scores computed the same day, evaluation 19 October |
| Model set version | 1.1.0 (`liveness-engine/src/main/resources/models/models.json`) |
| Thresholds: pass, attack | Measured at 0.85 and 0.30, the defaults at the time of the captures. After the second subject, the default pass threshold became **1.00** for the three workflows, the attack threshold staying 0.30 (decision of the coordinator on #7, 21 October; see the end of section 1.2.1) |
| Subjects: calibration split, test split | 1 subject, S10. No held-out subject: `evaluate.py --scores ... --no-split`, figures flagged as preliminary |
| Skin tone bands (MST 1-3, 4-7, 8-10), subjects per band | Not published: with one subject, the band would describe that person |
| Cameras and resolutions | Integrated laptop camera, 1280 x 720 requested, 10 frames saved per second |
| Machine for the timings | Intel Core i7-8550U, 16 GB, Windows 11 Pro, Python 3.10.8 (section 4) |
| Sequences | 36 of 10 s, about 100 frames each, under the three lighting conditions: 6 `bona_fide`, 3 per attack species, 3 per action. The action sequences are left out of the passive evaluation (a turned head scores low on purpose) and reported in section 1.4 |

### 1.2 Error rates on the test split

Each sequence is replayed through the passive window of section 4 of the decision flow with the thresholds above; the first decision reached is counted. APCER counts the attacks that end in the pass zone, BPCER the bona fide presentations that end in the attack zone.

| Type | Sequences | APCER or BPCER | Pass zone | Challenge zone | Attack zone | No decision |
| --- | --- | --- | --- | --- | --- | --- |
| `print_matte` | 3 | 0 % | 0 | 0 | 3 | 0 |
| `print_glossy` | 3 | 0 % | 0 | 0 | 3 | 0 |
| `screen_phone` | 3 | 0 % | 0 | 0 | 3 | 0 |
| `screen_laptop` | 3 | 0 % | 0 | 0 | 3 | 0 |
| `replay_video` | 3 | 0 % | 0 | 0 | 3 | 0 |
| `paper_mask` | 3 | 33 % (1 of 3) | 1 | 0 | 0 | 2 |
| Highest APCER | | 33 % (`paper_mask`); 0 % for the five other species | | | | |
| `bona_fide` (BPCER) | 6 | 0 % | 6 | 0 | 0 | 0 |
| ACER, benchmark figure | | 17 % with the paper mask, 0 % without | | | | |

Frame scores: median 0.996 for the bona fide frames (0.976 for the lowest sequence), 0.000 for every scored frame of the prints, screens and replayed video, 0.998 for the scored frames of the paper mask. Share of frames that passed the quality checks and received a score: 99 % for the bona fide frames and the prints, 100 % for the phone screen, 54 % for the laptop screen and 37 % for the replayed video (the others rejected for lighting or because no face was found), 15 % for the paper mask (74 % rejected by the head angle check, 6 % with no face found).

**Paper mask: known limitation.** The passive classifier scores a cut-out paper mask worn by another person as a live face. The mask was accepted once (backlight) and stayed without a decision in the two other sequences, where the head angle check rejected most frames. The active check does not stop it either: the wearer blinks and turns the head through the cut-outs. The decision of the coordinator (#7, 19 October) is to report it with its rate, keep the code and thresholds unchanged before the submission, and describe the controls in place and a dedicated mask check as future work in [security-privacy.md](security-privacy.md).

DET curve of the sequence medians, single threshold: [../training/results/chart_det.png](../training/results/chart_det.png). Equal error rate 3 % with the paper mask, 0 % without it (the lowest bona fide median, 0.976, is far above the highest print, screen or video median, 0.000).

### 1.2.1 Second subject, S30

Score file received on 21 October from the machine that recorded S30 (20 October, 31 sequences: 6 `bona_fide`, 3 per action, 3 `print_glossy`, 3 `screen_phone`, 3 `screen_laptop`, 3 `replay_video`, 1 `print_matte`, no `paper_mask`). Defaults 0.85 and 0.30, same replay of the passive window:

| Type | Sequences | Pass zone | Challenge zone | Attack zone | No decision | Sequence medians |
| --- | --- | --- | --- | --- | --- | --- |
| `bona_fide` | 6 | 3 | 0 | 0 | 3 (low light: no face found in most frames) | 0.996 to 0.999 |
| `print_glossy` | 3 | **2** | 0 | 0 | 1 | **0.998 and 0.999** |
| `screen_phone` | 3 | **1** | 1 | 0 | 1 | 0.829 and 0.403 |
| `screen_laptop` | 3 | 0 | 0 | 3 | 0 | 0.10 to 0.18 |
| `replay_video` | 3 | 0 | 0 | 0 | 3 (4 frames scored in all) | |
| `print_matte` | 1 | 0 | 0 | 0 | 1 (no face found in any frame) | |

No face was found in 37 % of the S30 frames, against 6 % for S10, and the analysis took 29 ms per frame (another machine). The capture conditions of S30 (distance, resolution, artefacts) are being checked with the person who recorded them (#7).

Both subjects together, at the defaults:

| Type | Sequences | APCER |
| --- | --- | --- |
| `print_glossy` | 6 | **33 %** (2 of 6, both S30) |
| `paper_mask` | 3 | **33 %** (1 of 3, S10) |
| `screen_phone` | 6 | 17 % (1 of 6, S30) |
| `print_matte`, `screen_laptop`, `replay_video` | 4, 6, 6 | 0 % |
| `bona_fide` (BPCER) | 12 | 0 %; 9 passed, 3 without a decision |

Calibrated on one subject and tested on the other (`evaluate.py --test-subjects`), the threshold rule finds no pass threshold that keeps every calibration attack out of the pass zone (a glossy print of S30 scores 0.999, above part of the live faces): it proposes a pass threshold of 1.00, which sends every session to the active check. With that setting, no attack sequence of either subject ends in the pass zone (APCER 0 % for the passive check), 9 of the 12 bona fide sequences go to a challenge and 3 stay without a decision. A print or a still screen then has to perform a randomly drawn blink or head turn, which it cannot; that last step is the design of the active check and is not measured on these recordings.

**Decision (#7, 21 October): pass threshold 1.00 by default for the resident, the operator and the supervisor, attack threshold 0.30.** The reason is the APCER measured at 0.85 above: 33 % for glossy prints and 17 % for phone screens, with no pass threshold that separates them from the live faces. At 1.00 the passive check still rejects the clear attacks at once (every print, screen and video of S10, the laptop screens of S30), and every other session ends with the random challenges. Cost: every live person performs the challenges, 2 by default, about 5 s each on the S10 replay (section 4). The paper mask stays a known limitation: its wearer can perform the challenges. The engine applies 1.00 from `LivenessPolicy.defaults()`, and a score of exactly 1.0 never passes on the passive check (#105). The figures of sections 1 and 3 were measured at 0.85 and are kept as the reason for the choice.

### 1.3 BPCER by condition

| Group | Sequences | BPCER |
| --- | --- | --- |
| Light `normal` | 2 | 0 % (median frame score 0.998) |
| Light `low` | 2 | 0 % (0.997) |
| Light `backlight` | 2 | 0 % (0.993) |
| Skin tone MST 1-3 | not run | no subject in this band yet |
| Skin tone MST 4-7 | not run | not published: with at most one subject per band, the band would describe that person |
| Skin tone MST 8-10 | not run | not published, as above |

### 1.4 Actions

Measured by the coordinator with the Python detectors of `training/liveness/actions.py` on the S10 action sequences (#5, 15 October), and with the engine in section 3.3:

| Action | Sequences | Detected | Other action detected |
| --- | --- | --- | --- |
| `action_blink` | 3 | 3 | none |
| `action_smile` | 3 | 1, with the factor 1.06 on 2 consecutive frames | none |
| `action_turn_left` | 3 | 3, once performed towards the subject's left | a smile, under 2 conditions of 3 |
| `action_turn_right` | 3 | 3 | none |

Consequence: `SMILE` was left out of the default challenge types on 16 October (#82); it stays available through `active.challenge_types`. The other factors are those of section 5.3 of the decision flow.

### 1.5 Time per frame

| Step | Mean time (ms) |
| --- | --- |
| Face detection (YuNet) | 22 to 40, reference figure of `training/README.md` on the same processor |
| Landmarks (Face Mesh V2) | 14 to 15, same reference |
| Classifiers (two MiniFASNet) | 11 to 15, same reference |
| Total | 55.2 over the 3 580 frames of S10: detection, quality checks and the two classifiers, Python, Intel Core i7-8550U. The Java engine: section 4 |

## 2. Functional test matrix

Cases of [test-plan.md](test-plan.md), section 9 (#27). Workflows: R resident capture, O operator authentication, S supervisor authentication. Online and offline as defined in the plan.

The matrix needs resident registrations in the Registration Client signed in to a MOSIP environment. On Collab, sign-in and synchronisation work and the operator onboarding was checked on screen (section 2.1); a resident registration needs the onboarding saved first, which the server refuses (`RPR-RCT-001`), an issue of the Collab environment under review by MOSIP ([workflows.md](workflows.md), section 2.4). The engine behaviour behind F04 to F06 and F09 to F11 is measured on replayed sequences in sections 3 and 5.

| ID | Case | Expected | R online | R offline | O online | O offline | S online | S offline |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F01 | Live face, good light | `PASSED` after the challenges (pass threshold 1.00), capture accepted, `LIVENESS_SUCCESS` | not run | not run | not run | not run | not run | not run |
| F02 | Ambiguous score leads to a challenge | `ACTIVE_CHALLENGE`, challenges detected, then `PASSED` | not run | not run | not run | not run | not run | not run |
| F03 | Action not performed | `FAILED` with `CHALLENGE_TIMEOUT` or `CHALLENGE_FAILED`, retry offered, attempt counted | not run | not run | not run | not run | not run | not run |
| F04 | Printed photo | Never `PASSED`. `PAD_ATTACK_DETECTED` with the generic message, or failed challenge | not run | not run | not run | not run | not run | not run |
| F05 | Photo on a screen | As F04 | not run | not run | not run | not run | not run | not run |
| F06 | Replayed video | Never `PASSED`. Attack detected or challenge failed | not run | not run | not run | not run | not run | not run |
| F07 | No face | Guidance `NO_FACE`, then `FAILED` with `NO_FACE` after 20 s | not run | not run | not run | not run | not run | not run |
| F08 | Two faces | Guidance `MULTIPLE_FACES`. During a challenge, `FACE_CONTINUITY_LOST` | not run | not run | not run | not run | not run | not run |
| F09 | Face changed during a challenge | `FAILED` with `FACE_CONTINUITY_LOST` | not run | not run | not run | not run | not run | not run |
| F10 | Device unplugged | `DEVICE_ERROR`, `LIVENESS_DEVICE_UNAVAILABLE`. Attempt counted only after the first analysed frame | not run | not run | not run | not run | not run | not run |
| F11 | Corrupted frame | Frame ignored, `INVALID_FRAME` in the diagnostics, session continues | not run | not run | not run | not run | not run | not run |
| F12 | Attempts exhausted | `MAX_ATTEMPTS_EXCEEDED`. R: exception process offered. O, S: authentication refused | not run | not run | not run | not run | not run | not run |
| F13 | Liveness disabled | No session, original client behaviour | not run | not run | not run | not run | not run | not run |
| F14 | Supervisor override | R: capture under exception, supervisor authentication with liveness at submission. O, S: no override offered | not run | not run | not run | not run | not run | not run |

### 2.1 Cases run on screen

Run on 25 October 2026 for the video of the submission, with the default settings unless stated: the demo application ([../demo/README.md](../demo/README.md)) runs the engine, the status pane and the messages of the modified client, fed over SBI by the simulated device in webcam mode, which scores each frame. It does not run the screens of the Registration Client, so these rows do not fill the matrix above. The last row is the Registration Client itself on Collab.

| Case | Workflow, settings | Where | Result |
| --- | --- | --- | --- |
| F01, live face | Resident | Demo application | Device score, then 2 challenges (turn right, blink), `PASSED`, captured image accepted |
| F01, live face | Operator, French | Demo application | `PASSED` after 2 challenges (blink, turn left) |
| F01, live face | Supervisor, diagnostic mode | Demo application, Wi-Fi turned off | `PASSED` after 2 challenges (blink, turn right), device score shown on the diagnostic line |
| F01, live face, language changed during the session | Supervisor, English then Arabic | Demo application | Instructions switched to Arabic, right to left, then `PASSED` |
| Passive pass allowed at 0.85 (setting) | Resident | Demo application | `PASSED` in 2.5 s without a challenge |
| F04, printed photo | Resident | Demo application | Refused with the generic message, attempt 1 of 3, retry offered |
| F05, photo on a phone screen | Resident | Demo application | Refused the same way, attempt 2 of 3 |
| F12 and F14, attempts exhausted | Resident, 1 attempt, printed photo | Demo application | Exception process offered; face captured under exception, a supervisor required before submission |
| F05 and F12, phone screen | Supervisor, 1 attempt, French | Demo application | Refused, the user is sent to another sign-in method |
| Onboarding, live face | Operator | Registration Client on MOSIP Collab, 23 and 25 October | Device score, 2 challenges (blink, turn right), liveness verified, face captured. Saving the onboarding refused by the server (`RPR-RCT-001`), under review by MOSIP |

## 3. System PAD campaign

Full system evaluation (ISO/IEC 30107-3, clause 7.5), on the complete Registration Client with passive and active checks (#28).

Run on 19 October 2026 at the level of the engine public API: each recorded sequence goes through `LivenessEngine` and a `LivenessSession`, the code the client calls, in real time (frames submitted at their recorded timestamps, so frames are skipped as on a device stream), with `ReplayBenchmarkTest` of `liveness-engine`. The same run through the Registration Client screens waits for resident registrations on Collab (section 2).

### 3.1 Context

| Item | Value |
| --- | --- |
| Date | 19 October 2026 |
| Client version, engine version, model set version | Engine of `main` at 1d95604, model set 1.1.0; client not involved (see above) |
| Settings different from the defaults | One attempt per sequence. Bona fide and attack sequences: active check disabled, so an ambiguous score would fail rather than go to a challenge. Action sequences: pass threshold 1.0, so that every session goes to the active check, with one challenge of the recorded action |
| Subjects, attack presenters | 1 subject (S10). Prints, screens and video presented by the subject; paper mask worn by a volunteer who signed the consent form, as no other team member was available (deviation from section 3.2 of the test plan, noted in the local register) |
| Lighting conditions | `normal`, `low`, `backlight`, one sequence per type and condition |
| Cameras, machine | Integrated laptop camera; Intel Core i7-8550U, 16 GB, Windows 11 Pro (section 4) |

### 3.2 Artefact register

| Artefact | Species | Subject | Made with |
| --- | --- | --- | --- |
| A01 | Matte print | S10 | Frontal photo of the subject printed on matte paper |
| A02 | Glossy print | S10 | Same photo printed on glossy paper |
| A03 | Photo on a phone screen | S10 | Same photo shown full screen on a phone |
| A04 | Photo on a laptop screen | S10 | Same photo shown full screen on a laptop |
| A05 | Replayed video | S10 | Short video of the subject played on a phone |
| A06 | Paper mask | S10 | Matte print with the eyes and mouth cut out, worn by a volunteer |

### 3.3 Error rates

| Type | Attempts | APCER or BPCER | Challenge rate |
| --- | --- | --- | --- |
| `print_matte` | 3 | 0 %, `PAD_ATTACK_DETECTED` in 0.76 to 0.92 s | not applicable, active check disabled for this run |
| `print_glossy` | 3 | 0 %, detected in 0.83 to 0.92 s | as above |
| `screen_phone` | 3 | 0 %, detected in 0.77 to 0.92 s | as above |
| `screen_laptop` | 3 | 0 %, detected in 0.92 to 1.54 s | as above |
| `replay_video` | 3 | 0 %, detected in 0.86 to 0.93 s | as above |
| `paper_mask` | 3 | 33 %: passed in 0.91 s under backlight; no decision within the 10 s sequence under normal and low light (still in the passive check) | as above |
| Highest APCER | | 33 % (`paper_mask`); 0 % for the five other species, 15 of 15 attacks detected | |
| Bona fide (BPCER) | 3 | 0 %, passed without a challenge in 1.19 to 1.47 s | 0 % |
| ACER, benchmark figure | | 17 % with the paper mask, 0 % without | |

Challenges on the bona fide action sequences (one challenge of the recorded action, see the settings):

| Challenge | Attempts | Passed | Failures |
| --- | --- | --- | --- |
| `TURN_RIGHT` | 3 | 3, in 4.9 to 6.6 s from the start of the challenge | none |
| `TURN_LEFT` | 3 | 2, in 4.6 and 7.1 s | backlight: `FACE_CONTINUITY_LOST` after 4.0 s |
| `BLINK` | 3 | 1, in 3.3 s (backlight) | normal and low light: `CHALLENGE_TIMEOUT` after 8 s |

The two blink timeouts are most likely an effect of the replay rather than of the detector: the subject blinked about 2 s after the start of the recording, as the capture script asks, while the session reaches the challenge only after its passive check and the hold-still phase, about 0.8 s and 0.6 s later; a blink that happens before the instruction is not counted, by design. The Python detectors found the blink in all three sequences when they read them from the start (section 1.4). In the live client the instruction comes first and the person blinks after it.

DET curve: not drawn for this run, which has one attempt per type and condition; see section 1.2.

## 4. Performance

Measured on 19 October 2026 with the run of section 3 (`ReplayBenchmarkTest`, 30 sequences, 551 analysed frames), on the machine that recorded the captures.

| Measure | Value |
| --- | --- |
| Mean analysis time of one frame (decode, detection, quality, classifiers, and the landmarks during a challenge) | 56.4 ms over the 551 frames, 54.4 to 59.1 ms per run, against the 100 ms of section 5.3 of the decision flow. The first sequence of each run takes 73 to 114 ms: start of a new JVM |
| Mean passive decision time | Live face: 1.31 s (1.19 to 1.47 s, 3 sessions). Attack: 0.91 s (0.76 to 1.54 s, 15 sessions) |
| Mean challenge time | 5.3 s from the start of the challenge to `PASSED` (3.3 to 7.1 s, 6 challenges), hold-still phase included |
| Retry rate | Not measured: the replay uses one attempt per sequence. Needs client sessions |
| Failure rate (bona fide sessions ending in `MAX_ATTEMPTS_EXCEEDED`) | Not measured, as above. First-attempt failure of the bona fide replays: 3 of 12, all in challenges (2 replay effects on `BLINK`, 1 `FACE_CONTINUITY_LOST`) |
| CPU of the client during a session | Engine process during analysis: 1.9 to 3.1 cores of the 8 logical processors (194 to 309 % of one core); about 1 core when most frames are rejected before the classifiers (paper mask) |
| Memory of the client during a session | Java heap used after each sequence: 22 to 99 MB. The native memory of ONNX Runtime (models and inference buffers) is not included |
| Machine: processor, memory, operating system, camera | Intel Core i7-8550U (4 cores, 8 threads, 1.8 GHz), 16 GB, Windows 11 Pro 10.0.26300, integrated camera; mains power, Windows power plan "Balanced"; JDK 11.0.32 (Temurin) |

Second machine, for comparison (14 October): Intel Core Ultra 7 155H, 16 GB, Windows 11 Pro, JDK 11, power mode "Best performance", one sample image analysed repeatedly: 60 ms per frame in the passive check, 69 ms during a challenge. With the "Balanced" mode and other applications open, the same machine took 190 and 320 ms: on hybrid processors, Windows can run a background Java process on the efficiency cores. The power mode is worth setting on registration workstations.

The device stream of the simulated device in webcam mode reached 6 to 8 frames per second on the first machine (#17), below the 10 analysed frames per second the engine can handle; the analysis rate then follows the stream.

## 5. Replay automation and SBI interoperability

| Test | Result |
| --- | --- |
| Recorded sequences replayed through the simulated device, decisions identical to the live run | Device side: section 5.1. Engine decisions on the same sequences: section 3. Comparison through the client: not run |
| Stream of the simulated device received by the client without mirroring | Pass on Collab, 23 October: the turn challenge of the onboarding passed in the direction asked, which a mirrored stream would invert |
| Stream kept running during `RCAPTURE` | Pass, 21 October: the simulated device in webcam mode keeps sending frames during `RCAPTURE` and ends the stream when the capture completes ([decision-flow.md](decision-flow.md), section 6) |
| Capture response signature validated by the client | Pass on Collab, 23 October: the onboarding capture, signed with the device keys MOSIP provided for the environment, was accepted. With the public test keys, the client refuses it (`REG-MDM-108`, [dev-setup-windows.md](dev-setup-windows.md), section 8) |
| Device unavailable and reconnection handled | not run |

### 5.1 Device-level scenarios, automated

`mock-sbi/scripts/replay_scenarios.py` starts the simulated device in `replay` mode on a recorded sequence, calls it as the Registration Client does (MOSIPDINFO, MOSIPDISC, STREAM, RCAPTURE), checks the stream and the capture against the expected behaviour and stops the device, once per SBI specification version and per device configuration, without manual action.

Run of 19 October 2026 on the S10 sequences, on the machine that recorded them (JDK 11, Windows 11), observation of 8 s per run. SBI versions 0.9.5 and 1.0, the two supported by Registration Client 1.2.0.2; device configurations: recorded timestamps, and a fixed 5 frames per second.

| Scenario | Sequence | Expected | 0.9.5, timestamps | 0.9.5, 5 per second | 1.0, timestamps | 1.0, 5 per second |
| --- | --- | --- | --- | --- | --- | --- |
| Live face | `bona_fide` | frames change, RCAPTURE returns an ISO/IEC 19794-5 record with the version echoed | pass | pass | pass | pass |
| Live face, blink challenge | `action_blink` | as above | pass | pass | pass | pass |
| Photo | `print_matte` | as above | pass | pass | pass | pass |
| Screen | `screen_phone` | as above | pass | pass | pass | pass |
| Replayed video | `replay_video` | as above | pass | pass | pass | pass |
| Corrupted frames | `bona_fide`, every third frame truncated | incomplete JPEG frames on the stream | pass (80 of 240) | pass | pass | pass |

24 of 24 runs passed. The stream carried 208 to 241 frames in 8 s, 80 distinct frames at the recorded timestamps and 40 at 5 per second, as expected from the pacing of section `replay` of `mock-sbi/README.md`.

End of sequence, at 30 frames per second so that the 99-frame sequence ends after 3.3 s, within the observation:

| Scenario | Expected | 0.9.5 | 1.0 |
| --- | --- | --- | --- |
| Disconnection (`end=stop`) | the stream ends after the sequence | pass, ended after 3.4 s | pass, ended after 3.4 s |
| Frozen image (`end=hold`) | the last frame repeats | fail on the verdict, see below | fail on the verdict |

The frozen stream behaved as specified (the last frame kept coming after 3.3 s), but the verdict of that run expected one distinct frame over the whole observation. The review of that run (#29, 19 October) also found that the STREAM request asked for a 10 s timeout, so observations longer than 10 s saw the stream end, and that RCAPTURE still returned a record after `end=stop`. All three are fixed after that run: the verdict now looks at the last 1.5 s of the observation, the STREAM timeout follows the observation, and an unplugged replay refuses the capture and stays unplugged for the following requests until the device restarts. Check of the fixes on 21 October, on the sequences built from the sample images (Windows 11, JDK 11), observation of 12 s at 30 frames per second:

| Scenario | Expected | 0.9.5 | 1.0 |
| --- | --- | --- | --- |
| Observation longer than 10 s | the stream lasts the whole observation | pass, 378 frames in 12 s | pass, 374 frames |
| Frozen image (`end=hold`) | the last frame repeats at the end | pass | pass |
| Disconnection (`end=stop`) | the stream ends after the sequence and RCAPTURE gives no record | pass: ended after 1.0 s, RCAPTURE error 710 | pass: same |

The two end cases can be run again on the S10 sequences with the same command for completeness; the fixes do not depend on the content of the frames.

"No face" and "two faces" sequences are not part of the capture protocol and were not recorded. Their engine behaviour (guidance `NO_FACE` and `MULTIPLE_FACES`, then `FACE_CONTINUITY_LOST` during a challenge) is covered by the session tests of the engine (`SessionCorePassiveTest`, `SessionCoreActiveTest`, #15).

Earlier run, 19 October, on sequences built from the sample images of the reference project (one image repeated, 3 s long, observation of 6 s, on another machine): frozen image, disconnection and corrupted frames passed for both versions and both configurations, 12 of 12.
