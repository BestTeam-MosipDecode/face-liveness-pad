# Documentation

| Document | Content | Status |
| --- | --- | --- |
| [dev-setup-windows.md](dev-setup-windows.md) | Build the engine and the modified Registration Client on Windows, run them | Build verified from a clean checkout. Running the client needs a MOSIP environment |
| [architecture.md](architecture.md) | Components as built, frame flow, threads, integration points, online and offline behaviour | Updated on 16 October 2026 |
| [PROGRESS.md](PROGRESS.md) | Internal work log of the team, in French | Updated at each step |
| [decision-flow.md](decision-flow.md) | Session states, frame processing, passive and active rules, captured image check, attempts, feedback codes, audit events, parameters | Implemented in the engine |
| [workflows.md](workflows.md) | Sequence diagrams of resident capture and of operator and supervisor authentication, changes at each step | Implemented in the three workflows |
| [ui/README.md](ui/README.md) | Screen mock-ups of the three workflows, user flow, design rules, contrast | Implemented by `LivenessStatusPane` |
| [configuration.md](configuration.md) | Configuration keys, defaults, allowed values, validation, per-workflow overrides, snippet for `registration-default.properties` | Implemented by `LivenessConfigProvider` |
| [messages.md](messages.md) | Texts of the feature in English, French and Arabic, lines for the message and label files | In the client. Arabic to be read by an Arabic speaker |
| [android.md](android.md) | Android design note: how the client gets face images, options, reuse of the engine, work to do | Design only |
| [engine-api.md](engine-api.md) | Java API of the liveness engine: life cycle, threads, policy, updates, capture check, audit events, errors | As built |
| [mock-sbi.md](mock-sbi.md) | Simulated face device: study of the MOSIP mock device, what the client expects, fork decision | Study. Installation and modes in [../mock-sbi/README.md](../mock-sbi/README.md) |
| [iso-30107-alignment.md](iso-30107-alignment.md) | Mapping to the clauses of ISO/IEC 30107-1 and 30107-3, test design, metrics, gaps | First version. Metrics come with the team captures (#7) |
| [security-privacy.md](security-privacy.md) | Protected assets, attacks and controls, limits, personal data, fairness, recommended settings | Updated on 16 October 2026 |
| [test-plan.md](test-plan.md) | Test protocol: subjects, presentation types and artefacts, lighting, captures, splits, metrics, functional test matrix, data handling | In use for the team captures |
| [test-results.md](test-results.md) | PAD metrics, functional test results, performance | Skeleton. Filled after the team captures (#7) |
| [consent/consent-form.md](consent/consent-form.md) | Consent form signed by each subject before the team captures | In use |
| [demo-script.md](demo-script.md) | Minute-by-minute script of the demonstration video and plan of the live demonstration | Ready for the recording (#36) |

Folders: `diagrams/` holds the PlantUML sources and their SVG renders, `ui/` the screen mock-ups, `consent/` the consent form used for the team's own face captures. Signed forms are not stored in the repository.

## Technical Design deliverable

Where each item of the "Technical Design" deliverable of problem statement 04 is answered.

| Item of the problem statement | Where |
| --- | --- |
| High-level architecture for integrating L0/L1 biometric devices with the Registration Client | [architecture.md](architecture.md), sections 1 and 2 |
| Face frame streaming architecture | [architecture.md](architecture.md), section 3; stream format in [mock-sbi.md](mock-sbi.md) |
| Liveness and PAD processing architecture | [architecture.md](architecture.md), section 2.2; [decision-flow.md](decision-flow.md), sections 3 to 5 |
| Passive-to-active liveness decision flow | [decision-flow.md](decision-flow.md), sections 1, 4 and 5, with the state diagram |
| Dynamic challenge mechanism | [decision-flow.md](decision-flow.md), sections 5.1 to 5.3 |
| Resident registration integration flow | [workflows.md](workflows.md), section 1 |
| Operator authentication integration flow | [workflows.md](workflows.md), section 2 |
| Supervisor authentication integration flow | [workflows.md](workflows.md), section 2 |
| Online and offline architecture | [architecture.md](architecture.md), section 7; [workflows.md](workflows.md), section 3 |
| Configuration design | [configuration.md](configuration.md) |
| Device integration interface | [architecture.md](architecture.md), section 2.1; [mock-sbi.md](mock-sbi.md) |
| Liveness engine interface | [engine-api.md](engine-api.md) |
| Error handling and recovery flow | [decision-flow.md](decision-flow.md), sections 7 and 8; [engine-api.md](engine-api.md), section 9 |
| Sequence diagrams | [workflows.md](workflows.md) and [diagrams/](diagrams/) |
| Security and privacy considerations | [security-privacy.md](security-privacy.md) |
| ISO/IEC 30107 alignment considerations | [iso-30107-alignment.md](iso-30107-alignment.md) |

The "UI/UX Design" deliverable is answered by [ui/README.md](ui/README.md), its mock-ups and [messages.md](messages.md).
