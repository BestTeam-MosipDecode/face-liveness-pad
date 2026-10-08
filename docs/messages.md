# Messages

Status on 8 October 2026: first version. All texts of the face liveness feature, in English and French, with lines ready to paste into the message and label files of the Registration Client. The keys of the project plan are kept, and the keys needed by the screens of [ui/README.md](ui/README.md) are added. Which message appears in which situation is defined in [decision-flow.md](decision-flow.md), section 8.
## Rules

- Short sentences, plain words, no technical term. The person reads them in a few seconds, sometimes from a distance.
- The same word for the same thing from message to button: "try again", "exception".
- A detected attack is never named on screen: it shows `LIVENESS_GENERIC_FAILURE`, the message of an engine error.
- `{0}` and `{1}` are filled with `MessageFormat`. In a text that contains a placeholder, a single quote must be written twice (`''`). None of the texts below has both.
- Encoding: `messages_fr.properties` and `labels_fr.properties` of the client are encoded in ISO-8859-1, and `messages_ar.properties` uses `\uXXXX` escapes only. The lines to paste below use `\uXXXX` escapes for every non-ASCII character, which reads correctly whatever the encoding of the file.
- Arabic: the keys are added with the English text until a translation is available, so that no screen shows a missing key.
## Messages

Files `messages_en.properties`, `messages_fr.properties`, `messages_ar.properties`.

| Key | English | French | Used for |
| --- | --- | --- | --- |
| `LIVENESS_CHECKING` | Checking face liveness… | Vérification de la vivacité du visage… | Passive check in progress |
| `LIVENESS_PASSIVE_HINT` | Look at the camera. No action is needed. | Regardez la caméra. Aucune action n'est nécessaire. | Under the band, passive check |
| `LIVENESS_AUTH_HINT` | Look at the camera. Sign-in continues automatically once liveness is verified. | Regardez la caméra. La connexion se poursuit automatiquement une fois la vivacité vérifiée. | Under the band, authentication |
| `LIVENESS_HOLD_STILL` | Hold still | Ne bougez plus | Baseline of a challenge, blurred frame, capture in progress |
| `LIVENESS_INSTRUCTION_SOON` | An instruction will appear in a moment. | Une consigne va s'afficher dans un instant. | Under the band, hold-still phase |
| `LIVENESS_CHALLENGE_BLINK` | Please blink | Veuillez cligner des yeux | Challenge |
| `LIVENESS_CHALLENGE_SMILE` | Please smile | Veuillez sourire | Challenge |
| `LIVENESS_CHALLENGE_TURN_LEFT` | Please turn your head to the left | Veuillez tourner la tête vers la gauche | Challenge |
| `LIVENESS_CHALLENGE_TURN_RIGHT` | Please turn your head to the right | Veuillez tourner la tête vers la droite | Challenge |
| `LIVENESS_KEEP_GOING` | Please continue | Veuillez continuer | Action not detected yet |
| `LIVENESS_ACTION_DETECTED` | Action detected | Action détectée | Action detected |
| `LIVENESS_SUCCESS` | Face captured successfully | Visage capturé avec succès | Captured image accepted |
| `LIVENESS_SUCCESS_HINT` | Check the image, then go to the next step. | Vérifiez l'image, puis passez à l'étape suivante. | Under the band, resident capture accepted |
| `LIVENESS_FAILED` | We could not verify face liveness. Please try again. | Nous n'avons pas pu vérifier la vivacité du visage. Veuillez réessayer. | Liveness failure |
| `LIVENESS_GENERIC_FAILURE` | Face verification could not be completed. Please try again. | La vérification du visage n'a pas pu être terminée. Veuillez réessayer. | Attack detected, engine error |
| `LIVENESS_RETRY_AVAILABLE` | Attempt {0} of {1}. You can try again. | Tentative {0} sur {1}. Vous pouvez réessayer. | After a failure, attempts left |
| `LIVENESS_MAX_ATTEMPTS` | Face verification could not be completed. Please follow the exception process. | La vérification du visage n'a pas pu aboutir. Veuillez suivre la procédure d'exception. | Resident, attempts exhausted |
| `LIVENESS_EXCEPTION_INFO` | The face is captured once more without a liveness decision. A supervisor must authenticate before the registration is submitted. | Le visage est capturé une dernière fois sans décision de vivacité. Un superviseur devra s'authentifier avant la soumission de l'enregistrement. | Under the band, exception process |
| `LIVENESS_MAX_ATTEMPTS_AUTH` | Face verification could not be completed. Use another sign-in method or contact your supervisor. | La vérification du visage n'a pas pu aboutir. Utilisez un autre mode de connexion ou contactez votre superviseur. | Authentication, attempts exhausted |
| `LIVENESS_TIP_LOOK_CAMERA` | Look directly at the camera | Regardez directement la caméra | Tip |
| `LIVENESS_TIP_FACE_VISIBLE` | Make sure your face is clearly visible | Assurez-vous que votre visage est bien visible | Tip |
| `LIVENESS_TIP_LIGHTING` | Improve the lighting | Améliorez l'éclairage | Tip |
| `LIVENESS_TIP_STAY_IN_FRAME` | Keep your face inside the frame | Gardez votre visage dans le cadre | Tip |
| `LIVENESS_TIP_FOLLOW_ACTION` | Follow the requested action | Suivez l'action demandée | Tip |
| `LIVENESS_NO_FACE` | No face detected | Aucun visage détecté | Guidance |
| `LIVENESS_MULTIPLE_FACES` | Only one person should be in front of the camera | Une seule personne doit se trouver devant la caméra | Guidance |
| `LIVENESS_DEVICE_UNAVAILABLE` | Face capture device not available | Dispositif de capture du visage indisponible | Device error |
| `LIVENESS_TIP_DEVICE` | Check that the face device is connected and switched on, then try again. | Vérifiez que le dispositif de capture du visage est branché et allumé, puis réessayez. | Under the band, device error |

