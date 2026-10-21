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
ExecutorService captureThread = Executors.newSingleThreadExecutor();
LivenessSession session = engine.newSession(Workflow.RESIDENT, policy, new LivenessEventListener() {
    @Override
    public void onUpdate(LivenessUpdate update) {
        Platform.runLater(() -> showOnScreen(update));          // worker thread -> JavaFX thread
        if (update.state() == SessionState.PASSED && captureRequested.compareAndSet(false, true)) {
            captureThread.execute(() -> requestCapture());      // RCAPTURE once, after PASSED, off the worker
        }
    }

    @Override
    public void onEvent(LivenessEvent event) {
        audit(event);                                           // no score, no image
    }
});

// For each frame of the device stream (Streamer thread), with a monotonic clock:
session.submit(jpegBytes, System.nanoTime() / 1_000_000);

// When the capture request returns: RCAPTURE gives an ISO/IEC 19794-5 record holding a JPEG 2000 image.
ConvertRequestDto request = new ConvertRequestDto();
request.setVersion("ISO19794_5_2011");
request.setInputBytes(isoRecord);
BgrImage captured = ImageIoFrameDecoder.toBgr(FaceDecoder.convertFaceISOToBufferedImage(request));
CaptureCheck check = session.checkCapture(captured);
if (check.accepted()) {
    store(isoRecord);
}

// After a failure, if update.retryAvailable():
captureRequested.set(false);
session.retry();
```

## 3. Life cycle

- `LivenessEngine.create(decoder)` reads `models.json`, checks that it lists the four models of the engine, compares the size and the SHA-256 of each model file, then builds the ONNX sessions from the verified bytes. A missing or modified file throws `ModelIntegrityException`, whose message names the file: the engine does not start.
- One session is active at a time. `newSession` closes the previous one.
- `session.close()` stops the session. `engine.close()` closes the active session and releases the models.

## 4. Threads

- `submit` returns at once. A single worker thread per session analyses the frames. A frame is skipped while another one is analysed, or when it comes less than `samplingMs` after the last analysed frame. A frame whose timestamp goes back is not skipped: during an attempt it means that the device restarted its clock, so the attempt fails with `DEVICE_ERROR` (stream interrupted), as a device problem would.
- Frame times come from a monotonic clock (`System.nanoTime() / 1_000_000`), never from the wall clock, which goes back when the system time is adjusted.
- `submit` decodes the frame later, on the worker: the caller must not reuse the array it passed.
- `deviceEvent`, `retry` and `checkCapture` are never skipped. `checkCapture` and `retry` wait for their result at most 5 seconds; after that, a call that has not started yet is dropped: `checkCapture` refuses the image with `ENGINE_ERROR` and `retry` returns `false`. A call already started is awaited and its result returned, since the session has acted on it.
- The listener runs on the worker thread, after each transition is complete. Calling `retry` or `checkCapture` from the listener is allowed. The listener must return quickly and never block: while it runs, no frame is analysed and the face is not followed. The capture request goes to another thread.
- `session.close()` lets an analysis in progress finish without interrupting it; after `close()`, the session changes nothing more; a transition already under way is still delivered in full, so a failure never reaches the audit without its update. `newSession` and `engine.close()` wait up to 2 seconds for the previous worker before going on, so the models are never released during an inference.

## 5. Policy

| Builder method | Configuration key (`mosip.registration.face.liveness.` + key) | Default |
| --- | --- | --- |
| `passThreshold` | `passive.threshold.pass` | 1.00: no passive pass, every session that is not an attack goes to the active check (#7) |
| `attackThreshold` | `passive.threshold.attack` | 0.30 |
| `minFrames` | `passive.min_frames` | 5 |
| `windowMs` | `passive.window_ms` | 3000 |
| `passiveTimeoutMs` | `passive.timeout_ms` | 20000 |
| `samplingMs` | `frame.sampling_ms` | 100 |
| `minFacePx` | `quality.min_face_px` | 120 |
| `maxYawDeg` | `quality.max_yaw_deg` | 20 |
| `activeEnabled` | `active.enabled` | true |
| `minChallenges` | `active.min_challenges` | 2 |
| `challengeTypes` | `active.challenge_types` | BLINK, TURN_LEFT, TURN_RIGHT |
| `challengeTimeoutMs` | `active.challenge_timeout_ms` | 8000 |
| `baselineMs` | `active.baseline_ms` | 600 |
| `maxAttempts` | `max_attempts` | 3 |
| `diagnostic` | `diagnostic.enabled` | false |

Invalid values throw `IllegalArgumentException`. The configuration provider of the client falls back to the default for a missing or invalid key ([configuration.md](configuration.md)). `enabled` and `on_max_attempts` stay in the client: with liveness disabled, no session is created; after `MAX_ATTEMPTS_EXCEEDED`, the client blocks or starts the exception process.

## 6. Updates

`LivenessUpdate` holds the state, a `FeedbackCode`, an optional `GuidanceTip`, the challenge in progress with its position (`challengeIndex()` from 1 and `challengeCount()`, both 0 outside the active check, for "Challenge 1 of 2"), a progress value between 0 and 1, the attempt number and the maximum, the failure reason, `retryAvailable` and `finished`. The score is present only when `diagnostic` is true.

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

RCAPTURE returns an ISO/IEC 19794-5 record holding a JPEG 2000 image, which the engine does not decode. The client extracts the image with the MOSIP `FaceDecoder.convertFaceISOToBufferedImage`, which it already uses to display the captured face, and calls `checkCapture(BgrImage)` with `ImageIoFrameDecoder.toBgr`. `checkCapture(byte[])` takes JPEG or PNG bytes.

A device event after `PASSED` ends the attempt. Once the capture is requested, the client stops reporting the end of the stream (`LivenessFrameSource.captureRequested()` in the Registration Client), since a device may end its stream after the capture.

After `PASSED`, the session keeps following the face on the stream. `checkCapture` accepts the captured image when it decodes, holds exactly one face, passes the quality checks, scores above the attack threshold, and is consistent in position and size with the last face followed (section 6 of the decision flow). A refused image ends the attempt with its reason. A second call after a success is refused without changing anything.

## 8. Audit events

`LivenessEvent` types: `SESSION_STARTED`, `ACTIVE_TRIGGERED`, `CHALLENGE_PASSED`, `CHALLENGE_FAILED`, `PASSED`, `FAILED`, `MAX_ATTEMPTS`, `DEVICE_ERROR`. Each carries the workflow, the attempt number, the duration since the start of the attempt and the model set version, and when relevant the failure reason, the challenge type or the device event. No score, measure or image.

## 9. Errors

| Situation | Result |
| --- | --- |
| Frame that does not decode | Ignored, counted for diagnostics |
| Device problem before the first analysed frame | Update `DEVICE_ERROR`, no attempt used |
| Device problem later | Attempt failed with `DEVICE_ERROR` |
| Frame timestamp that goes back during an attempt | Attempt failed with `DEVICE_ERROR`, audit event with `STREAM_INTERRUPTED` |
| Exception inside the models | Attempt failed with `ENGINE_ERROR`; nothing is thrown to the caller |
| Model file missing or modified | `ModelIntegrityException` from `LivenessEngine.create` |
| Engine closed | `newSession` throws `IllegalStateException` |
