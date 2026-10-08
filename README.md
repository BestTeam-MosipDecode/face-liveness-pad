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

Work in progress. Target: Registration Client 1.2.0.2 (desktop, Java 11).

| Component | Folder | Status |
| --- | --- | --- |
| Reference pipeline in Python | `training/` | Classifiers exported to ONNX, face detector and landmark model chosen and packaged, reference code for detection, crop and landmarks |
| Liveness engine in Java | `liveness-engine/` | Maven skeleton, four ONNX models and golden vectors packaged |
| Simulated L0/L1 device | `mock-sbi/` | Not started |
| Registration Client integration | fork of `mosip/registration-client` | Not started |
| Documentation | `docs/` | Setup guide, architecture, decision flow, workflows, security and privacy, ISO/IEC 30107 alignment, screen mock-ups |

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
├── datasets/                 local only, ignored by Git
└── captures/                 local only, ignored by Git
```

No face image, video or public dataset is stored in this repository.

## Getting started

The build and run instructions for the Registration Client on Windows are in [docs/dev-setup-windows.md](docs/dev-setup-windows.md).

## Team

Best_Team, Cotonou, Benin.

| Member | GitHub |
| --- | --- |
| Elisée Atonde | [@Magloire04](https://github.com/Magloire04) |
| Temitayo Gbolahan | [@PrinceSpecial](https://github.com/PrinceSpecial) |
| Silverio Mensah | [@SilverioMen](https://github.com/SilverioMen) |
| Tobi Obassandjo | [@Tobi3h](https://github.com/Tobi3h) |

## License

This project is licensed under the [Mozilla Public License 2.0](LICENSE). Third-party elements and their licences are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
