# PROGRESS

Suivi de travail interne du projet Face Liveness / PAD (MOSIP Decode 2026, sujet 04).
Plan de référence : `PLAN_FACE_LIVENESS.md`, à la racine du workspace `mosip_decode/` (hors dépôt).
Dernière mise à jour : 7 octobre 2026.

## Étape en cours

Issue n° 21, part documentation (renfort de Tobi3h) : `docs/configuration.md` et `docs/messages.md`, sur la branche `feature/configuration`, poussée avec l'accord d'Elisée, Pull Request ouverte. Maquettes (issue n° 35) fusionnées avec la Pull Request n° 46. La phase 0 est terminée, sauf la fin de l'étape 0.4 qui attend l'accès à l'environnement Collab.

## Terminé

Phase 0

- [x] 0.1 Inventaire de l'environnement (6 octobre)
- [x] 0.2 `docs/PROGRESS.md` créé, ainsi que le fichier de consignes local du workspace, hors dépôt (6 octobre)
- [x] 0.3 Branche de base choisie : `master` (1.2.0.2). Branche `feature/face-liveness` créée en local dans `registration-client/`, non poussée (7 octobre)
- [ ] 0.4 Faire tourner le Registration Client non modifié
  - [x] Build de la 1.2.0.2 avec JDK 11.0.32 et Maven 3.9.9 (7 octobre)
  - [x] `docs/dev-setup-windows.md`, première version (7 octobre). Sections 1 à 3 vérifiées, sections 4 à 7 à vérifier avec Collab.
  - [ ] Démarrage du client, connexion d'un opérateur, capture du visage avec un dispositif simulé (bloqué, voir Blocages)
- [x] 0.5 Structurer `face-liveness-pad/` (7 octobre). Pull Request n° 1 fusionnée dans `main` par Elisée le 7 octobre.
- [x] 0.6 Analyse de Silent-Face-Anti-Spoofing (7 octobre, faite avant 0.5). Spécification de prétraitement dans `training/README.md`, vérifiée par exécution sur les trois images d'exemple.
- [x] 0.7 Issues GitHub et répartition (7 octobre). 12 étiquettes et 37 issues créées sur GitHub, n° 2 à 38, avec responsables et échéances.
- [x] 0.8 Architecture réalignée sur le sujet 04 (7 octobre) : `docs/architecture.md` et `docs/diagrams/components.puml`, première version.

Phase 1

- [x] Issue n° 3, export ONNX des deux MiniFASNet (7 octobre). Pull Request n° 39 fusionnée par Elisée, issue fermée.
- [x] Issue n° 4, détecteur de visage et points de repère (7 octobre). Pull Request n° 40 fusionnée par Elisée, issue fermée.
- [ ] Issues n° 2, 5, 6, 7, 8 (voir GitHub)