The French text of `LIVENESS_MAX_ATTEMPTS` says "procédure d'exception" rather than "procédure de recours" from the project plan, to match the button "Continuer sous exception".
## Labels

Files `labels_en.properties`, `labels_fr.properties`, `labels_ar.properties`. Short texts of the screens: field names, states, buttons.

| Key | English | French |
| --- | --- | --- |
| `liveness` | Liveness | Vivacité |
| `livenessChecking` | Checking | Vérification en cours |
| `livenessWaitingAction` | Waiting for an action | En attente d'une action |
| `livenessVerified` | Verified | Vérifiée |
| `livenessNotVerified` | Not verified | Non vérifiée |
| `livenessNotStarted` | Not started | Non commencée |
| `livenessChallengeCount` | Challenge {0} of {1} | Action {0} sur {1} |
| `livenessTimeLeft` | Time left | Temps restant |
| `livenessCaptureStartsAfter` | The capture starts once liveness is verified | La capture démarre une fois la vivacité vérifiée |
| `livenessTryAgain` | Try again | Réessayer |
| `livenessCaptureAgain` | Capture again | Capturer de nouveau |
| `livenessContinueException` | Continue under exception | Continuer sous exception |
| `livenessSignInPassword` | Sign in with password | Se connecter avec le mot de passe |
| `livenessDiagnostic` | Diagnostic mode, enabled by configuration | Mode diagnostic, activé par la configuration |

## Lines to paste

### `messages_en.properties`

```properties
#Face liveness
LIVENESS_CHECKING=Checking face liveness\u2026
LIVENESS_PASSIVE_HINT=Look at the camera. No action is needed.
LIVENESS_AUTH_HINT=Look at the camera. Sign-in continues automatically once liveness is verified.
LIVENESS_HOLD_STILL=Hold still
LIVENESS_INSTRUCTION_SOON=An instruction will appear in a moment.
LIVENESS_CHALLENGE_BLINK=Please blink
LIVENESS_CHALLENGE_SMILE=Please smile
LIVENESS_CHALLENGE_TURN_LEFT=Please turn your head to the left
LIVENESS_CHALLENGE_TURN_RIGHT=Please turn your head to the right
LIVENESS_KEEP_GOING=Please continue
LIVENESS_ACTION_DETECTED=Action detected
LIVENESS_SUCCESS=Face captured successfully
LIVENESS_SUCCESS_HINT=Check the image, then go to the next step.
LIVENESS_FAILED=We could not verify face liveness. Please try again.
LIVENESS_GENERIC_FAILURE=Face verification could not be completed. Please try again.
LIVENESS_RETRY_AVAILABLE=Attempt {0} of {1}. You can try again.
LIVENESS_MAX_ATTEMPTS=Face verification could not be completed. Please follow the exception process.
LIVENESS_EXCEPTION_INFO=The face is captured once more without a liveness decision. A supervisor must authenticate before the registration is submitted.
LIVENESS_MAX_ATTEMPTS_AUTH=Face verification could not be completed. Use another sign-in method or contact your supervisor.
LIVENESS_TIP_LOOK_CAMERA=Look directly at the camera
LIVENESS_TIP_FACE_VISIBLE=Make sure your face is clearly visible
LIVENESS_TIP_LIGHTING=Improve the lighting
LIVENESS_TIP_STAY_IN_FRAME=Keep your face inside the frame
LIVENESS_TIP_FOLLOW_ACTION=Follow the requested action
LIVENESS_NO_FACE=No face detected
LIVENESS_MULTIPLE_FACES=Only one person should be in front of the camera
LIVENESS_DEVICE_UNAVAILABLE=Face capture device not available
LIVENESS_TIP_DEVICE=Check that the face device is connected and switched on, then try again.
```

