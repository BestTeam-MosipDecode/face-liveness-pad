# Test results

Status on 13 October 2026: skeleton. Nothing has been measured yet. Every value marked "not run" is filled in when the corresponding test is done, following [test-plan.md](test-plan.md). No face image appears in this document.

## 1. Passive model on the team dataset

PAD subsystem evaluation (ISO/IEC 30107-3, clause 7.3), computed by `training/scripts/evaluate.py` (#7) on the test split of [test-plan.md](test-plan.md), section 6.

### 1.1 Context

| Item | Value |
| --- | --- |
| Date | not run |
| Model set version | not run |
| Thresholds: pass, attack | not run |
| Subjects: calibration split, test split | not run |
| Skin tone bands (MST 1-3, 4-7, 8-10), subjects per band | not run |
| Cameras and resolutions | not run |
| Machine for the timings | not run |

### 1.2 Error rates on the test split

| Type | Sequences | APCER or BPCER | Pass zone | Challenge zone | Attack zone | No decision |
| --- | --- | --- | --- | --- | --- | --- |
| `print_matte` | not run | not run | not run | not run | not run | not run |
| `print_glossy` | not run | not run | not run | not run | not run | not run |
| `screen_phone` | not run | not run | not run | not run | not run | not run |
| `screen_laptop` | not run | not run | not run | not run | not run | not run |
| `replay_video` | not run | not run | not run | not run | not run | not run |
| `paper_mask` | not run | not run | not run | not run | not run | not run |
| Highest APCER | | not run | | | | |
| `bona_fide` (BPCER) | not run | not run | not run | not run | not run | not run |
| ACER, benchmark figure | | not run | | | | |

DET curve: not run.

### 1.3 BPCER by condition

| Group | Sequences | BPCER |
| --- | --- | --- |
| Light `normal` | not run | not run |
| Light `low` | not run | not run |
| Light `backlight` | not run | not run |
| Skin tone MST 1-3 | not run | not run |
| Skin tone MST 4-7 | not run | not run |
| Skin tone MST 8-10 | not run | not run |

### 1.4 Actions

| Action | Sequences | Detected | Other action detected |
| --- | --- | --- | --- |
| `action_blink` | not run | not run | not run |
| `action_smile` | not run | not run | not run |
| `action_turn_left` | not run | not run | not run |
| `action_turn_right` | not run | not run | not run |

### 1.5 Time per frame

| Step | Mean time (ms) |
| --- | --- |
| Face detection (YuNet) | not run |
| Landmarks (Face Mesh V2) | not run |
| Classifiers (two MiniFASNet) | not run |
| Total | not run |

## 2. Functional test matrix

Cases of [test-plan.md](test-plan.md), section 9 (#27). Workflows: R resident capture, O operator authentication, S supervisor authentication. Online and offline as defined in the plan.

| ID | Case | Expected | R online | R offline | O online | O offline | S online | S offline |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F01 | Live face, good light | `PASSED` without challenge, capture accepted, `LIVENESS_SUCCESS` | not run | not run | not run | not run | not run | not run |
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

## 3. System PAD campaign

Full system evaluation (ISO/IEC 30107-3, clause 7.5), on the complete Registration Client with passive and active checks (#28).

### 3.1 Context

| Item | Value |
| --- | --- |
| Date | not run |
| Client version, engine version, model set version | not run |
| Settings different from the defaults | not run |
| Subjects, attack presenters | not run |
| Lighting conditions | not run |
| Cameras, machine | not run |

### 3.2 Artefact register

| Artefact | Species | Subject | Made with |
| --- | --- | --- | --- |
| not run | | | |

### 3.3 Error rates

| Type | Attempts | APCER or BPCER | Challenge rate |
| --- | --- | --- | --- |
| `print_matte` | not run | not run | not run |
| `print_glossy` | not run | not run | not run |
| `screen_phone` | not run | not run | not run |
| `screen_laptop` | not run | not run | not run |
| `replay_video` | not run | not run | not run |
| `paper_mask` | not run | not run | not run |
| Highest APCER | | not run | |
| Bona fide (BPCER) | not run | not run | not run |
| ACER, benchmark figure | | not run | |

DET curve: not run.

## 4. Performance

| Measure | Value |
| --- | --- |
| Mean passive decision time | not run |
| Mean challenge time | not run |
| Retry rate | not run |
| Failure rate (bona fide sessions ending in `MAX_ATTEMPTS_EXCEEDED`) | not run |
| CPU of the client during a session | not run |
| Memory of the client during a session | not run |
| Machine: processor, memory, operating system, camera | not run |

## 5. Replay automation and SBI interoperability

| Test | Result |
| --- | --- |
| Recorded sequences replayed through the simulated device, decisions identical to the live run | not run |
| Stream of the simulated device received by the client without mirroring | not run |
| Stream kept running during `RCAPTURE` | not run |
| Capture response signature validated by the client | not run |
| Device unavailable and reconnection handled | not run |
