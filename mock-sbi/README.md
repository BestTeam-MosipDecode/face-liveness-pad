# mock-sbi

Simulated L0/L1 face device for development and automated tests. It speaks the MOSIP Secure Biometric Interface (SBI), so the Registration Client uses it like a real device.

Status: not started.

## Planned modes

| Mode | Source of the frames | Purpose |
| --- | --- | --- |
| `webcam` | Webcam of the workstation | Live demonstration and manual tests |
| `replay` | Recorded video or image folder | Repeatable tests, including presentation attacks |
| `static` | Single fixed image | Check that a frozen image is rejected |

Recorded sequences that contain faces are kept outside the repository.