### `messages_fr.properties`

```properties
#Face liveness
LIVENESS_CHECKING=V\u00e9rification de la vivacit\u00e9 du visage\u2026
LIVENESS_PASSIVE_HINT=Regardez la cam\u00e9ra. Aucune action n'est n\u00e9cessaire.
LIVENESS_AUTH_HINT=Regardez la cam\u00e9ra. La connexion se poursuit automatiquement une fois la vivacit\u00e9 v\u00e9rifi\u00e9e.
LIVENESS_HOLD_STILL=Ne bougez plus
LIVENESS_INSTRUCTION_SOON=Une consigne va s'afficher dans un instant.
LIVENESS_CHALLENGE_BLINK=Veuillez cligner des yeux
LIVENESS_CHALLENGE_SMILE=Veuillez sourire
LIVENESS_CHALLENGE_TURN_LEFT=Veuillez tourner la t\u00eate vers la gauche
LIVENESS_CHALLENGE_TURN_RIGHT=Veuillez tourner la t\u00eate vers la droite
LIVENESS_KEEP_GOING=Veuillez continuer
LIVENESS_ACTION_DETECTED=Action d\u00e9tect\u00e9e
LIVENESS_SUCCESS=Visage captur\u00e9 avec succ\u00e8s
LIVENESS_SUCCESS_HINT=V\u00e9rifiez l'image, puis passez \u00e0 l'\u00e9tape suivante.
LIVENESS_FAILED=Nous n'avons pas pu v\u00e9rifier la vivacit\u00e9 du visage. Veuillez r\u00e9essayer.
LIVENESS_GENERIC_FAILURE=La v\u00e9rification du visage n'a pas pu \u00eatre termin\u00e9e. Veuillez r\u00e9essayer.
LIVENESS_RETRY_AVAILABLE=Tentative {0} sur {1}. Vous pouvez r\u00e9essayer.
LIVENESS_MAX_ATTEMPTS=La v\u00e9rification du visage n'a pas pu aboutir. Veuillez suivre la proc\u00e9dure d'exception.
LIVENESS_EXCEPTION_INFO=Le visage est captur\u00e9 une derni\u00e8re fois sans d\u00e9cision de vivacit\u00e9. Un superviseur devra s'authentifier avant la soumission de l'enregistrement.
LIVENESS_MAX_ATTEMPTS_AUTH=La v\u00e9rification du visage n'a pas pu aboutir. Utilisez un autre mode de connexion ou contactez votre superviseur.
LIVENESS_TIP_LOOK_CAMERA=Regardez directement la cam\u00e9ra
LIVENESS_TIP_FACE_VISIBLE=Assurez-vous que votre visage est bien visible
LIVENESS_TIP_LIGHTING=Am\u00e9liorez l'\u00e9clairage
LIVENESS_TIP_STAY_IN_FRAME=Gardez votre visage dans le cadre
LIVENESS_TIP_FOLLOW_ACTION=Suivez l'action demand\u00e9e
LIVENESS_NO_FACE=Aucun visage d\u00e9tect\u00e9
LIVENESS_MULTIPLE_FACES=Une seule personne doit se trouver devant la cam\u00e9ra
LIVENESS_DEVICE_UNAVAILABLE=Dispositif de capture du visage indisponible
LIVENESS_TIP_DEVICE=V\u00e9rifiez que le dispositif de capture du visage est branch\u00e9 et allum\u00e9, puis r\u00e9essayez.
```

### `messages_ar.properties`

