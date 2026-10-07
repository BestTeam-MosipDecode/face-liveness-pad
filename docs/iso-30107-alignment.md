# Alignment with ISO/IEC 30107

Status on 7 October 2026: first version. This document maps the project to the two parts of ISO/IEC 30107 cited by the problem statement:

- ISO/IEC 30107-1:2023, *Biometric presentation attack detection, Part 1: Framework* (second edition, 2023-08);
- ISO/IEC 30107-3:2023, *Biometric presentation attack detection, Part 3: Testing and reporting* (second edition, 2023-01).

The standards are copyrighted. This document cites clause numbers and clause titles only, and describes how the project relates to them in its own words. Clause numbers were checked against the tables of contents published in the official preview extracts of both standards. The project is not a certified evaluation, and nothing here claims conformance.

## 1. Scope of the project with respect to the standards

Both parts deal with attacks made at the capture device during the presentation of the biometric characteristic (scope, clause 1 of each part). The project addresses exactly that: a person presents a face, or an artefact, to the face device of the Registration Client.

Out of the scope of both the standards and the PAD part of the project: attacks on the device service, injection of a forged stream into the client, morphing at enrolment. They are discussed in [security-privacy.md](security-privacy.md), section 3.

## 2. ISO/IEC 30107-1, Framework

| Clause | Title | How the project relates |
| --- | --- | --- |
| 3 | Terms and definitions | The documents of the project use the vocabulary of the series and of ISO/IEC 2382-37: presentation attack, presentation attack detection (PAD), bona fide presentation, attack presentation, presentation attack instrument (PAI), artefact (3.1), liveness (3.2) |
| 4.2 | Presentation attack instruments | The instruments tested are listed in section 4.1 below, grouped by species |
| 5.1 | Types of presentation attack detection | PAD is done by software on the colour images of the capture device. No additional sensor is used |
| 5.2.2 | Challenge-response related to liveness detection | The active check asks for voluntary reactions drawn at random: blink, smile, head turn ([decision-flow.md](decision-flow.md), section 5) |
| 5.2.3 | Liveness detection not related to challenge-response | The passive check scores the appearance of the face on several frames without asking anything of the person (decision flow, section 4) |
| 5.3 | Presentation attack detection process | The liveness session processes each frame (detection, quality, score), aggregates the scores and decides, with the active check as a second stage (decision flow, sections 3 to 5) |
| 5.4.2 | PAD processing considerations relative to the other biometric subsystems | PAD runs before storage and before comparison. A face is stored in a registration, or compared for authentication, only after the liveness decision and the check of the captured image ([workflows.md](workflows.md)) |
| 5.4.3 | PAD location implications regarding data interchange | PAD runs in the Registration Client, not in the device. The stream it analyses is not signed, while the captured image is. The captured image check links the two (decision flow, section 6). The PAD result is not exchanged in a standard format: see section 5 of this document |
| 6 | Obstacles to biometric impostor presentation attacks in a biometric system | Beyond PAD, the registration context adds obstacles: an operator is present during the capture, attempts are limited, every outcome is audited, and the exception process requires a second user with the supervisor role |

## 3. ISO/IEC 30107-3, Testing and reporting

### 3.1 Levels of evaluation (clause 7)

