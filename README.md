# face-liveness-pad

Face liveness and presentation attack detection (PAD) for the MOSIP Registration Client.
MOSIP Decode 2026, problem statement 04.

## Goal

The Registration Client receives the face frame stream of the connected L0/L1 biometric device. Before a face capture or a face authentication is accepted, the client has to establish that the face belongs to a live person:

1. A passive check runs first, with no action from the user. It produces a liveness score compared with a configurable threshold.
2. If the score is not high enough, the client starts an active check on its own. It asks for facial actions chosen at random, such as a blink, a smile or a head turn, and verifies them on the following frames.
3. A capture or an authentication is accepted only when liveness and PAD are both satisfied.

This applies to three workflows: resident face capture, operator authentication and supervisor authentication. Everything runs locally, so the behaviour is the same online and offline.

## Status

Target: Registration Client 1.2.0.2 (desktop, Java 11). Submission for MOSIP Decode 2026 on 26 October 2026.

| Component | Where | Status |
| --- | --- | --- |
| Liveness engine in Java | `liveness-engine/` | Passive and active checks, session state machine, captured image check, model check at startup, public API. 193 tests |
| Registration Client integration | [fork of `mosip/registration-client`](https://github.com/BestTeam-MosipDecode/registration-client), branch `feature/face-liveness` | Configuration keys, stream to engine, audit events, resident face capture gated on liveness with the exception process after the last attempt, operator and supervisor face authentication gated on liveness, the face enrolled at onboarding gated on liveness, status pane on the capture, authentication and onboarding screens, signature check of the engine jar, passive score sent by the face device, liveness check of each face carried in the packet. Checked on the MOSIP Collab environment: sign-in, synchronisation and the operator onboarding, whose face passes liveness; saving the onboarding waits for MOSIP to link a UIN to the operator account |
| Simulated L0/L1 device | `mock-sbi/` | `static`, `webcam` and `replay` modes, a passive liveness score sent with each face frame, failure simulation, scenario runner |
| Demo application | `demo/` | Standalone JavaFX demo of the whole check: the engine, the status pane and the messages of the modified client, the simulated device over SBI. Runs without a MOSIP environment |
| Reference pipeline in Python | `training/` | Models exported to ONNX and checked, reference detection, crop and landmarks, evaluation script, capture tools. Team captures of two subjects measured (#7) |
| Documentation | `docs/` | Design documents as built, setup guide verified on Collab, screen mock-ups, messages in three languages, test plan and results |

## Repository structure

```text
face-liveness-pad/
├── README.md
├── LICENSE                   MPL-2.0
├── THIRD_PARTY_NOTICES.md    third-party code, weights and models
├── docs/                     design, setup, tests and results
├── training/                 Python: evaluation, calibration, ONNX export
├── liveness-engine/          Java 11 Maven module used by the Registration Client
├── mock-sbi/                 simulated L0/L1 device for development and tests
├── demo/                     standalone demo application of the liveness check
├── datasets/                 local only, ignored by Git
└── captures/                 local only, ignored by Git
```

No face image, video or public dataset is stored in this repository.

## Getting started

On Windows, with JDK 11 and Maven 3. The steps, the errors to expect and how to run the client are in [docs/dev-setup-windows.md](docs/dev-setup-windows.md).

1. Clone both repositories. The client needs long paths on Windows:

   ```powershell
   git clone https://github.com/BestTeam-MosipDecode/face-liveness-pad.git
   git clone -c core.longpaths=true --branch feature/face-liveness https://github.com/BestTeam-MosipDecode/registration-client.git
   ```

2. Build and install the engine first, from `face-liveness-pad/liveness-engine`:

   ```powershell
   mvn clean install
   ```

3. Build the client, from `registration-client/registration`:

   ```powershell
   mvn clean install "-Dgpg.skip" -DskipTests
   ```

4. Start the simulated face device: [mock-sbi/README.md](mock-sbi/README.md).
5. Run the client against a MOSIP environment: sections 5 to 8 and 11 of [docs/dev-setup-windows.md](docs/dev-setup-windows.md), as verified on Collab.
6. Without a MOSIP environment, run the demo application, which uses the same engine, status pane and device calls: [demo/README.md](demo/README.md).

The Python tools (model export, evaluation, captures) are described in [training/README.md](training/README.md). The design documents are listed in [docs/README.md](docs/README.md).

## Contributing

- One branch per issue, named `feature/<topic>`, created from an up-to-date `main`. Changes reach `main` only through a pull request.
- Commit messages in English, in the form `type(scope): description`, one commit per logical change.
- Each commit is written by the member who makes it. A `Co-authored-by` trailer may only name a team member, with the GitHub noreply address of the member.
- Every pull request runs the `Commit messages` check, which applies these rules to its commits, title and description. To apply them before each commit, enable the hook once in your clone:

  ```powershell
  git config core.hooksPath .githooks
  ```

- The fork [registration-client](https://github.com/BestTeam-MosipDecode/registration-client) runs the same check on pull requests into `feature/face-liveness`, which only accepts changes through a pull request. Its hook is enabled the same way.
- No face image, video or credential in the repository.

## Team

Best_Team, Cotonou, Benin.

| Member | GitHub |
| --- | --- |
| Temitayo Gbolahan | [@PrinceSpecial](https://github.com/PrinceSpecial) |
| Tobi Obassandjo | [@Tobi3h](https://github.com/Tobi3h) |

## License

This project is licensed under the [Mozilla Public License 2.0](LICENSE). Third-party elements and their licences are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
