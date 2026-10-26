# Submission

Draft of the submission email and plan of the shared folder (#38). MOSIP extended the deadline to 2 November 2026 (email of 26 October). The email is sent by the coordinator on Saturday 31 October 2026, ahead of that deadline. Nothing here is sent automatically.

## Email

- To: `decode@mosip.io`
- Subject: MOSIP Decode 2026 submission: Best_Team, problem statement 04

```text
Hello MOSIP Decode team,

Please find below the submission of Best_Team.

Team name: Best_Team
Problem statement: 04, Face Liveness Detection and Presentation Attack Detection

Source code (public GitHub repositories, with their history):
- Engine, simulated device, demo application and documentation:
  https://github.com/BestTeam-MosipDecode/face-liveness-pad
- Changes to the Registration Client 1.2.0.2, branch feature/face-liveness:
  https://github.com/BestTeam-MosipDecode/registration-client/tree/feature/face-liveness
Submitted commits: face-liveness-pad main [commit], registration-client feature/face-liveness [commit].

Shared folder (video, pitch deck, design, tests, documents, student IDs):
[Drive link]

Installation and use: README.md of face-liveness-pad and docs/dev-setup-windows.md.
Demo without a MOSIP environment: demo/README.md.
API documentation: docs/engine-api.md (Java API of the liveness engine), docs/mock-sbi.md and mock-sbi/README.md (SBI calls of the face device, formats, signatures and the passive score header), docs/decision-flow.md section 7.1 (liveness entry of the packet). PDF copies are in 06_Documentation of the shared folder.
The modified client was also run on the MOSIP Collab environment: sign-in, synchronisation and operator onboarding with the liveness check (scene 9 of the video).

Team members: Temitayo Gbolahan, Tobi Obassandjo.

Best regards,
Best_Team
```

## Shared folder

One folder, shared as "anyone with the link can view".

```text
MOSIP-Decode-2026_Best_Team_PS04/
├── 01_Video/              demo video, 2 to 5 minutes (docs/demo-script.md)
├── 02_Pitch/              pitch deck, .pptx and .pdf (docs/pitch-outline.md)
├── 03_Source/             README with the links of the two repositories and the commits submitted
├── 04_Design/             architecture, sequence and state diagrams (docs/diagrams/),
│                          screen mock-ups and status pane renders (docs/ui/)
├── 05_Tests/              test plan, test results, test data
├── 06_Documentation/      PDF exports of docs/: setup guide, engine API, configuration,
│                          decision flow, workflows, security and privacy, ISO/IEC 30107 alignment
└── 07_Student_IDs/        one file per member, in this folder as MOSIP confirmed on 21 October
```

## Mandatory items of the guidelines

| Item | Where |
| --- | --- |
| Source code | The two repositories |
| Working prototype or demo | The modified client in the fork; the demo application in `demo/`; the video |
| Pitch presentation or slide deck | `02_Pitch/` |
| Video demo | `01_Video/` |
| Usage and installation instructions | `README.md`, `docs/dev-setup-windows.md`, `demo/README.md` |
| API documentation | `docs/engine-api.md` (engine), `docs/mock-sbi.md` and `mock-sbi/README.md` (SBI calls used) |
| Git repository with version control history | The two repositories |
| College or university credentials | `07_Student_IDs/`, in the shared folder as MOSIP confirmed on 21 October |

Optional items: design mock-ups (`docs/ui/`), architecture diagram (`docs/diagrams/`), test cases and results (`docs/test-plan.md`, `docs/test-results.md`).

## Final pitch

MOSIP answered on 21 October, on the community forum, that the date and the format of the final pitch are sent to the shortlisted teams after the submission deadline, during the evaluation (1 to 6 November). The pitch is the deck, then a live demonstration of the working solution, preferred, or a recorded demo. The live demonstration uses the modified client on Collab if the environment accepts our machine by then, otherwise the demo application (`demo/`), with the video as fallback.

## Test data

To write in `05_Tests/test-data.md`:

- The demo application and the simulated device need no account and no credential.
- The test keys and sample records of the simulated device are public MOSIP test material, downloaded by `mock-sbi/scripts/fetch_assets.py`.
- The face captures of the team are not shared: they show real faces, and the consent form allows only aggregated results to be published. A reviewer can record their own sequences with `training/scripts/capture_session.py`.
- The configuration keys and their defaults are in `docs/configuration.md`.
- No MOSIP account or Collab credential is shared.

## Checks before sending

- Every link opens in a private browser window, without signing in to a team account.
- Both repositories are public, and the latest commit of each is the one named in `03_Source/`.
- The video plays from the shared folder, and its length is between 2 and 5 minutes.
- The deck opens in .pptx and .pdf.
- The email names the team, the problem statement and the links.
