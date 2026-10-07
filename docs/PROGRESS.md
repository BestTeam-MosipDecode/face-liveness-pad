# PROGRESS

Suivi de travail interne du projet Face Liveness / PAD (MOSIP Decode 2026, sujet 04).
Plan de référence : `PLAN_FACE_LIVENESS.md`, à la racine du workspace `mosip_decode/` (hors dépôt).
Dernière mise à jour : 7 octobre 2026.

## Étape en cours

0.7 Issues GitHub et répartition. Les étapes 0.5 et 0.8 sont committées en local sur `feature/repo-structure`, en attente d'accord pour le push et la Pull Request. L'étape 0.4 reste ouverte : la fin attend l'accès à l'environnement Collab.

## Terminé

Phase 0

- [x] 0.1 Inventaire de l'environnement (6 octobre)
- [x] 0.2 `docs/PROGRESS.md` créé, ainsi que le fichier de consignes local du workspace, hors dépôt (6 octobre)
- [x] 0.3 Branche de base choisie : `master` (1.2.0.2). Branche `feature/face-liveness` créée en local dans `registration-client/`, non poussée (7 octobre)
- [ ] 0.4 Faire tourner le Registration Client non modifié
  - [x] Build de la 1.2.0.2 avec JDK 11.0.32 et Maven 3.9.9 (7 octobre)
  - [x] `docs/dev-setup-windows.md`, première version (7 octobre). Sections 1 à 3 vérifiées, sections 4 à 7 à vérifier avec Collab.
  - [ ] Démarrage du client, connexion d'un opérateur, capture du visage avec un dispositif simulé (bloqué, voir Blocages)
- [x] 0.5 Structurer `face-liveness-pad/` (7 octobre). Branche `feature/repo-structure`, commits locaux. Reste à faire : push et Pull Request, après accord.
- [x] 0.6 Analyse de Silent-Face-Anti-Spoofing (7 octobre, faite avant 0.5). Spécification de prétraitement dans `training/README.md`, vérifiée par exécution sur les trois images d'exemple.
- [ ] 0.7 Issues GitHub et répartition
- [x] 0.8 Architecture réalignée sur le sujet 04 (7 octobre) : `docs/architecture.md` et `docs/diagrams/components.puml`, première version.

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
| `face-liveness-pad/` | `feature/repo-structure`, créée depuis `main` (485d978), non poussée | `origin` = BestTeam-MosipDecode/face-liveness-pad | propre après les commits de l'étape 0.5. Identité Git : `Magloire04`, adresse noreply GitHub. Pas de signature GPG configurée. |
| `registration-client/` | `feature/face-liveness` (22fe01f99b), créée depuis `master`, non poussée | `origin` = fork de l'équipe, `upstream` = mosip/registration-client | propre. |
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

| Membre | Compte GitHub |
| --- | --- |
| Elisée Atonde (coordination) | `Magloire04` |
| Temitayo Gbolahan | `PrinceSpecial` |
| Silverio Mensah | `SilverioMen` |
| Tobi Obassandjo | `Tobi3h` |

## Ce qui reste valable du premier document de solution

Pour la mise à jour de `Solution_envisagee.md` par Elisée (étape 0.8). La version réalignée est `docs/architecture.md`.

- Reste valable : le classifieur MiniFASNet, la fusion des scores, le clignement par rapport d'aspect de l'œil, le plan de mesures ISO/IEC 30107-3, le traitement entièrement local.
- Change : la cible est le Registration Client de bureau (1.2.0.2, Java 11) et non Inji Wallet ni le mobile. L'intégration passe par les dispositifs SBI, trois parcours (résident, opérateur, superviseur), une configuration par parcours et une interface JavaFX.
- Réduit par le calendrier : pas de jeux de données publics, Android en conception seulement.

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
- 7 octobre. JDK 11.0.32 et Maven 3.9.9 conservés pour le projet. Les versions 21.0.3 et 3.9.6 ne sont pas installées : l'exigence vient du README de `develop` et ne concerne pas la 1.2.0.2. En attente de confirmation d'Elisée (voir Questions).
- 7 octobre. Les capacités de la machine (disque, mémoire) ne sont plus suivies comme point de risque, à la demande d'Elisée.
- 7 octobre. Calendrier resserré et périmètre réduit validés par Elisée. Le moteur en Java 11 fait partie de ce calendrier.
- 7 octobre. Coordonnées Maven provisoires du moteur : `io.mosip.registration:liveness-engine:0.1.0-SNAPSHOT`. Le groupe et le nom de paquet restent à confirmer (Q6).
- 7 octobre. Les graphiques de résultats versionnés portent un nom en `chart_*.png`. Toute autre image sous un dossier `results/` est ignorée par Git.
- 7 octobre. `docs/architecture.md` est committé sur la même branche que l'arborescence, pour n'ouvrir qu'une Pull Request.

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
- Q0 (à confirmer) : faut-il quand même installer JDK 21.0.3 et Maven 3.9.6 à côté de l'existant ? Ils ne servent pas à la 1.2.0.2.
- Q4 (en partie close, 7 octobre) : membres et comptes GitHub reçus (voir Équipe). Reste ouverte pour l'étape 0.7 : la répartition des lots entre les membres.
- Q6 étendue (étape 2.1) : moteur compilé pour Java 11, nom de paquet `io.mosip.registration.liveness`.
