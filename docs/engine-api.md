# Engine API

Status on 14 October 2026: first version. Java API of `liveness-engine`, package `io.mosip.registration.liveness.api`. The rules behind each result are in [decision-flow.md](decision-flow.md).

## 1. Overview

| Type | Role |
| --- | --- |
| `LivenessEngine` | Created once. Checks the model files against `models.json`, loads them, opens sessions |
| `LivenessSession` | One face capture or one face authentication. Receives frames, publishes updates and audit events |
| `LivenessPolicy` | Parameters of a workflow, immutable, built from the defaults of section 10 of the decision flow |
| `FrameDecoder` | Platform adapter that decodes frames. `ImageIoFrameDecoder` on the desktop |
| `LivenessEventListener` | Receives `LivenessUpdate` (screen) and `LivenessEvent` (audit) |
| `CaptureCheck` | Result of the check of the image returned by the capture request |

The engine has no dependency on the Registration Client, on JavaFX or on `java.awt`, so that the Android client can reuse it with its own decoder.

## 2. Use

```java
LivenessEngine engine = LivenessEngine.create(new ImageIoFrameDecoder());   // once, at start-up

AtomicBoolean captureRequested = new AtomicBoolean();
LivenessSession session = engine.newSession(Workflow.RESIDENT, policy, new LivenessEventListener() {
    @Override
    public void onUpdate(LivenessUpdate update) {
        Platform.runLater(() -> showOnScreen(update));          // worker thread -> JavaFX thread
        if (update.state() == SessionState.PASSED && captureRequested.compareAndSet(false, true)) {
            requestCapture();                                   // RCAPTURE once, only after PASSED
        }
    }

    @Override
    public void onEvent(LivenessEvent event) {
        audit(event);                                           // no score, no image
    }
});

// For each frame of the device stream (Streamer thread):
session.submit(jpegBytes, System.currentTimeMillis());

// When the capture request returns:
CaptureCheck check = session.checkCapture(capturedImageBytes);
if (check.accepted()) {
    store(capturedImageBytes);
}

// After a failure, if update.retryAvailable():
captureRequested.set(false);
session.retry();
```

## 3. Life cycle

- `LivenessEngine.create(decoder)` reads `models.json`, compares the size and the SHA-256 of each model file, then loads the four models. A missing or modified file throws `ModelIntegrityException`, whose message names the file: the engine does not start.
- One session is active at a time. `newSession` closes the previous one.
- `session.close()` stops the session. `engine.close()` closes the active session and releases the models.

## 4. Threads

- `submit` returns at once. A single worker thread per session analyses the frames. A frame is skipped while another one is analysed, or when it comes less than `samplingMs` after the last analysed frame. A frame whose timestamp goes back is analysed.
- `deviceEvent`, `retry` and `checkCapture` are never skipped. `checkCapture` and `retry` wait for their result at most 5 seconds; after that, `checkCapture` refuses the image with `ENGINE_ERROR`.
- The listener runs on the worker thread. Calling `retry` or `checkCapture` from the listener is allowed.

## 5. Policy

| Builder method | Configuration key (`mosip.registration.face.liveness.` + key) | Default |
| --- | --- | --- |
| `passThreshold` | `passive.threshold.pass` | 0.85 |
| `attackThreshold` | `passive.threshold.attack` | 0.30 |
| `minFrames` | `passive.min_frames` | 5 |
| `windowMs` | `passive.window_ms` | 3000 |
| `passiveTimeoutMs` | `passive.timeout_ms` | 20000 |
| `samplingMs` | `frame.sampling_ms` | 100 |
| `minFacePx` | `quality.min_face_px` | 120 |
| `maxYawDeg` | `quality.max_yaw_deg` | 20 |
| `activeEnabled` | `active.enabled` | true |
| `minChallenges` | `active.min_challenges` | 2 |
| `challengeTypes` | `active.challenge_types` | BLINK, SMILE, TURN_LEFT, TURN_RIGHT |
| `challengeTimeoutMs` | `active.challenge_timeout_ms` | 8000 |
| `baselineMs` | `active.baseline_ms` | 600 |
| `maxAttempts` | `max_attempts` | 3 |
| `diagnostic` | `diagnostic.enabled` | false |