Sources de vérité lues le 6 octobre : sujet officiel anglais (12 pages), critères d'évaluation, consignes de soumission, plan. La traduction française du sujet n'est pas dans le workspace, ce qui ne bloque rien (l'anglais fait foi).

## Environnement (étape 0.1)

Relevé sur la machine d'Elisée. Racine du workspace : `C:\wamp64\www\mosip_decode`.

| Élément | Valeur relevée |
| --- | --- |
| Système | Windows 11 Professionnel 10.0.26300, 64 bits |
| Processeur | Intel Core i7-8550U 1,8 GHz, 4 cœurs, 8 threads |
| Mémoire | 15,9 Go |
| Carte graphique | Intel UHD Graphics 620 (intégrée), inférence prévue sur CPU |
| TPM | présent, version 2.0 (ST Microelectronics), initialisé, prêt pour le stockage. Lu avec `tpmtool getdeviceinformation`, sans droits administrateur. |
| JDK utilisé pour le projet | Temurin 11.0.32, activé par `use-jdk11.ps1` à la racine du workspace |
| JDK par défaut de la machine | Temurin 21.0.12+8 |
| Maven | 3.9.9, installé dans `%USERPROFILE%\tools` |
| Git | 2.47.0.windows.2 |
| Python | 3.10.8 (PATH et `venv` de Silent-Face) |
| Dépôt Maven local | `~/.m2/repository`, contient les artefacts MOSIP 1.2.0.1 et 1.2.0.2 |

### État des dépôts

| Dépôt | Branche locale | Remotes | État |
| --- | --- | --- | --- |
| `face-liveness-pad/` | `main`, à jour après la fusion de la Pull Request n° 40 | `origin` = BestTeam-MosipDecode/face-liveness-pad | propre. Identité Git : `Magloire04`, adresse noreply GitHub. Pas de signature GPG configurée. |
| `registration-client/` | `feature/face-liveness` (22fe01f99b), identique à `master`, poussée le 7 octobre avec l'accord d'Elisée | `origin` = fork de l'équipe, `upstream` = mosip/registration-client | propre. |
| `Silent-Face-Anti-Spoofing/` | `master` (b6d5f04) | `origin` = minivision-ai | arbre de travail modifié : `LICENSE`, `.gitignore`, `train.py`, `datasets/README.md` supprimés (suppressions indexées), `requirements.txt` modifié, `webcam_test.py`, `venv/` et `images/sample/pie*.jpg` non suivis. |

## Build du Registration Client (étape 0.4)

Commande, depuis `registration-client/registration/`, JDK 11 actif : `mvn clean install -Dgpg.skip -DskipTests`.

| Date | JDK / Maven | Résultat | Durée |
| --- | --- | --- | --- |
| 21 septembre (Elisée) | 11.0.32 / 3.9.9 | réussi | 5 min 20 s |
| 7 octobre, premier lancement | 11.0.32 / 3.9.9 | échec au `clean` de `registration-services` après 2 min 27 s | |
| 7 octobre, reprise avec `-rf :registration-services` | 11.0.32 / 3.9.9 | réussi | 8 min 02 s |

Cause de l'échec : `Failed to delete ...\registration-services\target`. Trois instances du serveur de langage Java de l'IDE (extension `redhat.java`) tournaient, dont une lancée pendant le build. Elles écrivent dans les mêmes dossiers `target` que Maven. Il ne restait dans `target` que les dossiers que ce serveur crée (`generated-sources/annotations`, `test-classes`). La même commande, relancée à partir du module en échec, est passée sans autre changement. Le verrou était donc passager.

Classe principale du client : `io.mosip.registration.controller.Initialization` (manifest du JAR et sources). Elle lance `ClientApplication` avec son préchargeur. Le plan citait `ClientApplication` avec la mention "à vérifier".

Le nom d'hôte du serveur (`mosip.hostname`, valeur par défaut `dev.mosip.net`) n'est lu que dans `registration-services/src/main/resources/props/mosip-application.properties`. Pas de variable d'environnement en 1.2.0.2 : il faudra modifier ce fichier en local sans le committer.

## Comparaison des branches (étape 0.3)

Faite le 6 octobre sur les références amont (`upstream/*`).

| Critère | `master` | `develop` | `release-1.3.x` |
| --- | --- | --- | --- |
| Version | 1.2.0.2 (publiée, tag `v1.2.0.2`) | 1.2.1-SNAPSHOT | 1.3.0-SNAPSHOT |
| Dernier commit amont | 17 mars 2026 | 23 septembre 2026 | 21 septembre 2026 (code : 2 juin) |
| Java (compilation) | 11 | 21 | 21 |
| OpenJFX | 11.0.2 | 21.0.3 | 21.0.3 |
| Spring | 5.0.6 | 6.1.4 | 6.1.4 |
| Espace de noms | `javax.*` | `jakarta.*` | `jakarta.*` |
| Dépendances MOSIP | 1.2.0.1 / 1.2.0.2, publiées sur Maven Central | kernel 1.4.0-SNAPSHOT | kernel 1.3.1-SNAPSHOT |
| Dépendances MOSIP directes résolubles | 17 sur 17 | 17 sur 17 | 13 sur 17 |
| Modules | 5 | 6 (nouveau `registration-launcher`, arrivé le 23/09) | 5 |

`master` ne diffère du tag `v1.2.0.2` que par le README et les modèles d'issue. Aucun fichier de code.

Les 4 dépendances introuvables de `release-1.3.x` en 1.3.1-SNAPSHOT : `kernel-auditmanager-api`, `kernel-biometrics-api`, `kernel-biosdk-provider`, `biometrics-util`. Vérification limitée aux dépendances MOSIP directes, par requête HTTP sur les dépôts déclarés dans le POM.

### Fichiers biométriques

| Fichier | `master` vers `develop` | `release-1.3.x` vers `develop` |
| --- | --- | --- |
| `Streamer.java` | identique | identique |
| `BiometricsController.java` | identique | identique |
| `GenericBiometricsController.java` | identique | identique |
| `EODAuthenticationController.java` | identique | identique |
| `AuthenticationController.java` | 1 ligne | identique |
| `LoginController.java` | 1 ligne | identique |
| `BaseController.java` | +33 / -21, sans rapport avec la capture du visage | identique |
| `ApplicationContext.java` | identique | identique |
| `RegistrationConstants.java` | +55 / -54, réorganisation de constantes | identique |
| Fournisseurs SBI (0.9.2, 0.9.5, SBI 1.0) | appels HTTP déplacés dans `mosipDeviceSpecificationHelper`, `stream()` et `isDeviceAvailable()` déclarent `IOException` | +5 / -2 chacun : `previousHash` vide au lieu de `null` dans la requête RCAPTURE |

Le code où la vivacité s'accroche est le même sur les trois branches. Un report ultérieur du correctif vers `develop` resterait limité.

### Environnement Collab

- `collab.mosip.net` annonce : Desktop Registration Client 1.2.0.2, Android Registration Client 1.0.0, Admin Portal 1.3.0, Pre-registration 1.3.0.
- Le guide Collab du Registration Client demande Java 11, TPM 2.0, un accès WireGuard, et l'enregistrement de la machine auprès de MOSIP (utilitaire TPM, formulaire, identifiants reçus par e-mail). Il fournit un Mock MDS prêt à l'emploi.
- Sources : https://collab.mosip.net/ et https://docs.mosip.io/1.2.0/id-lifecycle-management/identity-issuance/registration-client/test/collab-reg-client-setup-guide (consultées le 6 octobre).

### Numéros de ligne

Les numéros de ligne de la section 4.1 du plan viennent de `develop`. À revérifier sur `feature/face-liveness` (1.2.0.2) avant toute modification. Les contrôleurs de capture ont le même nombre de lignes sur les deux branches, sauf `BaseController` (1799 lignes sur `master`, 1811 sur `develop`).

## Équipe

Best_Team, Cotonou. Les quatre comptes GitHub existent (vérifié le 7 octobre).

| Membre | Compte GitHub | Lot |
| --- | --- | --- |
| Elisée Atonde (coordination) | `Magloire04` | Documentation |
| Temitayo Gbolahan | `PrinceSpecial` | Tests |
| Silverio Mensah | `SilverioMen` | Dispositif simulé |
| Tobi Obassandjo | `Tobi3h` | Moteur Java et intégration dans le Registration Client |

Répartition donnée par Elisée le 7 octobre. `PrinceSpecial` et `Magloire04` viennent en renfort sur les lots de `Tobi3h` et de `SilverioMen` quand c'est nécessaire.

Phase 1 (modèles en Python), validée par Elisée le 7 octobre : `PrinceSpecial` pour le pipeline de référence, les détecteurs d'actions, le protocole de capture et l'évaluation (issues n° 2, 5, 6, 7), `Magloire04` pour l'export ONNX et le choix du détecteur de visage (issues n° 3 et 4).

Sur GitHub, l'issue n correspond à la ligne n - 1 du brouillon initial, la Pull Request n° 1 ayant pris le premier numéro.

## Ce qui reste valable du premier document de solution

Pour la mise à jour de `Solution_envisagee.md` par Elisée (étape 0.8). La version réalignée est `docs/architecture.md`.

- Reste valable : le classifieur MiniFASNet, la fusion des scores, le clignement par rapport d'aspect de l'œil, le plan de mesures ISO/IEC 30107-3, le traitement entièrement local.
- Change : la cible est le Registration Client de bureau (1.2.0.2, Java 11) et non Inji Wallet ni le mobile. L'intégration passe par les dispositifs SBI, trois parcours (résident, opérateur, superviseur), une configuration par parcours et une interface JavaFX.
- Réduit par le calendrier : pas de jeux de données publics, Android en conception seulement.

## Configuration et messages (issue n° 21, part documentation)

Rédigés le 8 octobre. Relevé dans le client 1.2.0.2 : la configuration du serveur (`registration-default.properties` de `mosip-config`) est synchronisée dans la table locale `REG.GLOBAL_PARAM`, puis chargée dans `ApplicationContext` ; la table `REG.LOCAL_PREFERENCES` permet des surcharges locales, limitées aux clés autorisées par le serveur.

- `configuration.md` : 17 clés avec type, valeur par défaut et valeurs admises ; règles de validation (une valeur invalide retombe sur la valeur par défaut du moteur, jamais sur une valeur plus faible) ; surcharges par parcours, `enabled` compris ; extrait prêt à coller au format de `mosip-config`.
- Recommandation de sécurité : ne pas autoriser les clés de vivacité en surcharge locale, sinon on pourrait désactiver la vivacité sur un poste.
- Les interrupteurs acceptent `Y`/`N` comme les clés existantes du client (`mosip.registration.face_enable_flag=Y`), en plus de `true`/`false`.
- `messages.md` : 28 messages et 14 libellés en anglais et en français, avec les lignes prêtes à coller. Les fichiers français du client sont en ISO-8859-1 : les lignes utilisent des séquences d'échappement Unicode, vérifiées par décodage. L'arabe reçoit les clés avec le texte anglais en attendant une traduction.
- Écart avec le plan : le message français de fin de tentatives dit « procédure d'exception » et non « procédure de recours », pour correspondre au bouton « Continuer sous exception ».
- Activité de l'équipe au 8 octobre : aucune branche, Pull Request ou commentaire des autres membres sur GitHub. Échéances proches : issue n° 2 (PrinceSpecial) le 9 octobre, issue n° 9 (Tobi3h) le 10 octobre.

## Maquettes d'écran (issue n° 35)

`docs/ui/`, réalisé le 7 octobre : 12 écrans de capture du résident et 4 écrans d'authentification, un diagramme de parcours, une page de présentation. Les écrans sont écrits en HTML (`mockups.html`) et rendus en PNG par Chrome sans interface (`render_mockups.py`), avec un profil temporaire. Aucun visage réel : une silhouette dessinée.

- Style repris du Registration Client (`application.css`) : Roboto, bleus `#005BAA` et `#004887`, gris et bordures du client.
- Élément fort : la consigne du challenge dans un bandeau bleu foncé en 44 px, et en 76 px sur un second écran tourné vers le résident (choix d'Elisée du 7 octobre).
- Contrastes mesurés : tous les textes au-dessus de 4,5:1. Le rouge `#FF0000` et le vert `#45A30A` du client n'atteignent que 4,0:1 et 3,2:1 en texte : gardés pour l'ovale et les bordures, nuances plus foncées pour le texte.
- Deux écrans en français (authentification de l'opérateur, second écran du résident).
- Corrigé pendant la revue des rendus : anneau de progression déformé, libellés anglais sur l'écran français, icône de caméra débordante, message « contactez votre superviseur » affiché à un superviseur.

## Alignement ISO/IEC 30107 (issue n° 34, quatrième partie)

`docs/iso-30107-alignment.md`, rédigé le 7 octobre. Numéros et titres de clause vérifiés sur les extraits officiels gratuits des deux normes (sommaire, avant-propos, introduction, premières clauses), téléchargés depuis le site de l'éditeur VDE et gardés hors du dépôt. Aucun texte des normes n'est reproduit.

- Partie 1 : vocabulaire, types de PAD, challenge-réponse (5.2.2) et vivacité sans challenge (5.2.3), place de la PAD dans le système (5.4), obstacles aux attaques (6).
- Partie 3 : deux niveaux d'évaluation, sous-système PAD (7.3) et système complet (7.5) ; métriques de la clause 13 (APCER par espèce et son maximum, BPCER, taux de non-réponse, temps).
- Écart avec le plan : l'ACER, prévu par le plan, est déconseillé par la norme depuis 2017 (il moyenne les espèces d'attaque et masque la plus faible). Il ne sera publié qu'à côté de l'APCER par espèce, comme chiffre de comparaison avec la littérature.
- Limites écrites : pas de format standard du résultat PAD (ISO/IEC 30107-2), attaques de dissimulation et présentations non conformes non testées, petit jeu d'évaluation, pas de testeur indépendant.

## Sécurité et vie privée (issue n° 34, troisième partie)

`docs/security-privacy.md`, rédigé le 7 octobre sur la branche `feature/security-privacy`, partie de `feature/sequence-diagrams` (Pull Request n° 43 non fusionnée au moment du travail). Deux constats dans le code du client :

- au démarrage, `ClientIntegrityValidator` ne vérifie la signature que des JAR `registration-client*` et `registration-services*`, et pas du tout quand `environment=LOCAL`. Un JAR `liveness-engine` séparé ne serait pas vérifié : étendre le contrôle ou intégrer le moteur dans un JAR vérifié, à décider avec l'issue n° 26 ;
- la réponse `RCAPTURE` est signée par le dispositif et vérifiée par le client (`validateJWTResponse`), le flux vidéo ne l'est pas. Les attaques par injection d'un flux falsifié restent hors de portée de la détection d'attaque par présentation, comme prévu par le plan (phase 6, point 6).

Limites écrites telles quelles : données d'entraînement du classifieur non publiées, masques 3D non mesurables par l'équipe, caméras couleur uniquement, petit jeu de calibrage.

## Parcours et diagrammes de séquence (issue n° 34, deuxième partie)

`docs/workflows.md`, rédigé le 7 octobre, avec deux diagrammes de séquence : capture du visage du résident, authentification de l'opérateur et du superviseur. Référence des issues d'intégration n° 20 à 26. Relevé dans le code 1.2.0.2 :

- résident : `GenericBiometricsController` envoie aujourd'hui la demande de capture en même temps que l'ouverture du flux (lignes 480 à 492). Avec la vivacité, la capture attend l'état `PASSED` ;
- authentification : `captureAndValidateFace(...)` (`BaseController`, ligne 1713) capture puis compare localement. Le contrôle de l'image capturée se place entre les deux. La connexion par visage passe par `SessionContext.create(...)`, côté services : il faudra y placer un point d'accroche (issue n° 23) ;
- après `PASSED`, la capture part sans clic, comme le demande le sujet ;
- le compteur de tentatives existant du client (`ATTEMPTS`) n'est pas celui de la vivacité.

Point d'ergonomie pour l'issue n° 24 : l'écran fait face à l'opérateur, pas au résident. Décision d'Elisée du 7 octobre : prévoir les deux, un affichage de la consigne en grand lisible depuis la place du résident (par défaut), et la lecture à voix haute par l'opérateur si le résident ne voit pas l'écran.

## Logique de décision (issue n° 34, première partie)

`docs/decision-flow.md`, rédigé le 7 octobre à la demande d'Elisée, sert de référence aux issues n° 11 à 15 du moteur. Il précise ce que le plan laissait ouvert :

- recadrage par la boîte carrée de YuNet, score d'une image = probabilité moyenne de la classe « réel » des deux MiniFASNet, score agrégé = médiane sur la fenêtre ;
- suivi du visage d'une image à l'autre (recouvrement d'au moins 0,3) : une rupture vide la fenêtre en passif, termine la tentative en actif ;
- limite de rotation de la tête levée pendant l'actif, mais pas de score passif sur ces images ;
- phase « Ne bougez plus » avant chaque challenge pour mesurer la ligne de base, puis règles de détection des quatre actions (valeurs de départ à calibrer, issue n° 5) ;
- contrôle de l'image `RCAPTURE` en cinq points, avec une nouvelle opération dans l'API (issue n° 10) ;
- table des codes de retour vers les clés de message du plan, et des transitions vers les événements d'audit.

Validé par Elisée le 7 octobre : deux nouvelles clés, `passive.timeout_ms` (20 000) et `active.baseline_ms` (600) ; une opération de contrôle de l'image capturée dans l'API ; une tentative est comptée pour tout échec après la première image analysée, erreurs de dispositif comprises, pour qu'un débranchement ne remette pas le compteur à zéro.

Résident après la dernière tentative (décision d'Elisée, 7 octobre) : orientation vers la procédure d'exception de MOSIP. Dans la 1.2.0.2, le visage ne peut pas être déclaré en exception biométrique (seuls les doigts et l'iris le peuvent). Le document s'appuie donc sur le circuit existant des exceptions : nouvelle capture sans décision de vivacité, marque d'exception dans l'audit et dans le paquet, authentification obligatoire d'un superviseur à la soumission, comme pour une exception de doigts ou d'iris. Opérateur et superviseur : blocage, jamais d'exception. Reste à demander aux mentors comment la marque doit voyager dans le paquet jusqu'au serveur.

Les diagrammes sont rendus en SVG avec le PlantUML 1.2026.2 de l'extension de l'IDE, pour être lisibles directement sur GitHub. Le diagramme de composants de `architecture.md` l'est aussi.

## Détecteur de visage et points de repère (issue n° 4)

Choix validé par Elisée le 7 octobre : YuNet pour la détection, MediaPipe Face Mesh V2 pour les points de repère. Le détail (comparaison, contrats des modèles, mesures) est dans `training/README.md`. À retenir :

- Comparaison du 7 octobre : PFLD et PIPNet écartés (poids sans licence établie), détecteur de Silent-Face écarté (origine non documentée), conversions ONNX tierces de Face Mesh gardées en repli.
- Téléchargements autorisés par Elisée le 7 octobre, rangés dans `training/downloads/` (ignoré par Git). YuNet : empreinte identique à celle du pointeur Git LFS d'OpenCV Zoo. Lot MediaPipe : empreinte MD5 identique à celle annoncée par le serveur.
- Environnement séparé `training/.venv-convert` : TensorFlow 2.21.0, tf2onnx 1.17.0, onnx 1.23.2, ONNX Runtime 1.23.2.
- YuNet copié tel quel (232 589 octets). Face Mesh V2 converti en ONNX (4 822 155 octets) : écart maximal avec TFLite de 6,3e-04 pixel sur les points et 2,0e-04 sur l'indicateur de présence. Conversion reproductible à l'octet près.
- Erreur corrigée en cours de route : le modèle TFLite a deux sorties d'une seule valeur, et la première version du script prenait la mauvaise. Vérification sur visages et sur images sans visage : la bonne est `Identity_1`.
- Sur les trois images d'exemple : un visage trouvé par YuNet à chaque fois (score 0,93), présence Face Mesh à 1,000, centres des yeux de Face Mesh à 5 à 11 % de l'écart entre les yeux des points de YuNet.
- Les boîtes de YuNet sont plus hautes que larges. Avec la boîte brute, les scores MiniFASNet bougent (0,23, 0,003, 1,000 contre 0,07, 0,18, 0,99 avec la boîte de référence). Avec une boîte carrée de même surface : 0,03, 0,02, 0,996, mêmes décisions et meilleure séparation. Recommandation pour le moteur, à confirmer sur les captures de l'équipe (issue n° 7).
- La région de Face Mesh n'est pas tournée pour mettre les yeux à l'horizontale. La tête doit rester droite. À vérifier sur les captures.
- Temps par image en Python sur la machine d'Elisée : 22 à 40 ms pour YuNet, 14 à 15 ms pour Face Mesh, 11 à 15 ms pour les deux MiniFASNet.
- Taille totale des quatre modèles : 10,4 Mo, à embarquer dans le JAR du moteur.
- Le code Python de référence de la détection (`training/liveness/yunet.py`) et des points de repère (`training/liveness/face_mesh.py`) couvre une partie de l'issue n° 2 de PrinceSpecial. À lui signaler.

## Export ONNX (phase 1, issue n° 3)

Le détail est dans `training/README.md`. À retenir :

- Environnement `training/.venv` (Python 3.10.8) : PyTorch 2.14.0 CPU, NumPy 2.2.6, OpenCV 4.10.0, onnx 1.23.2, ONNX Runtime 1.23.2. Installation autorisée par Elisée le 7 octobre. Les paquets d'évaluation (scikit-learn, pandas, matplotlib) ne sont pas encore installés.
- Modèles : `minifasnet_v2_2.7_80x80.onnx` (1 743 495 octets) et `minifasnet_v1se_4.0_80x80.onnx` (1 742 663 octets), opset 13, entrée `input`, sortie `logits` avant softmax.
- Parité PyTorch / ONNX Runtime sur 20 entrées : écart maximal de 1,3e-05 et 6,9e-06 sur les logits, pour un seuil de 1e-4.
- Les modèles ONNX redonnent les scores de référence sur les trois images d'exemple : 0,73, 0,82 et 0,99.
- Export reproductible : deux lancements donnent des fichiers identiques.
- Vecteurs dorés : 4 entrées réseau et 12 cas de prétraitement, tous synthétiques. Les images sources des cas de prétraitement ne sont pas stockées : une formule entière les reconstruit, en Python comme en Java.
- `training/scripts/verify_golden.py` revérifie le tout sans le clone de Silent-Face. Le recadrage réécrit d'après la spécification (`training/liveness/crop.py`) redonne les 12 patchs de référence à l'octet près.
- Le texte de la licence Apache 2.0 de Silent-Face est copié à côté des modèles, qui sont redistribués dans le JAR.
- Point ouvert pour l'issue n° 11 : le décodage JPEG de Java et celui d'OpenCV peuvent différer d'un ou deux niveaux par pixel. Les vecteurs dorés partent d'images sans compression pour isoler le recadrage et le redimensionnement. L'effet du décodage JPEG sur le score reste à mesurer.

## Silent-Face-Anti-Spoofing (étape 0.6)

Le détail est dans `training/README.md`. À retenir :

- Licence : Apache License 2.0, "Copyright 2020 Minivision", pas de fichier `NOTICE`. Lue par `git show HEAD:LICENSE`, le fichier étant supprimé de l'arbre de travail. Réutilisation permise avec attribution, à inscrire dans `THIRD_PARTY_NOTICES.md` (étape 0.5).
- Entrée des modèles : 1 x 3 x 80 x 80, `float32`, ordre BGR, valeurs de 0 à 255. Pas de division par 255, pas de moyenne ni d'écart-type.
- Sortie : 3 classes, softmax. La classe 1 est le visage réel. Les deux modèles sont additionnés.
- Scores obtenus : `image_F1.jpg` faux 0,73, `image_F2.jpg` faux 0,82, `image_T1.jpg` réel 0,99. Environnement : PyTorch 2.14.0 CPU, OpenCV 4.10.0.
- L'échelle de recadrage est plafonnée par la taille de l'image. Sur les images d'exemple, les deux modèles reçoivent le même recadrage.
- Le code de référence n'applique aucun seuil de confiance au détecteur : il renvoie une boîte même sans visage.
- Le redimensionnement d'OpenCV est à reproduire à l'identique en Java. C'est le principal risque d'écart entre Python et Java.
- Exécution faite avec un script placé hors du dossier, sans écriture dans `Silent-Face-Anti-Spoofing/` (`test.py` y aurait écrit une image annotée).
- `images/sample/pie.jpg` et `pie_result.jpg` sont des copies de `image_T1.jpg` et de son résultat (même SHA-256). Ce ne sont pas des photos d'un membre de l'équipe.

## AMA du 7 octobre et calendrier

Relevé dans la transcription de la séance fournie par Elisée.

- Version : la mentore du sujet 04 (Varaniya) confirme la 1.2.0.2, dernière version publiée. `develop` change trop souvent pour servir de base.
- Serveur : le client 1.2.0.2 est annoncé compatible avec le module d'enregistrement côté serveur jusqu'à la 1.3.1.
- Périmètre : le sujet 04 porte sur le client. La mentore précise qu'il n'y a pas à se soucier de l'environnement serveur.
- Soumission : par e-mail à `decode@mosip.io` (nom d'équipe, sujet traité, URL). Rien ne passe par Unstop. Un dépôt GitHub suffit, aucun déploiement n'est demandé.
- Questions entre deux séances : `decode@mosip.io` ou le forum MOSIP Community.

Dates relevées sur la page Unstop du hackathon le 7 octobre (affichées en heure de New York) :

| Étape | Dates |
| --- | --- |
| AMA hebdomadaires | jusqu'au 21 octobre. Séances restantes : 14 et 21 octobre |
| Soumission | du 23 septembre au 26 octobre 2026, 14 h 29 EDT, soit 19 h 29 à Cotonou |
| Évaluation et résultats | du 1er au 6 novembre |

La date limite est donc le 26 octobre et non en novembre. Le calendrier de la section 9 du plan, qui court jusqu'au 16 novembre, ne tient plus.

### Calendrier resserré (validé par Elisée le 7 octobre)

| Dates | Contenu |
| --- | --- |
| 7 au 9 octobre | Fin de la phase 0 (0.5, 0.6, 0.8). Export ONNX des deux MiniFASNet et vecteurs de parité. Choix du détecteur de visage et du modèle de points de repère |
| 10 au 15 octobre | Moteur Java 11 : prétraitement, inférence, score passif, challenges (clignement, sourire, rotation de la tête), machine à états, tests |
| 13 au 17 octobre, en parallèle | Dispositif simulé en mode webcam |
| 16 au 21 octobre | Intégration dans le Registration Client : flux, configuration, parcours résident, opérateur, superviseur, messages, audit |
| 22 et 23 octobre | Matrice fonctionnelle, mesures PAD sur le jeu interne, temps de traitement |
| 24 et 25 octobre | README, documentation, diagrammes, vidéo de démonstration, pitch |
| 26 octobre | E-mail de soumission, le matin |

Ce qui sortirait du périmètre : jeux de données publics (1.3) et évaluation à grande échelle (1.5), remplacés par le jeu interne de l'équipe. Challenges de regard. Phase 6, sauf la vérification de version du modèle. Android limité à un document de conception.

## Décisions

- 6 octobre. Les références `upstream/*` ont été récupérées par `git fetch upstream` dans `registration-client/`. Aucun fichier de travail modifié.
- 6 octobre. Ce fichier est créé sur `main` sans commit. Il sera committé avec l'arborescence à l'étape 0.5, sur `feature/repo-structure`.
- 7 octobre. Base du Registration Client : version 1.2.0.2, branche `master`. Réponse des mentors à l'AMA du 7 octobre, rapportée par Elisée. C'est aussi la version annoncée par Collab.
- 7 octobre. Conséquence du choix précédent : le client tourne sur Java 11, donc le moteur `liveness-engine` doit être compilé pour Java 11. Les sections 5.3 et 7.1 du plan supposaient Java 21 (les `record` de l'API deviennent des classes ordinaires). À faire valider par Elisée à l'étape 2.1.
- 7 octobre. JDK 11.0.32 et Maven 3.9.9 conservés pour le projet. Les versions 21.0.3 et 3.9.6 ne sont pas installées : l'exigence vient du README de `develop` et ne concerne pas la 1.2.0.2. Confirmé par Elisée le 7 octobre : on ne les installe pas.
- 7 octobre. Les capacités de la machine (disque, mémoire) ne sont plus suivies comme point de risque, à la demande d'Elisée.
- 7 octobre. Calendrier resserré et périmètre réduit validés par Elisée. Le moteur en Java 11 fait partie de ce calendrier.
- 7 octobre. Coordonnées Maven provisoires du moteur : `io.mosip.registration:liveness-engine:0.1.0-SNAPSHOT`. Le groupe et le nom de paquet restent à confirmer (Q6).
- 7 octobre. Les graphiques de résultats versionnés portent un nom en `chart_*.png`. Toute autre image sous un dossier `results/` est ignorée par Git.
- 7 octobre. `docs/architecture.md` est committé sur la même branche que l'arborescence, pour n'ouvrir qu'une Pull Request.
- 7 octobre. Push de `feature/repo-structure` et ouverture de la Pull Request n° 1 autorisés par Elisée. Pas de fusion sans nouvel accord.
- 7 octobre. Création des étiquettes et des issues autorisée par Elisée. Le brouillon local a été supprimé : les issues GitHub font foi.
- 7 octobre. Les vecteurs dorés n'utilisent aucune image de visage, alors que la règle 4.2 aurait permis les images d'exemple de Silent-Face. Des entrées synthétiques suffisent pour tester la parité numérique.
- 7 octobre. Les définitions de réseau de Silent-Face ne sont pas copiées dans le dépôt. Le script d'export les importe depuis le clone local.
- 7 octobre. Pull Request n° 39 fusionnée par Elisée. Push de `feature/face-liveness` dans le fork `registration-client` autorisé et fait.
- 7 octobre. Détecteur YuNet et points de repère MediaPipe Face Mesh V2 validés par Elisée.
- 7 octobre. `models.json` est partagé par les scripts d'export : chacun ne remplace que ses propres entrées. Version du lot de modèles : 1.1.0. `golden.json` enregistre désormais l'empreinte des modèles utilisés au lieu d'un numéro de version.
- 7 octobre. Pas de vecteurs dorés pour YuNet et Face Mesh dans cette étape : Java exécutera les mêmes fichiers ONNX avec ONNX Runtime. Le risque d'écart porte sur le prétraitement et le décodage, à tester dans les issues n° 11 et n° 12 contre `training/liveness/`.

## Blocages

- Accès Collab. Elisée a envoyé le formulaire de demande. Pas de réponse de MOSIP au 7 octobre. Sans enregistrement de la machine, sans identifiants et sans accès WireGuard, le client ne peut pas se synchroniser avec le serveur. La fin de l'étape 0.4 attend cet accès. Les étapes 0.5, 0.6 et 0.8 n'en dépendent pas.

## Écarts relevés par rapport au plan

1. Section 4.1 : le tableau des points d'accroche cite `BiometricsController` pour la capture du résident. `GenericBiometricsController` (lié à `GenericBiometricFXML.fxml`) contient le même enchaînement : `rCaptureTaskService()` puis `streamer.startStream(...)`. Les deux contrôleurs sont à traiter en phase 4. À confirmer à l'exécution : lequel sert le parcours résident.
2. Sections 3, 5.3 et 7.1 : le plan suppose `develop` et Java 21. La base retenue est la 1.2.0.2 en Java 11 (voir Décisions).
3. Étape 0.4 : la classe principale est `Initialization`, pas `ClientApplication`.
4. Sections 1.1 et 9 : le plan situe la date limite en novembre. Elle est fixée au 26 octobre (voir la section sur l'AMA).
5. Section 3 : `Silent-Face-Anti-Spoofing/LICENSE` est supprimé de l'arbre de travail. Le texte reste lisible par `git show HEAD:LICENSE` : Apache License 2.0. Licence relevée à l'étape 0.6, dossier laissé tel quel.
6. Sujet officiel : les livrables citent "Android Registration Client changes" et "Consistent Desktop and Android experience". Le plan limite Android à une conception documentée. À garder en tête pour Q8.
7. Sujet officiel : le message de succès donné en exemple est "Good, face captured successfully". Le plan (7.5) écrit "Face captured successfully".
8. `Silent-Face-Anti-Spoofing/images/sample/pie.jpg` et `pie_result.jpg` ne viennent pas du dépôt d'origine. Point levé le 7 octobre : ce sont des copies de `image_T1.jpg` et de son résultat.

## Questions pour Elisée

- Q1 (close, 7 octobre) : base = 1.2.0.2.
- Q2 (close, 7 octobre) : date limite = 26 octobre 2026, 19 h 29 à Cotonou.
- Q9 (close, 7 octobre) : calendrier resserré et périmètre réduit validés.
- Q3 (ouverte, étape 0.4) : accès Collab demandé, sans réponse. À la réception : nom d'hôte de l'environnement, identifiants, configuration WireGuard, tous hors dépôt.
- Q0 (close, 7 octobre) : JDK 21.0.3 et Maven 3.9.6 non installés, ils ne servent pas à la 1.2.0.2.
- Q4 (close, 7 octobre) : membres, comptes GitHub et répartition reçus (voir Équipe).
- Q10 (close, 7 octobre) : issues et étiquettes créées, phase 1 répartie.
- Q11 (close, 7 octobre) : Pull Request n° 1 fusionnée par Elisée.
- Q12 (close, 7 octobre) : Pull Request n° 39 fusionnée.
- Q13 (close, 7 octobre) : Pull Request n° 40 ouverte avec l'accord d'Elisée, puis fusionnée par Elisée.
- Q14 (close, 7 octobre) : clés `passive.timeout_ms` et `active.baseline_ms`, opération de contrôle de l'image capturée et règle de comptage des tentatives validées.
- Q7 (close, 7 octobre) : résident orienté vers la procédure d'exception de MOSIP après la dernière tentative.
- Q15 (ouverte, AMA du 14 octobre) : comment une exception de vivacité du visage doit-elle être portée dans le paquet pour le traitement côté serveur ?
- Q6 étendue (étape 2.1) : moteur compilé pour Java 11, nom de paquet `io.mosip.registration.liveness`.
