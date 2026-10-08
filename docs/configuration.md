# Configuration

Status on 8 October 2026: first version. This document lists the configuration keys of the face liveness feature, their defaults and allowed values, how the Registration Client receives them, and a snippet ready for the MOSIP configuration. The rules that use each key are in [decision-flow.md](decision-flow.md). The texts shown to users are in [messages.md](messages.md).

## 1. How the client receives its configuration

In Registration Client 1.2.0.2:

1. The server configuration, `registration-default.properties` in the `mosip-config` repository, is synchronised to the client and stored in its local database (table `REG.GLOBAL_PARAM`).
2. At startup, `DaoConfig` loads the active values into `ApplicationContext`. The client reads them with `getStringValueFromApplicationMap(...)`, `getIntValueFromApplicationMap(...)` and `getFloatValueFromApplicationMap(...)`.
3. A machine can override some keys locally (table `REG.LOCAL_PREFERENCES`), only for keys that the server lists as permitted for local change.

The liveness configuration follows the same path. A small provider in the client (`LivenessConfigProvider`, #21) reads the keys of this document, applies the per-workflow overrides and builds the policy given to the engine.

- **Offline.** The client uses the values of its last synchronisation. Liveness behaves the same online and offline.
- **Missing key.** On a first installation, or with a server that does not know the keys yet, the engine applies the defaults it ships with (`liveness-defaults.properties` inside the engine JAR, #7). These defaults are the values of the table below.
- **Invalid value.** A value that cannot be read or breaks a rule of section 3 is ignored. The engine default applies and the client writes a warning to its log, with the key and the value. A wrong setting can therefore never weaken liveness below the defaults.
- **Local override.** Liveness keys should not be added to the list of keys permitted for local change. Otherwise, anyone with access to the settings of a machine could disable liveness or lower its thresholds on that machine ([security-privacy.md](security-privacy.md), section 2.3).

## 2. Keys

Every key starts with `mosip.registration.face.liveness.`, written `…` below.

| Key | Type | Default | Allowed values | Role |
| --- | --- | --- | --- | --- |
| `…enabled` | boolean | `true` | `true`, `false`, `Y`, `N` | Turns liveness on. With `false`, the client keeps its original behaviour |
| `…passive.threshold.pass` | decimal | `0.85` | 0 to 1, above the attack threshold | Aggregate score from which liveness is verified without a challenge |
| `…passive.threshold.attack` | decimal | `0.30` | 0 to 1, below the pass threshold | Aggregate score at or below which the attempt fails as an attack |
| `…passive.min_frames` | integer | `5` | 1 to 30 | Scored frames needed for a passive decision |
| `…passive.window_ms` | integer, ms | `3000` | 500 to 10000 | Time window of the scored frames |
| `…passive.timeout_ms` | integer, ms | `20000` | 5000 to 120000 | Time without a passive decision before the attempt fails |
| `…frame.sampling_ms` | integer, ms | `100` | 33 to 500 | Shortest time between two analysed frames |
| `…quality.min_face_px` | integer, px | `120` | 40 to 1000 | Shortest side of the face box in the source image |
| `…quality.max_yaw_deg` | integer, degrees | `20` | 5 to 45 | Largest head turn accepted for the passive check |
| `…active.enabled` | boolean | `true` | `true`, `false`, `Y`, `N` | Allows the active check when the passive score is ambiguous |
| `…active.min_challenges` | integer | `2` | 1 to 4 | Challenges drawn per attempt. The same type never comes twice in a row, unless only one type is allowed |
| `…active.challenge_types` | list | `BLINK,SMILE,TURN_LEFT,TURN_RIGHT` | non-empty subset of the default, comma-separated | Challenges that may be drawn |
| `…active.challenge_timeout_ms` | integer, ms | `8000` | 3000 to 30000 | Time to perform one challenge, hold-still phase included |
| `…active.baseline_ms` | integer, ms | `600` | 300 to 3000 | Duration of the hold-still phase before an instruction |
| `…max_attempts` | integer | `3` | 1 to 10 | Attempts per capture or authentication |
| `…on_max_attempts` | text | `BLOCK` | `BLOCK`; `EXCEPTION` for residents only | What follows the last failed attempt |
| `…diagnostic.enabled` | boolean | `false` | `true`, `false`, `Y`, `N` | Shows scores and states on screen and writes them to the diagnostic log |

Notes:

- Booleans accept `Y` and `N` as well as `true` and `false`, like the existing `mosip.registration.face_enable_flag`.
- Thresholds are decimals between 0 and 1, the scale of the engine score. The existing quality thresholds of the client (`mosip.registration.face_threshold=90`) use 0 to 100 and are unrelated.
- The two thresholds are provisional until the calibration on the team captures (#7).
- The challenge types `LOOK_LEFT` and `LOOK_RIGHT` of the project plan are not available in this version. A list that contains them is invalid as a whole.

## 3. Validation rules

The provider checks the values of each workflow after the overrides are applied. When a rule fails, every key involved in it falls back to its default.

1. `passive.threshold.attack` < `passive.threshold.pass`.
2. `passive.window_ms` ≥ `passive.min_frames` × `frame.sampling_ms`, so that the window can hold the frames it needs.
3. `active.baseline_ms` < `active.challenge_timeout_ms`.
4. `on_max_attempts=EXCEPTION` is accepted for `RESIDENT` only. For `OPERATOR` and `SUPERVISOR` it is replaced by `BLOCK`.

## 4. Per-workflow overrides

Any key of section 2 can be set for one workflow by inserting the workflow name after the prefix:

```properties
mosip.registration.face.liveness.supervisor.active.min_challenges=3
```

Workflow names: `resident`, `operator`, `supervisor`.

For each key, the value is taken from the first place where it exists:

1. the key of the workflow, for example `…supervisor.active.min_challenges`;
2. the general key, `…active.min_challenges`;
3. the engine default.

Turning liveness off for one workflow, for example `…operator.enabled=N`, removes the control for that workflow only. It is a deliberate policy choice of the country, and the client writes the effective policy of each workflow to its log at startup.

| Workflow | Used for |
| --- | --- |
| `resident` | Resident face capture during registration |
| `operator` | Operator sign-in, operator authentication of a packet |
| `supervisor` | Supervisor authentication of a packet with exceptions, end of day approval |

## 5. Existing keys of the client

These keys keep their current meaning. Liveness does not change them.

| Key | Default in `mosip-config` | Meaning |
| --- | --- | --- |
| `mosip.registration.face_enable_flag` | `Y` | Face capture enabled in the client. Liveness applies only when the face is captured |
| `mosip.registration.num_of_face_retries` | `3` | Retries of the face capture itself, counted by the client. Separate from the liveness attempts |
| `mosip.registration.face_threshold` | `90` | Quality threshold of the captured face image |
| `mosip.face_authentication.quality_score` | `30` | Quality threshold of a face captured for authentication |
| `mosip.registration.face_recapture_time` | `5` | Existing recapture setting of the client |

## 6. Snippet for `registration-default.properties`

Ready to paste into the `mosip-config` file of the environment. The values are the defaults of section 2, with the stricter supervisor policy and the exception process for residents.

```properties
#------------------------------------------------------------------------------
# Face liveness and presentation attack detection
#------------------------------------------------------------------------------
#Enable face liveness. Possible values Y, N, true, false
mosip.registration.face.liveness.enabled=Y
#Aggregate score from which liveness is verified without a challenge. Possible values 0 to 1, above the attack threshold
mosip.registration.face.liveness.passive.threshold.pass=0.85
#Aggregate score at or below which the attempt fails as an attack. Possible values 0 to 1, below the pass threshold
mosip.registration.face.liveness.passive.threshold.attack=0.30
#Scored frames needed for a passive decision. Possible values 1 to 30
mosip.registration.face.liveness.passive.min_frames=5
#Time window of the scored frames, in milliseconds. Possible values 500 to 10000
mosip.registration.face.liveness.passive.window_ms=3000
#Time without a passive decision before the attempt fails, in milliseconds. Possible values 5000 to 120000
mosip.registration.face.liveness.passive.timeout_ms=20000
#Shortest time between two analysed frames, in milliseconds. Possible values 33 to 500
mosip.registration.face.liveness.frame.sampling_ms=100
#Shortest side of the face box, in pixels. Possible values 40 to 1000
mosip.registration.face.liveness.quality.min_face_px=120
#Largest head turn for the passive check, in degrees. Possible values 5 to 45
mosip.registration.face.liveness.quality.max_yaw_deg=20
#Allow the active check when the passive score is ambiguous. Possible values Y, N, true, false
mosip.registration.face.liveness.active.enabled=Y
#Challenges drawn per attempt. Possible values 1 to 4
mosip.registration.face.liveness.active.min_challenges=2
#Challenges that may be drawn. Possible values: comma-separated subset of BLINK,SMILE,TURN_LEFT,TURN_RIGHT
mosip.registration.face.liveness.active.challenge_types=BLINK,SMILE,TURN_LEFT,TURN_RIGHT
#Time to perform one challenge, in milliseconds. Possible values 3000 to 30000
mosip.registration.face.liveness.active.challenge_timeout_ms=8000
#Duration of the hold-still phase before an instruction, in milliseconds. Possible values 300 to 3000
mosip.registration.face.liveness.active.baseline_ms=600
#Attempts per capture or authentication. Possible values 1 to 10
mosip.registration.face.liveness.max_attempts=3
#After the last failed attempt. Possible values BLOCK, EXCEPTION (resident only)
mosip.registration.face.liveness.on_max_attempts=BLOCK
mosip.registration.face.liveness.resident.on_max_attempts=EXCEPTION
#Supervisors approve packets and exceptions: one more challenge
mosip.registration.face.liveness.supervisor.active.min_challenges=3
#Show scores on screen for troubleshooting. Possible values Y, N, true, false
mosip.registration.face.liveness.diagnostic.enabled=N
```

## 7. Settings for development and demonstration

| Situation | Settings |
| --- | --- |
| Troubleshooting a capture | `diagnostic.enabled=Y` on a test machine, never in production |
| Showing the active check during a demonstration | `active.enabled=Y` and a temporarily raised `passive.threshold.pass`, for example `0.99`, so that a live face lands in the ambiguous zone and a challenge appears |
| Comparing with the original client | `enabled=N` |

Recommended production values and their reasons are in [security-privacy.md](security-privacy.md), section 5.