Invalid values throw `IllegalArgumentException`. The configuration provider of the client falls back to the default for a missing or invalid key ([configuration.md](configuration.md)). `enabled` and `on_max_attempts` stay in the client: with liveness disabled, no session is created; after `MAX_ATTEMPTS_EXCEEDED`, the client blocks or starts the exception process.

## 6. Updates

`LivenessUpdate` holds the state, a `FeedbackCode`, an optional `GuidanceTip`, the challenge in progress, a progress value between 0 and 1, the attempt number and the maximum, the failure reason, `retryAvailable` and `finished`. The score is present only when `diagnostic` is true.

| Feedback code | Message key |
| --- | --- |
| `CHECKING` | `LIVENESS_CHECKING` |
| `NO_FACE` | `LIVENESS_NO_FACE` |
| `MULTIPLE_FACES` | `LIVENESS_MULTIPLE_FACES` |
| `LOW_QUALITY` | the key of the tip: `LIVENESS_TIP_STAY_IN_FRAME`, `LIVENESS_TIP_LIGHTING`, `LIVENESS_HOLD_STILL`, `LIVENESS_TIP_LOOK_CAMERA` |
| `HOLD_STILL` | `LIVENESS_HOLD_STILL` |
| `CHALLENGE` | `LIVENESS_CHALLENGE_BLINK`, `_SMILE`, `_TURN_LEFT`, `_TURN_RIGHT` |
| `KEEP_GOING` | `LIVENESS_KEEP_GOING` |
| `ACTION_DETECTED` | `LIVENESS_ACTION_DETECTED` |
| `SUCCESS` | `LIVENESS_SUCCESS` |
| `FAILED` | `LIVENESS_GENERIC_FAILURE` for `PAD_ATTACK_DETECTED` and `ENGINE_ERROR`, otherwise `LIVENESS_FAILED` with the tip; adds `LIVENESS_RETRY_AVAILABLE` when `retryAvailable` |
| `MAX_ATTEMPTS_EXCEEDED` | `LIVENESS_MAX_ATTEMPTS` (resident) or `LIVENESS_MAX_ATTEMPTS_AUTH` (operator, supervisor) |
| `DEVICE_ERROR` | `LIVENESS_DEVICE_UNAVAILABLE` |

The screen never says that an attack was suspected: `PAD_ATTACK_DETECTED` shows the same message as an engine error.

## 7. Capture check

After `PASSED`, the session keeps following the face on the stream. `checkCapture` accepts the captured image when it decodes, holds exactly one face, passes the quality checks, scores above the attack threshold, and is consistent in position and size with the last face followed (section 6 of the decision flow). A refused image ends the attempt with its reason. A second call after a success is refused without changing anything.

## 8. Audit events

`LivenessEvent` types: `SESSION_STARTED`, `ACTIVE_TRIGGERED`, `CHALLENGE_PASSED`, `CHALLENGE_FAILED`, `PASSED`, `FAILED`, `MAX_ATTEMPTS`, `DEVICE_ERROR`. Each carries the workflow, the attempt number, the duration since the start of the attempt and the model set version, and when relevant the failure reason, the challenge type or the device event. No score, measure or image.

## 9. Errors

| Situation | Result |
| --- | --- |
| Frame that does not decode | Ignored, counted for diagnostics |
| Device problem before the first analysed frame | Update `DEVICE_ERROR`, no attempt used |
| Device problem later | Attempt failed with `DEVICE_ERROR` |
| Exception inside the models | Attempt failed with `ENGINE_ERROR`; nothing is thrown to the caller |
| Model file missing or modified | `ModelIntegrityException` from `LivenessEngine.create` |
| Engine closed | `newSession` throws `IllegalStateException` |