```properties
#Face liveness, English text until translated
LIVENESS_CHECKING=Checking face liveness\u2026
LIVENESS_PASSIVE_HINT=Look at the camera. No action is needed.
LIVENESS_AUTH_HINT=Look at the camera. Sign-in continues automatically once liveness is verified.
LIVENESS_HOLD_STILL=Hold still
LIVENESS_INSTRUCTION_SOON=An instruction will appear in a moment.
LIVENESS_CHALLENGE_BLINK=Please blink
LIVENESS_CHALLENGE_SMILE=Please smile
LIVENESS_CHALLENGE_TURN_LEFT=Please turn your head to the left
LIVENESS_CHALLENGE_TURN_RIGHT=Please turn your head to the right
LIVENESS_KEEP_GOING=Please continue
LIVENESS_ACTION_DETECTED=Action detected
LIVENESS_SUCCESS=Face captured successfully
LIVENESS_SUCCESS_HINT=Check the image, then go to the next step.
LIVENESS_FAILED=We could not verify face liveness. Please try again.
LIVENESS_GENERIC_FAILURE=Face verification could not be completed. Please try again.
LIVENESS_RETRY_AVAILABLE=Attempt {0} of {1}. You can try again.
LIVENESS_MAX_ATTEMPTS=Face verification could not be completed. Please follow the exception process.
LIVENESS_EXCEPTION_INFO=The face is captured once more without a liveness decision. A supervisor must authenticate before the registration is submitted.
LIVENESS_MAX_ATTEMPTS_AUTH=Face verification could not be completed. Use another sign-in method or contact your supervisor.
LIVENESS_TIP_LOOK_CAMERA=Look directly at the camera
LIVENESS_TIP_FACE_VISIBLE=Make sure your face is clearly visible
LIVENESS_TIP_LIGHTING=Improve the lighting
LIVENESS_TIP_STAY_IN_FRAME=Keep your face inside the frame
LIVENESS_TIP_FOLLOW_ACTION=Follow the requested action
LIVENESS_NO_FACE=No face detected
LIVENESS_MULTIPLE_FACES=Only one person should be in front of the camera
LIVENESS_DEVICE_UNAVAILABLE=Face capture device not available
LIVENESS_TIP_DEVICE=Check that the face device is connected and switched on, then try again.
```

### `labels_en.properties`

```properties
#Face liveness
liveness=Liveness
livenessChecking=Checking
livenessWaitingAction=Waiting for an action
livenessVerified=Verified
livenessNotVerified=Not verified
livenessNotStarted=Not started
livenessChallengeCount=Challenge {0} of {1}
livenessTimeLeft=Time left
livenessCaptureStartsAfter=The capture starts once liveness is verified
livenessTryAgain=Try again
livenessCaptureAgain=Capture again
livenessContinueException=Continue under exception
livenessSignInPassword=Sign in with password
livenessDiagnostic=Diagnostic mode, enabled by configuration
```

### `labels_fr.properties`

```properties
#Face liveness
liveness=Vivacit\u00e9
livenessChecking=V\u00e9rification en cours
livenessWaitingAction=En attente d'une action
livenessVerified=V\u00e9rifi\u00e9e
livenessNotVerified=Non v\u00e9rifi\u00e9e
livenessNotStarted=Non commenc\u00e9e
livenessChallengeCount=Action {0} sur {1}
livenessTimeLeft=Temps restant
livenessCaptureStartsAfter=La capture d\u00e9marre une fois la vivacit\u00e9 v\u00e9rifi\u00e9e
livenessTryAgain=R\u00e9essayer
livenessCaptureAgain=Capturer de nouveau
livenessContinueException=Continuer sous exception
livenessSignInPassword=Se connecter avec le mot de passe
livenessDiagnostic=Mode diagnostic, activ\u00e9 par la configuration
```

### `labels_ar.properties`

```properties
#Face liveness, English text until translated
liveness=Liveness
livenessChecking=Checking
livenessWaitingAction=Waiting for an action
livenessVerified=Verified
livenessNotVerified=Not verified
livenessNotStarted=Not started
livenessChallengeCount=Challenge {0} of {1}
livenessTimeLeft=Time left
livenessCaptureStartsAfter=The capture starts once liveness is verified
livenessTryAgain=Try again
livenessCaptureAgain=Capture again
livenessContinueException=Continue under exception
livenessSignInPassword=Sign in with password
livenessDiagnostic=Diagnostic mode, enabled by configuration
```
