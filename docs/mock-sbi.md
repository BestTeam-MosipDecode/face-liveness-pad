# Simulated face device (mock-sbi)

Status on 9 October 2026: study of the MOSIP mock device (#16). Installation, modes and configuration will be added with #17, #18 and #19.

This document describes how the MOSIP mock device works, what the Registration Client 1.2.0.2 expects from a face device, and how we build `mock-sbi` on top of it.

Sources studied:

- `mosip/mosip-mock-services`, tag `v1.2.0.2`, folder `MockMDS` (paths below are relative to it);
- our fork of `registration-client`, branch `feature/face-liveness` (paths relative to `registration/`).

## 1. Overview of MockMDS

| Topic | Finding |
| --- | --- |
| Entry point | `io.mosip.mock.sbi.test.TestMockSBI` (`pom.xml`, `mainClass`) |
| Arguments | `mosip.mock.sbi.device.purpose=Registration` and `mosip.mock.sbi.biometric.type=Biometric Device` (or `Face`, `Finger`, `Iris`), as in `run.sh` and `run_reg.bat` |
| HTTP server | Written by hand on a `ServerSocket`, one thread per connection (`service/SBIMockService.java`, `service/SBIWorker.java`). No framework at runtime. The Spring classes under `org/biometric/provider` are legacy code, not used |
| Dispatch | Text matching on the request line, for example `"MOSIPDISC /device HTTP"` (`SBIConstant.java`, `SBIServiceResponse.getServiceresponse`) |
| Configuration | `application.properties`, read from the current directory. All data paths are relative to it, so the service must start from `target/` |
| Build | `mvn clean install` copies `Biometric Devices/`, `Profile/`, `files/`, `application.properties` and the run scripts into `target/` |
| Platform | Pure Java on the runtime path. Runs on Windows, macOS and Linux. The jar name in `run.sh` (`1.2.1-SNAPSHOT`) does not match the pom version and must be fixed |

## 2. Ports

- `server.minport=4501`, `server.maxport=4600`, `server.serveripaddress=127.0.0.1` (`application.properties`).
- The service takes the first free port of the range (`SBIMockService.getAvailabilePort`). One process serves one port and binds to `127.0.0.1` only.
- The client scans `mosip.registration.mdm.portRangeFrom` to `mosip.registration.mdm.portRangeTo`, default 4501 to 4600, and only if the range stays within 4500 to 4600 (`MosipDeviceSpecificationFactory.java`). The host is fixed to `127.0.0.1`.

## 3. Endpoints

| Method and path | Purpose | Notes |
| --- | --- | --- |
| `MOSIPDISC /device` | Device discovery | Body `{"type":"Face"}`. Returns an array with `deviceId`, `deviceStatus`, `certification`, `specVersion`, `callbackId`, `digitalId` (unsigned) |
| `MOSIPDINFO /info` | Device information | Returns `[{"deviceInfo":"<JWS>","error":{...}}]`. The client uses this call to find devices, not `MOSIPDISC` |
| `STREAM /stream` | Preview stream | Body `{"deviceId","deviceSubId","timeout"}`. Face uses `deviceSubId` `0` |
| `RCAPTURE /capture` | Registration capture | Returns `{"biometrics":[{"specVersion","data":"<JWS>","hash","error"}]}` |
| `CAPTURE /capture` | Authentication capture | Not used by the Registration Client |
| `POST /admin/status` | Set the device status | `Ready`, `Busy`, `Not Ready`, `Not Registered` |
| `POST /admin/score` | Set the quality score | `0` to `100`, or read it from the ISO record |
| `POST /admin/delay` | Delay responses | Per method, in ms. A delay longer than the client timeout simulates a timeout |
| `POST /admin/profile` | Choose the profile folder | `Default`, `Automatic` or any folder under `Profile/` |

The admin calls reset the other modalities to their defaults. The device advertises `specVersion` `["0.9.5"]` and certification `L0`.

## 4. Stream format

The response header is written without `Content-Length`, with `Content-Type: multipart/x-mixed-replace; boundary=--BoundaryString`. Each frame is:

```
--BoundaryString\r\n
Access-Control-Allow-Origin:*\r\n
Content-Type:image/jpeg\r\n
Content-Length:<n>\r\n
\r\n
<n bytes of JPEG>\r\n
\r\n
```

- **Frame source.** One fixed JPEG, `Biometric Devices/Face/Stream Image/0.jpeg` (490 x 360), read again from disk for every frame.
- **Rate.** About 30 frames per second (`Thread.sleep(30)` between frames).
- **End of stream.** At the request timeout, after a successful `RCAPTURE`, or when the client closes the socket. The service then writes a second HTTP response on the same socket, which the client ignores.
- **Stream during `RCAPTURE`.** Measured in webcam mode on 21 October 2026 (Windows 11, JDK 11): frames keep coming while `RCAPTURE` runs, and the stream ends with the capture. Without `RCAPTURE`, a 20 s stream runs to its timeout. The client removes its frame listener when it sends `RCAPTURE`, so this end of stream does not affect the liveness session.

The client reads this stream in `Streamer.retrieveNextImage` (`registration-client/.../controller/device/Streamer.java`):

1. it reads bytes until the text `Content-Length:` appears (case sensitive), then reads the number up to the end of line;
2. it skips bytes until the first `0xFF`, which it takes as the start of the JPEG;
3. it reads `n + 1` bytes from there, so one byte after each JPEG is consumed.

Any device we write must therefore keep the exact `Content-Length:` header, send no `0xFF` byte between the header and the JPEG, and keep the trailing `\r\n` after each frame. If the stream ends, the skip loop of step 2 never stops. Simulating a disconnection (#18) will show this behaviour in the client.

## 5. Capture

- **Image.** `RCAPTURE` returns a prepared ISO/IEC 19794-5:2011 record, `Profile/<profile>/Registration/Face.iso`, which contains a JPEG 2000 image. Nothing is converted at runtime.
- **Encoding.** `bioValue` is the base64url encoding of the ISO record. The client decodes it with `FaceDecoder` and the version `ISO19794_5_2011`, so any image we send must be a valid ISO 19794-5:2011 record.
- **Data block.** A JWS containing `deviceCode`, `digitalId` (itself a JWS), `bioType` `Face`, `purpose`, `env`, `bioValue`, `transactionId`, `timestamp` (UTC, `yyyy-MM-dd'T'HH:mm:ss'Z'`), `requestedScore` and `qualityScore` (default 44).
- **Hash.** Upper case hex of `SHA-256(previousHash || SHA-256(image))`.
- **Client checks** (`MosipDeviceSpecification_095_ProviderImpl.java`): signature, timestamp within `mosip.registration.mds.allowed.lag.mins` (5 minutes), non empty quality score, same `transactionId` and `specVersion` as the request.
- **Timing.** The client starts `RCAPTURE` while the stream is still running (`GenericBiometricsController.java`).

## 6. Signatures and test certificates

- The device signs `deviceInfo`, `digitalId` and each capture block in RS256 with jose4j. The `x5c` header carries the device certificate only (`org/biometric/provider/JwtUtility.java`).
- The face key is `Biometric Devices/Face/Keys/mosipface.p12` (alias and password `mosipface`). Its certificate `CN=MosipFace` is issued by `CN=RootCA,O=Mosip` (`Biometric Devices/rootca.cer`), and both are valid until 8 February 2031.
- The authentication key `mosipfaceftm.p12` is issued by `CN=RootCAFTM`, which expired on 10 June 2022. Registration does not use it.
- These keys are public test material. They must never be used outside development and tests.
- The client always validates the signature and the trust chain (`MosipDeviceSpecificationHelper.validateJWTResponse`, `setValidateTrust(true)`). No setting turns this off. The chain is checked against CA certificates synchronised from the server for the `DEVICE` domain (`CertificateSyncServiceImpl.java`). The MOSIP Collab setup guide offers MockMDS to capture biometrics without devices, which suggests the Collab server already trusts `rootca.cer`. This is to be confirmed on a registered machine.

## 7. Profiles and failure simulation

| Need for #18 | Available in MockMDS | Missing |
| --- | --- | --- |
| Fixed image (`static` mode) | Fixed stream JPEG, profile folders for the capture | Choosing the stream image by configuration |
| Webcam (`webcam` mode) | Nothing | Everything |
| Replay of a video or an image folder | Nothing | Everything |
| Device not ready, busy, not registered | `/admin/status` | Nothing |
| Timeout | `/admin/delay` | Nothing |
| Disconnection, interrupted stream, corrupted frame | Nothing | Everything |

## 8. Licence

`mosip-mock-services` is under the Mozilla Public License 2.0 (`LICENSE`, and `pom.xml` for MockMDS), the same licence as this repository. MPL 2.0 applies file by file: the MockMDS files we modify stay under MPL 2.0 with their original notices, and our new files can carry our own MPL 2.0 header. `THIRD_PARTY_NOTICES.md` must list MockMDS.

## 9. Choice: fork or extension

Three options were compared:

| Option | Advantages | Drawbacks |
| --- | --- | --- |
| Use MockMDS unchanged and drive it from outside | No code to maintain | No webcam, no replay, no stream failures: does not cover #17 and #18 |
| Write our own device, for example in Python | Free design, simple frame handling with OpenCV | Must rebuild the JWS signing with `x5c`, the hash chain, the ISO 19794-5 encoding and the exact stream framing. Every detail is a risk of rejection by the client |
| **Fork MockMDS into `mock-sbi/`** | Keeps what the client already accepts: signatures, test certificates, ISO records, hash chain, endpoints, admin API | Old code base (Spring Boot 2.0.2 parent, hand written HTTP) that we must build and keep |

**Decision: fork.** We copy the `MockMDS` sources of tag `v1.2.0.2` into `mock-sbi/`, keep their MPL notices and change only the face part:

1. **Frame source.** Replace the fixed image in `SBIFaceHelper.getLiveStream` with a frame source chosen by configuration: `webcam`, `replay` (video file or image folder) or `static` (current behaviour).
2. **Capture.** In `SBIFaceHelper.getBioCapture`, take the current frame and build the ISO 19794-5:2011 record at runtime with `FaceEncoder` from `biometrics-util`, already a dependency. The prepared `Face.iso` stays the fallback.
3. **Failures.** Add admin calls, or configuration keys, for disconnection, interrupted stream and corrupted frame. Keep `/admin/status` and `/admin/delay` for the other cases.
4. **Webcam library.** OpenCV for Java (`org.openpnp:opencv`), which ships native libraries for Windows x64 and macOS arm64. To be confirmed in #17.
5. **Housekeeping.** Fix the jar name in the run scripts, and add a `run` script for macOS and Linux.

The import of the MockMDS sources will be a separate first commit, so that our changes are easy to review against the original.

## 10. Known limits and risks

- The trust chain of section 6 depends on the server. The full path through the Registration Client needs a machine registered on a MOSIP server; the device itself can be tested with direct HTTP calls.
- The client spins without end if the stream stops (section 4). This is client behaviour, not something the device can fix.
- `SBIWorker` reads the request body as characters and parses `Content-Length` with a fixed offset. This is enough for the client but must be kept in mind if other tools call the device.
- The SBI 1.0 provider of the client builds the stream URL from the port and `serialNo`, not from `callbackId`. Interoperability with SBI 1.0 is checked in #30.
