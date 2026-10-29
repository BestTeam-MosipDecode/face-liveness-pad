# Developer setup on Windows

How to build the liveness engine and the modified MOSIP Registration Client on a Windows workstation, and how to run them.

Status on 23 October 2026: sections 1 to 4 and 8 were verified from a clean checkout of both repositories on the team's reference machine (Windows 11, JDK 11.0.32, Maven 3.9.9). Sections 5 to 8 and 11 were verified on the MOSIP Collab environment from 21 to 23 October, up to the operator onboarding, which the server refused (section 11). Section 7 describes the command-line run used by the team; a run from an IDE was not tried. The notes on antivirus software in sections 1 and 8 come from the runs of 19 October on the same machine.

## 1. Prerequisites

| Item | Required | Used by the team |
| --- | --- | --- |
| Operating system | Windows 10 or 11, 64-bit | Windows 11 Pro |
| TPM | version 2.0, enabled | version 2.0 |
| JDK | 11 | Eclipse Temurin 11.0.32 |
| Maven | 3.x | 3.9.9 |
| Git | any recent version | 2.47 |
| Python | 3.10, only for the tools of `training/` and `mock-sbi/scripts/` | 3.10 |
| WireGuard | only for the MOSIP Collab environment (section 6) | WireGuard for Windows |

Check the TPM from a terminal. No administrator rights are needed.

```powershell
tpmtool getdeviceinformation
```

The output must report that a TPM is present, in version 2.0.

The project is based on Registration Client 1.2.0.2, which targets Java 11. If another JDK is the default on the machine, select JDK 11 for the current PowerShell session before calling Maven:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-11.0.32.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
java -version
```

All commands below are for PowerShell. In PowerShell, keep the quotes around Maven options that contain a dot, such as `"-Dgpg.skip"`: without them, PowerShell splits the option at the dot and Maven stops with `Unknown lifecycle phase ".skip"`.

If a download stops with `PKIX path building failed` and `unable to find valid certification path to requested target`, an antivirus that scans HTTPS traffic is presenting its own certificate to Maven. Windows trusts that certificate, the JDK does not. On the reference machine the antivirus is Avast, and `curl` reached Maven Central while Maven could not. Let Maven use the Windows certificate store for the current session:

```powershell
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
```

Then run the same build command again with `-U` added once: Maven remembers a failed download and does not retry it otherwise.

## 2. Get the sources

The work is split across two repositories: this one, which holds the liveness engine, the simulated device and the documentation, and the team's fork of the Registration Client, which holds the client changes on the `feature/face-liveness` branch.

```powershell
git clone https://github.com/BestTeam-MosipDecode/face-liveness-pad.git
git clone -c core.longpaths=true --branch feature/face-liveness https://github.com/BestTeam-MosipDecode/registration-client.git
```

Some paths of the Registration Client are longer than the 260 characters Windows allows by default once the folder of the clone is added. Without `-c core.longpaths=true`, the clone ends with `Filename too long` and `unable to checkout working tree`. Cloning into a short folder, for example `C:\mosip`, also avoids it.

`feature/face-liveness` starts from `master`, which holds the 1.2.0.2 release. The only differences between `master` and the `v1.2.0.2` tag are the README and the issue templates.

## 3. Build the liveness engine

The client depends on the engine, `io.mosip.registration:liveness-engine:0.1.0-SNAPSHOT`, which is not published in any public repository. Build it and install it into the local Maven repository first. From `face-liveness-pad/liveness-engine`:

```powershell
mvn clean install
```

The build runs the tests of the engine, 193 of them, and takes about one minute. The first build downloads ONNX Runtime for Java, about 90 MB. The engine embeds its four models and checks them at startup against `models.json`.

## 4. Build the Registration Client

From `registration-client/registration`:

```powershell
mvn clean install "-Dgpg.skip" -DskipTests
```

The reactor builds six projects: the parent POM, `registration-api`, `registration-api-stub-impl`, `registration-services`, `registration-client` and `registration-test`. All MOSIP dependencies of this release are published on Maven Central. From a clean checkout with a populated local Maven repository, the build takes about five minutes on the reference machine.

`registration-client/target/lib` then holds the libraries of the client, among them `liveness-engine-0.1.0-SNAPSHOT.jar` (6 MB, models included) and `onnxruntime-1.19.2.jar` (89 MB, with its native libraries).

If the build stops with `Could not find artifact io.mosip.registration:liveness-engine`, the engine was not installed: go back to section 3.

If the build stops with `Failed to delete ...\target`, the Java language server of an open IDE was writing into that folder while Maven was cleaning it. Run the command again from the module that failed, for example:

```powershell
mvn clean install "-Dgpg.skip" -DskipTests -rf :registration-services
```

## 5. Select the MOSIP environment

The target environment is set in `registration/registration-services/src/main/resources/props/mosip-application.properties`:

| Property | Default value | Collab |
| --- | --- | --- |
| `mosip.hostname` | `dev.mosip.net` | `api-internal.collab.mosip.net` |
| `mosip.client.upgrade.server.url` | `https://dev.mosip.net` | `https://collab.mosip.net` |

