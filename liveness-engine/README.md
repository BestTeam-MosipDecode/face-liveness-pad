# liveness-engine

Java module that decides whether the face in a frame stream belongs to a live person. The Registration Client calls it during resident face capture, operator authentication and supervisor authentication.

Status: Maven skeleton. No source code yet.

## Design constraints

- **Java 11 bytecode.** Registration Client 1.2.0.2 runs on Java 11, so the module is compiled with `maven.compiler.release` set to 11.
- **No dependency on Registration Client code.** The module receives JPEG frames and a policy, and returns a state and a result. This keeps it reusable by another client.
- **Local processing only.** Models are packaged in the JAR under `src/main/resources/models/`. No network call is made.

## Build

```powershell
mvn clean verify
```

To make the module available to the Registration Client build:

```powershell
mvn install
```