| Clause | Title | How the project relates |
| --- | --- | --- |
| 7.3 | PAD subsystem evaluation | Classifier-level measurement: the passive score alone, on recorded presentations, with the Python reference pipeline (#7) |
| 7.4 | Data capture subsystem evaluation | Not done as such. The project does not change the capture device |
| 7.5 | Full system evaluation | System-level measurement: the Registration Client with the liveness engine, passive and active checks and captured image check together, through the simulated device and live presentations (#28) |

Results are reported separately for the two levels. A good classifier result does not imply a good system result, and the reverse.

### 3.2 Artefacts and presentations (clauses 8 to 10)

| Clause | Title | How the project relates |
| --- | --- | --- |
| 8.1 | Properties of PAIs in biometric impostor attacks | The artefacts tested reproduce the face of another person, as an impostor would for operator or supervisor authentication |
| 8.2 | Properties of PAIs in biometric concealer attacks | A resident who wants to escape de-duplication at registration would be a concealer. Concealer attacks (make-up, partial masks) are not tested by the project. This is a stated limit |
| 9 | Considerations in non-conformant capture attempts of biometric characteristics | Not covered by the test plan. The quality checks reject a strong head turn or a partly hidden face (decision flow, section 3.5), but non-conformant presentations of a real face are not tested as an attack species. This is a stated limit |
| 10.2 | Artefact creation and preparation | The test protocol records how each artefact is made: printer and paper, screen device and brightness, video source (`test-plan.md`, #6) |
| 10.3 | Artefact usage | The protocol fixes how artefacts are presented: distance, movement, lighting conditions |
| 10.4 | Iterative testing to identify effective artefacts | Not done systematically, for lack of time. Artefacts that pass are reported as such |

### 3.3 Process-dependent factors (clause 11)

| Clause | Title | How the project relates |
| --- | --- | --- |
| 11.2 | Evaluating the enrolment process | Resident face capture is an enrolment: the face goes into a new registration |
| 11.3 | Evaluating the verification process | Operator and supervisor authentication are verifications against the templates of a claimed user |
| 11.5 | Evaluating offline PAD mechanisms | In this project, "offline" means without network access. The PAD decision is always made during the capture, on the workstation. The two notions should not be confused when reading the results |

Clause 12 (evaluation using the Common Criteria framework) is out of the scope of the project. No Common Criteria evaluation is made.

### 3.4 Metrics (clause 13)

| Clause | Title | Metrics reported by the project |
| --- | --- | --- |
| 13.2.2 | Classification metrics (PAD subsystem) | APCER for each PAI species, the highest APCER over the species, and BPCER. A detection error trade-off (DET) curve for the passive score |
| 13.2.3 | Non-response metrics (PAD subsystem) | Share of attack presentations and of bona fide presentations for which no decision is reached (attempts ending on `NO_FACE`, `MULTIPLE_FACES` or `LOW_QUALITY`) |
| 13.2.4 | Efficiency metrics (PAD subsystem) | Mean processing time per frame and time to a passive decision |
| 13.4.2 | Accuracy metrics (full system) | Same classification rates measured on the whole workflow, including the active check and the captured image check |
| 13.4.3 | Efficiency metrics (full system) | Time to complete a capture or an authentication, share of sessions needing a challenge, retry rate |
| 13.4.4 | Generalized full-system evaluation performance | For operator authentication, the share of impostor attacks accepted by the whole chain (liveness and face comparison) can be measured with artefacts of enrolled team members. Planned if time allows (#28) |

ACER, the mean of APCER and BPCER used by academic benchmarks, is described in published PAD benchmarks as deprecated by ISO/IEC 30107-3 since its 2017 edition. It averages over attack species and hides the weakest one. The project reports it only next to the per-species APCER, labelled as a benchmark figure, never instead of it.

The introduction of Part 3 stresses that PAD error rates depend on the set of attack species, the application, the test approach and the people running the test, and are not directly comparable between tests. The results of the project are reported with this context: the species list, the number of artefacts, subjects and presentations, the lighting conditions, the hardware, and the testers.

### 3.5 Annexes

| Annex | Title | How the project relates |
| --- | --- | --- |
| A (informative) | Classification of attack types | Used to name and group the attacks of section 4.1 |
| C (informative) | Roles in PAD testing | Test roles of the project: the test lead (PrinceSpecial), the attack presenters and the bona fide subjects (team members under pseudonyms), the data analyst |

## 4. Test design derived from the standards

### 4.1 PAI species

| Species | Instrument | Expected main control |
| --- | --- | --- |
| Printed photo, matte | Face photo printed on standard paper | Passive classifier |
| Printed photo, glossy | Face photo printed on photo paper | Passive classifier |
| Photo on a phone screen | Still image on a smartphone | Passive classifier |
| Photo on a laptop screen | Still image on a laptop | Passive classifier |
| Replayed video | Video of the person, including blinks and head movements, on a phone or tablet | Random challenges |
| Paper mask | Printed face with cut-out eyes or mouth, worn | Classifier and challenges |

Each species is presented under three lighting conditions (normal, low light, backlight), by several attack presenters, against several subjects of different skin tones. The exact counts are fixed in `test-plan.md` (#6).

### 4.2 Reporting

For each level of evaluation (PAD subsystem, full system), `test-results.md` gives:

- APCER per species and its maximum, BPCER, non-response rates, efficiency figures;
- the number of subjects, artefacts and presentations per species, the conditions and the hardware;
- the thresholds in use and the model set version;
- the BPCER per capture condition, with its sample size, to show differences between groups (see [security-privacy.md](security-privacy.md), section 4.3).

## 5. Gaps

1. **No PAD result in a standard data format.** ISO/IEC 30107-2 defines how to convey PAD approach and results, integrated in the ISO/IEC 39794 series. The project records the result in the audit trail of the client, not in the biometric record. This could be added if MOSIP packets adopt those formats.
2. **Concealer attacks and non-conformant presentations not tested** (clauses 8.2 and 9).
3. **Small evaluation set.** The number of subjects and artefacts of a four-person team does not reach the statistical significance discussed in the introduction of Part 3. The results show behaviour on a defined set, not a general error rate.
4. **No independent tester.** The team that built the feature also tests it. The test roles of Annex C are filled by the same people.

## 6. Sources

- ISO/IEC 30107-1:2023, table of contents, foreword, introduction and clauses 1 to 3 from the official preview extract.
- ISO/IEC 30107-3:2023, table of contents, foreword, introduction and clauses 1 and 2 from the official preview extract.
- Published face PAD benchmarks for the status of ACER.