The client reads this file from the classpath at startup. Release 1.2.0.2 offers no environment variable or command-line override, so the file has to be edited and the client rebuilt. From `registration-client/registration`:

```powershell
mvn clean install "-Dgpg.skip" -DskipTests -pl registration-services,registration-client
```

Keep this edit out of the Git history:

```powershell
git update-index --skip-worktree registration/registration-services/src/main/resources/props/mosip-application.properties
```

`api-internal.collab.mosip.net` is a private address, reachable only through the WireGuard tunnel of section 6. With the tunnel active, this request answers `UP`:

```powershell
curl.exe https://api-internal.collab.mosip.net/v1/syncdata/actuator/health
```

Behind an antivirus that scans HTTPS, add `--ssl-no-revoke` to that command. No client version is published on Collab: at each start the update check fails with `Failed to check if update is available or not`, which does not block the client.

## 6. Register the machine on Collab

The Registration Client only talks to a MOSIP server that knows the TPM public key of the machine. For Collab, the procedure is described in the [Registration Client Collab Guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/test/collab-reg-client-setup-guide):

1. Download the TPM utility linked in the guide and run `java -jar tpmutility-0.0.2.jar > tpmdetails.txt`.
2. Send the content of `tpmdetails.txt` through the request form linked in the guide, with the name of the machine.
3. MOSIP registers the machine, then sends the operator credentials and the WireGuard configuration by e-mail.
4. Import the configuration into WireGuard and activate the tunnel before starting the client. With the tunnel off, the health request of section 5 times out and the client cannot sign in.
5. Until MOSIP activates the machine, the first sign-in fails with `KER-SYN-AUTH-001` ("Unable to get Authentication Token"), even with the right password. Ask MOSIP to check that the machine is registered, active and mapped to the centre of the operator account. Once it is, the first sign-in runs the initial synchronisation.

Credentials and the WireGuard configuration never go into a repository: the configuration holds a private key.

## 7. Run the client

Main class: `io.mosip.registration.controller.Initialization`, in the `registration-client` module. It sets the logging configuration, then starts `ClientApplication` with its preloader. The team runs it from a terminal; JavaFX is already in `registration-client/target/lib`, so no separate OpenJFX SDK is needed. A run from an IDE is described in the [MOSIP developers guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/develop/registration-client-developers-guide) and was not tried.

1. Fetch the biometric SDK that the Collab configuration declares (`io.mosip.mock.sdk.impl.SampleSDK`) and the converter it uses. Without them, the onboarding stops with `BIO_SDK_007`.

   ```powershell
   mvn dependency:get "-Dartifact=io.mosip.mock.sdk:mock-sdk:1.2.0.2" "-Dtransitive=false"
   mvn dependency:get "-Dartifact=io.mosip.kernel:kernel-bio-converter:1.2.0.1:jar:lib" "-Dtransitive=false"
   ```

