# Developer setup on Windows

How to build and run the MOSIP Registration Client used by this project on a Windows workstation.

Status on 7 October 2026: sections 1 to 3 are verified on the team's reference machine. Sections 4 to 7 come from the MOSIP documentation and from the client source code. They will be verified and completed once the team has access to the MOSIP Collab environment.

## 1. Prerequisites

| Item | Required | Used by the team |
| --- | --- | --- |
| Operating system | Windows 10 or 11, 64-bit | Windows 11 Pro |
| TPM | version 2.0, enabled | version 2.0 |
| JDK | 11 | Eclipse Temurin 11.0.32 |
| Maven | 3.x | 3.9.9 |
| Git | any recent version | 2.47 |

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

## 2. Get the sources

```powershell
git clone https://github.com/BestTeam-MosipDecode/registration-client.git
cd registration-client
git switch feature/face-liveness
```

`feature/face-liveness` starts from `master`, which holds the 1.2.0.2 release. The only differences between `master` and the `v1.2.0.2` tag are the README and the issue templates.

## 3. Build

From `registration-client/registration`:

```powershell
mvn clean install -Dgpg.skip -DskipTests
```

The reactor builds six projects: the parent POM, `registration-api`, `registration-api-stub-impl`, `registration-services`, `registration-client` and `registration-test`. All MOSIP dependencies of this release are published on Maven Central. The build takes five to ten minutes on the reference machine once the local Maven repository is populated.

If the build stops with `Failed to delete ...\target`, the Java language server of an open IDE was writing into that folder while Maven was cleaning it. Run the command again from the module that failed, for example:

```powershell
mvn clean install -Dgpg.skip -DskipTests -rf :registration-services
```

## 4. Select the MOSIP environment

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

## 5. Register the machine on Collab

The Registration Client only talks to a MOSIP server that knows the TPM public key of the machine. For Collab, the procedure is described in the [Registration Client Collab Guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/test/collab-reg-client-setup-guide):

1. Download the TPM utility linked in the guide and run `java -jar tpmutility-0.0.2.jar > tpmdetails.txt`.
2. Send the content of `tpmdetails.txt` through the request form linked in the guide.
3. MOSIP registers the machine, then sends the operator credentials and the WireGuard access by e-mail.
4. Connect WireGuard before starting the client.

Credentials and the WireGuard configuration never go into a repository.

## 6. Run the client from the IDE

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

## 7. Simulated biometric device

The Collab guide provides a ready-to-use Mock MDS: download the archive linked in the guide, extract it, then start `run_reg.bat`. The client discovers the device on the local ports reserved for SBI.

This project will ship its own simulated device, with a webcam mode and a replay mode. It will be documented in `mock-sbi.md`.

## Not covered yet

First login, operator onboarding and a complete face capture. These steps need a registered machine and will be written after the first successful run.

## References

- [Registration Client, MOSIP Docs 1.2.0](https://docs.mosip.io/1.2.0/modules/registration-client)
- [Registration Client developers guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/develop/registration-client-developers-guide)
- [Registration Client Collab guide](https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/test/collab-reg-client-setup-guide)
- [MOSIP Collab environment](https://collab.mosip.net/)
