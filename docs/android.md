# Android design note

Status on 8 October 2026: design only. The submission contains no Android code (#32). This note explains how the same models and the same decision flow would run in the Android Registration Client, what has to be decided first, and what would have to be written.

Facts about the Android client come from the `master` branch of [mosip/android-registration-client](https://github.com/mosip/android-registration-client) (MIT licence), read on 8 October 2026.

## 1. The Android Registration Client today

| Topic | What the code shows |
| --- | --- |
| Structure | A Flutter application (Dart) for the screens, and native Java modules (`clientmanager`) for the business logic. The two talk through Pigeon APIs, for example `BiometricsDetailsApi`, `AuthenticationApi`, `PacketAuthenticationApi` |
| Build | `minSdkVersion 28`, `compileSdkVersion 34`, Java source and target 21 |
| Biometric devices | A separate SBI application, called with Android intents: discovery with `io.sbi.device`, then `<callbackId>.Info` and `<callbackId>.rCapture`, through `startActivityForResult` |
| Face capture | The SBI application opens the camera, captures, and returns the signed biometrics. The client reads the face image from the returned record (`FaceBDIR`) |
| Camera | The client declares no `CAMERA` permission. It never receives a frame stream: there is no stream intent |
| Configuration | Global parameters synchronised from the server, read through `GlobalParamRepository` |
| Texts | Flutter localisation files, `assets/l10n/app_<language>.arb` |

## 2. What this changes

On the desktop, liveness runs on the stream that the device sends before the capture ([workflows.md](workflows.md)). On Android, the client has no stream: the SBI application holds the camera from the request to the result. The desktop design cannot be reused as it is. Something has to give the engine frames.

## 3. Options

| Option | How it works | For | Against |
| --- | --- | --- | --- |
| A. Liveness in the client, with the camera of the tablet | The client opens the camera with CameraX, runs the liveness session on its frames, then sends `rCapture` once the session reaches `PASSED`. The face returned by the SBI application goes through the captured image check of [decision-flow.md](decision-flow.md), section 6 | Same engine, same rules, same screens as the desktop. Works whatever the SBI application. The client keeps control of the policy, as the problem statement asks. The recommended stack of the problem statement lists the Android camera APIs | Needs the `CAMERA` permission. When the SBI application uses another camera than the tablet's, positions and sizes cannot be compared, and the link between the checked frames and the captured image gets weaker |
| B. Liveness in the SBI application | The SBI application runs the engine on its own frames before it answers `rCapture`, and includes the result in its signed response | The decision is made on the frames of the capture itself, and the response is signed | Depends on each device vendor. The client cannot apply its own thresholds, challenges or attempt policy. Out of the Registration Client |
| C. A stream intent | An extension of the SBI specification for Android, so that the device sends frames as on the desktop | Closest to the desktop design | Needs a change of the MOSIP specification and of every SBI application. Not considered |

**Recommendation: option A** for the Registration Client, with the captured image check. When the SBI application uses the tablet's own camera, the check compares the same scene as on the desktop. Option B can come later for certified L1 devices, as a second layer.

With option A, the client must release the camera before it sends `rCapture`, so that the SBI application can open it.

## 4. Reusing the engine

| Topic | Desktop | Android | Consequence |
| --- | --- | --- | --- |
| Bytecode | Java 11 | The client compiles Java 21 sources, `minSdkVersion 28` | The engine JAR compiled for Java 11 can be used as it is |
| Inference | `com.microsoft.onnxruntime:onnxruntime` | `com.microsoft.onnxruntime:onnxruntime-android` | Same Java API (`ai.onnxruntime`). Version 1.30.0 of both on Maven Central on 8 October 2026 |
| Image decoding | `javax.imageio` is available | `java.awt` and `javax.imageio` do not exist | **Requirement for the engine API (#10, #11):** the core works on decoded pixels (width, height, B, G, R bytes) and never imports `java.awt` or `javax.imageio`. Decoding sits in a small platform adapter: `ImageIO` on the desktop, the YUV image of CameraX or `BitmapFactory` on Android |
| Crop and resize | Plain arrays | Plain arrays | Already required for parity with Python (`training/README.md`) |
| Logging | SLF4J API | SLF4J API with a logcat binding, or none | No change in the engine |
| Time, randomness | `java.time.Clock`, `SecureRandom` | Available | No change |
| Models | In the JAR | In the APK, same four ONNX files (10.4 MB) and `models.json` | Same checksums checked at startup |

Packaging can stay a single engine module, provided the decoder is an interface and the core has no platform import. A split into `liveness-engine-core` and two platform adapters can come with the Android work.

## 5. What would have to be written for option A

| Part | Work |
| --- | --- |
| Android adapter of the engine | CameraX `ImageAnalysis` use case delivering frames at `frame.sampling_ms`, YUV to BGR conversion, rotation of the frame from `rotationDegrees`, release of the camera before `rCapture` |
| Native API | A Pigeon API, for example `LivenessApi`: start a session for a workflow, receive state, feedback code and progress for each frame, retry, result. Implemented in `api_services` next to `BiometricsDetailsApi` |
| Capture | In `BiometricsDetailsApi`, send the `rCapture` intent only after `PASSED`, then submit the face read from `FaceBDIR` to the captured image check |
| Authentication | The same gate for operator and supervisor face authentication (`AuthenticationApi`, `PacketAuthenticationApi`) |
| Flutter screens | A liveness widget on the face capture and authentication screens, following [ui/README.md](ui/README.md): positioning oval with progress, instruction band, attempts, failure and exception states |
| Texts | The keys of [messages.md](messages.md) added to the `.arb` files |
| Configuration | The keys of [configuration.md](configuration.md), read through `GlobalParamRepository` |
| Audit | The events of [decision-flow.md](decision-flow.md), section 9, through `AuditManagerService` |
| Permission | `android.permission.CAMERA` in the manifest, requested at runtime |

## 6. Low-resource devices

The problem statement lists optimisation for low-resource Android devices and hardware acceleration as bonus tasks. Levers, to measure on a real tablet:

- the hardware execution providers of ONNX Runtime on Android (NNAPI), with the CPU as fallback;
- int8 quantization of the models, kept only if the parity tests still pass;
- a longer `frame.sampling_ms`, keeping it short enough for blinks ([decision-flow.md](decision-flow.md), section 5.3);
- a smaller detector input when the face fills the frame.

On the desktop reference machine, the three models take 50 to 70 ms per frame in Python ([training/README.md](../training/README.md)). No Android figure exists yet.

## 7. Points to verify before any Android work

1. Whether the face SBI applications used with the Android client open the tablet's own camera, or an external one.
2. That CameraX analysis frames are not mirrored, as the head-turn rule assumes ([decision-flow.md](decision-flow.md), section 2).
3. With the mentors: whether option A is acceptable for MOSIP, since it adds a camera use to the client outside the SBI flow.