2. Create a working folder outside both repositories, for example `C:\mosip\regclient-run`, and copy `registration-client\registration\lib\logback.xml` into its `lib` subfolder: `Initialization` loads `lib/logback.xml` relative to the working directory. The client writes its encrypted database, its logs and its packets in this folder. Keep it out of any repository, since it holds the data of the test registrations, and delete it when the tests are over.

3. Start the client from that folder, with JDK 11 selected (section 1) and the paths adapted:

   ```powershell
   $client = "C:\mosip\registration-client\registration\registration-client\target"
   $m2 = Join-Path $env:USERPROFILE ".m2\repository"
   $sdk = "$m2\io\mosip\mock\sdk\mock-sdk\1.2.0.2\mock-sdk-1.2.0.2.jar;$m2\io\mosip\kernel\kernel-bio-converter\1.2.0.1\kernel-bio-converter-1.2.0.1-lib.jar"
   Set-Location C:\mosip\regclient-run
   java "-Dfile.encoding=UTF-8" "-Djavax.net.ssl.trustStoreType=Windows-ROOT" -cp "$client\registration-client-1.2.0.2.jar;$client\lib\*;$sdk" io.mosip.registration.controller.Initialization
   ```

   `-Djavax.net.ssl.trustStoreType=Windows-ROOT` is needed behind an antivirus that scans HTTPS (section 1) and does no harm otherwise. The first start takes about two minutes, the time to create the TPM key and the encrypted database; on the reference machine the later starts take one to three minutes before the sign-in screen.

4. On the sign-in screen, choose **English**. Centre 10905 of Collab has no data in French: in French, the header shows the centre as "null (null)" and every synchronisation after the first one is refused with `CENTER_INACTIVE` ("Sync Configuration Failure").

5. Before rebuilding the client, close it and check that no `java` process of the client is left: its jar is locked while it runs, and Maven then stops with `Failed to delete ...\registration-client-1.2.0.2.jar`.

## 8. Simulated face device

`face-liveness-pad/mock-sbi` is a simulated L0/L1 face device that speaks SBI like a real one, with a `static` mode (fixed image, as the MOSIP mock device), a `webcam` mode (frames of the workstation camera) and a `replay` mode (recorded sequences). Its build, its modes and the Windows steps are in [../mock-sbi/README.md](../mock-sbi/README.md). In short, from the root of `face-liveness-pad`:

```powershell
python mock-sbi\scripts\fetch_assets.py
cd mock-sbi
mvn -DskipTests "-Dgpg.skip" clean install
cd target
.\run_reg.bat
```

Start the device in a terminal where JDK 11 is selected (section 1), like the client. With a newer JDK, the stream works but every capture fails: the face ISO converter of MOSIP loads OpenCV in a way that Java 12 and later refuse (`UnsatisfiedLinkError: no opencv_java490`), the device closes the connection, and the log of the client shows `127.0.0.1:4501 failed to respond`.

The device depends on `liveness-engine`, which scores each face frame in the device (section 3 first). The device listens on the first free port from 4501, where the client discovers it. `python mock-sbi\scripts\check_device.py`, from the root of the repository, calls it the way the client does and checks each answer.

An antivirus can take the device for a threat: it opens a local server, loads a private test key and signs its answers. On the reference machine, the behaviour shield of Avast stopped the device during a replay run and moved `mock-mds-1.2.0.2.jar` to quarantine without showing any dialog. The next start then fails with `Could not find or load main class io.mosip.mock.sbi.test.TestMockSBI`. Add an exception for the build folder, `face-liveness-pad\mock-sbi\target\*`, in the antivirus settings, then build the device again.

