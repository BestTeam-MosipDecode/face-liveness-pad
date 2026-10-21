# Developer setup on Windows

How to build the liveness engine and the modified MOSIP Registration Client on a Windows workstation, and how to run them.

Status on 16 October 2026: sections 1 to 4 were verified from a clean checkout of both repositories on the team's reference machine (Windows 11, JDK 11.0.32, Maven 3.9.9), and section 8 on the same machine. Sections 5 to 7 come from the MOSIP documentation and from the client source code: they need a MOSIP environment and a registered machine, and will be verified once the team has access to the MOSIP Collab environment. The notes on antivirus software in sections 1 and 8 come from the runs of 19 October on the same machine.

## 1. Prerequisites

| Item | Required | Used by the team |
| --- | --- | --- |
| Operating system | Windows 10 or 11, 64-bit | Windows 11 Pro |
| TPM | version 2.0, enabled | version 2.0 |
| JDK | 11 | Eclipse Temurin 11.0.32 |
| Maven | 3.x | 3.9.9 |
| Git | any recent version | 2.47 |
| Python | 3.10, only for the tools of `training/` and `mock-sbi/scripts/` | 3.10 |

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

The build runs the tests of the engine, 189 of them, and takes about one minute. The first build downloads ONNX Runtime for Java, about 90 MB. The engine embeds its four models and checks them at startup against `models.json`.

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

| Property | Default value |
| --- | --- |
| `mosip.hostname` | `dev.mosip.net` |
| `mosip.client.upgrade.server.url` | `https://dev.mosip.net` |

The client reads this file from the classpath at startup. Release 1.2.0.2 offers no environment variable or command-line override, so the file has to be edited and `registration-services` rebuilt.

Keep this edit out of the Git history:

```powershell
git update-index --skip-worktree registration/registration-services/src/main/resources/props/mosip-application.properties
```

The host name to use for Collab is not given in the public Collab guide. It will be added here when access is granted.

## 6. Register the machine on Collab

The Registration Client only talks to a MOSIP server that knows the TPM public key of the machine. For Collab, the procedure is described in the [Registration Client Collab Guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/test/collab-reg-client-setup-guide):

1. Download the TPM utility linked in the guide and run `java -jar tpmutility-0.0.2.jar > tpmdetails.txt`.
2. Send the content of `tpmdetails.txt` through the request form linked in the guide.
3. MOSIP registers the machine, then sends the operator credentials and the WireGuard access by e-mail.
4. Connect WireGuard before starting the client.

Credentials and the WireGuard configuration never go into a repository.

## 7. Run the client from the IDE

Main class: `io.mosip.registration.controller.Initialization`, in the `registration-client` module. It sets the logging configuration, then starts `ClientApplication` with its preloader.

According to the [MOSIP developers guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/develop/registration-client-developers-guide), a run configuration needs:

- the OpenJFX SDK 11.0.2 for Windows, unzipped on the local disk;
- these VM arguments, with the path adapted:

  ```text
  --module-path C:\path\to\javafx-sdk-11.0.2\lib
  --add-modules=javafx.controls,javafx.fxml,javafx.base,javafx.web,javafx.swing,javafx.graphics
  --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
  ```

- the `mock-sdk` JAR on the classpath of `registration-services`, as the biometric SDK implementation.

`Initialization` loads `lib/logback.xml` relative to the working directory. The file exists in `registration-client/registration/lib`, so the working directory of the run configuration should be `registration-client/registration`. To be confirmed at the first run.

## 8. Simulated face device

`face-liveness-pad/mock-sbi` is a simulated L0/L1 face device that speaks SBI like a real one, with a `static` mode (fixed image, as the MOSIP mock device), a `webcam` mode (frames of the workstation camera) and a `replay` mode (recorded sequences). Its build, its modes and the Windows steps are in [../mock-sbi/README.md](../mock-sbi/README.md). In short, from the root of `face-liveness-pad`:

```powershell
python mock-sbi\scripts\fetch_assets.py
cd mock-sbi
mvn -DskipTests "-Dgpg.skip" clean install
cd target
.\run_reg.bat
```

The device listens on the first free port from 4501, where the client discovers it. `python mock-sbi\scripts\check_device.py`, from the root of the repository, calls it the way the client does and checks each answer.

An antivirus can take the device for a threat: it opens a local server, loads a private test key and signs its answers. On the reference machine, the behaviour shield of Avast stopped the device during a replay run and moved `mock-mds-1.2.0.2.jar` to quarantine without showing any dialog. The next start then fails with `Could not find or load main class io.mosip.mock.sbi.test.TestMockSBI`. Add an exception for the build folder, `face-liveness-pad\mock-sbi\target\*`, in the antivirus settings, then build the device again.

## 9. Face liveness settings

Face liveness is on by default, with the defaults of the engine, as soon as the face is captured (`mosip.registration.face_enable_flag=Y`). Its settings are configuration keys of the client, synchronised from the server like the other client settings. [configuration.md](configuration.md) lists every key, its default, its allowed values, the per-workflow overrides and a snippet for `registration-default.properties` on the server side. Liveness keys should not be made changeable locally on a workstation (section 1 of that document).

## 10. Demo without a MOSIP environment

[../demo/README.md](../demo/README.md) builds and starts a standalone application that runs the liveness check end to end with the engine, the status pane and the message files of the modified client, and the simulated device in webcam mode. It needs sections 3, 4 and 8, not a MOSIP environment.

## Not covered yet

First login, operator onboarding and a complete face capture in the client. These steps need a registered machine and will be written after the first successful run.

## References

- [Registration Client, MOSIP Docs 1.2.0](https://docs.mosip.io/1.2.0/modules/registration-client)
- [Registration Client developers guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/develop/registration-client-developers-guide)
- [Registration Client Collab guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/test/collab-reg-client-setup-guide)
- [MOSIP Collab environment](https://collab.mosip.net/)