**On Collab.** The client trusts only device certificates issued for the environment. With the public test keys of the device, the onboarding stops with `REG-MDM-108`; the certificates of the mock devices published for Collab expired on 13 September 2025 (`KER-JWS-107`). Ask MOSIP for device keys: on 22 October 2026 they sent `Device.p12` and `FTM.p12`, valid until August 2027, with a sample configuration. Then, in `mock-sbi\target`:

1. Copy both files into `Biometric Devices\Face\Keys\`.
2. In `application.properties`, set the face keys: `mosip.mock.sbi.file.face.keys.keystorefilename`, `.keyalias` and `.keystorepwd` for `Device.p12`, and the same three keys with the `.ftm` suffix for `FTM.p12`, with the aliases and the password given by MOSIP. In the files of 22 October, both keystores open with the password of the device key, while the FTM lines of the sample point to another file.
3. Set `mosip.mock.sbi.quality.score=94`. The device reports a fixed quality, and Collab requires 90 % for the onboarding face: with the default of 44, the **Save** button stays disabled.
4. For a live face, keep `mosip.mock.sbi.face.source=webcam`, then restart the device.

`mock-sbi\target` is ignored by Git. Never copy these keys or their password into a tracked file. A rebuild of the device copies `application.properties` and `Biometric Devices` into `target` again and replaces these settings: keep a copy of `target\application.properties` outside the repository and put it back after each build.

## 9. Face liveness settings

Face liveness is on by default, with the defaults of the engine, as soon as the face is captured (`mosip.registration.face_enable_flag=Y`). Its settings are configuration keys of the client, synchronised from the server like the other client settings. [configuration.md](configuration.md) lists every key, its default, its allowed values, the per-workflow overrides and a snippet for `registration-default.properties` on the server side. Liveness keys should not be made changeable locally on a workstation (section 1 of that document).

## 10. Demo without a MOSIP environment

[../demo/README.md](../demo/README.md) builds and starts a standalone application that runs the liveness check end to end with the engine, the status pane and the message files of the modified client, and the simulated device in webcam mode. It needs sections 3, 4 and 8, not a MOSIP environment.

## 11. First run on Collab

Verified on 23 October 2026, with the tunnel active, the device of section 8 and the client of section 7.

1. Sign in in English with the operator account. The first sign-in runs the initial synchronisation.
2. From the home page, open the onboarding of the operator. With only a face device, mark every fingerprint and both irises as exceptions, by clicking each image.
3. On the face, press **Scan**. The liveness pane appears over the face image: a short passive analysis, then two random challenges, with 15 s for each. For a turn, turn the head to the side of the arrow, then look back at the camera. The capture starts on its own once liveness is established, and the face is stored only if it matches the face followed on the stream ([workflows.md](workflows.md), section 2.4).
4. On Collab, the device stream lasts `mosip.registration.capture_time_out`, about 10 s: the whole check and the capture must finish within it ([configuration.md](configuration.md), section 5). An attempt that lasts longer fails, either while the stream is cut or at the capture, with `error 700` from the device. The device takes about 2.5 s to open the webcam, then from under 1 s to more than 3 s for the capture, which leaves about 6 s for the passive analysis and the two challenges. After three failed attempts, the scan of the onboarding is blocked (`on_max_attempts` is `BLOCK` for the operator); restarting the client resets the count.
5. Press **Save**. On 23 October the server refused the onboarding with `RPR-RCT-001` ("Unable to access API"), a failure on the server side that was reported to MOSIP. The client shows the message returned by the server (before registration-client#17 it showed only "ERROR").

## Not covered yet

Resident registration, packet upload and face sign-in on Collab: they need a completed onboarding (section 11).

## References

- [Registration Client, MOSIP Docs 1.2.0](https://docs.mosip.io/1.2.0/modules/registration-client)
- [Registration Client developers guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/develop/registration-client-developers-guide)
- [Registration Client Collab guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/test/collab-reg-client-setup-guide)
- [MOSIP Collab environment](https://collab.mosip.net/)
