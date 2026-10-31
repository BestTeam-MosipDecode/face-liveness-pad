# PROGRESS

Suivi de travail interne du projet Face Liveness / PAD (MOSIP Decode 2026, sujet 04).
Plan de référence : `PLAN_FACE_LIVENESS.md`, à la racine du workspace `mosip_decode/` (hors dépôt).
Dernière mise à jour : 31 octobre 2026.

## Étape en cours

Soumission (issue n° 38) : date limite reportée au 2 novembre par MOSIP (e-mail du 26 octobre) ; envoi prévu le samedi 31 octobre, que l'embarquement sur Collab aboutisse ou non (voir Décisions). La vidéo, le deck et le dossier Drive sont prêts depuis le 25 octobre (issues n° 36 et 37 fermées, voir la section Soumission). Sur Collab, le client modifié se connecte, se synchronise et vérifie la vivacité du visage à l'embarquement ; l'enregistrement de l'embarquement est refusé par le serveur : depuis le rattachement de l'UIN par MOSIP le 29 octobre, la requête atteint IDA, qui répond par une erreur interne (`RPR-RTS-002`), signalée à MOSIP le même jour, et les parcours de bout en bout des issues n° 26 et 27 attendent cet embarquement (voir la section Collab).

## Terminé

Phase 0

- [x] 0.1 Inventaire de l'environnement (6 octobre)
- [x] 0.2 `docs/PROGRESS.md` créé, ainsi que le fichier de consignes local du workspace, hors dépôt (6 octobre)
- [x] 0.3 Branche de base choisie : `master` (1.2.0.2). Branche `feature/face-liveness` créée en local dans `registration-client/`, non poussée (7 octobre)
- [ ] 0.4 Faire tourner le Registration Client non modifié
  - [x] Build de la 1.2.0.2 avec JDK 11.0.32 et Maven 3.9.9 (7 octobre)
  - [x] `docs/dev-setup-windows.md`, première version (7 octobre). Sections 1 à 4 et 8 vérifiées sur le poste de référence ; sections 5 à 8 et 11 vérifiées sur Collab du 21 au 23 octobre, jusqu'à l'embarquement (issue n° 33).
  - [x] Démarrage du client et connexion d'un opérateur sur Collab (21 octobre), capture du visage avec vivacité à l'embarquement, avec le dispositif simulé (23 octobre)
  - [ ] Enregistrement de l'embarquement, refusé par le serveur : `RPR-RCT-001` faute d'UIN rattaché au compte opérateur, puis `RPR-RTS-002` (erreur interne d'IDA) après le rattachement du 29 octobre ; signalé à MOSIP (voir Blocages)
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
| Elisée Atonde | `Magloire04` | Documentation |
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

## Campagne de captures (issue n° 7, 14 octobre)

- Consignes de PrinceSpecial sur l'issue n° 7 : chaque membre filme les sujets de sa tranche (S10 à S19 pour Elisée), calcule les scores sur son poste avec `evaluate.py --score-only` et transmet le fichier de scores. Les images ne quittent pas la machine.
- Deux corrections publiées sur l'issue. Le fichier de scores part en privé à Temitayo et n'est jamais joint à une issue : le dépôt est public, le fichier donne un score par image et par pseudonyme, alors que le formulaire de consentement et le plan de test ne publient que des chiffres agrégés. Pour `paper_mask`, une autre personne que le sujet porte le masque (plan de test, section 3.2). `training/README.md` corrigé dans le même sens.
- Paquets manquants de `training/.venv` installés sur le poste d'Elisée (matplotlib, pandas, scikit-learn, scipy), avec son accord.
- Facteur du sourire passé de 1,10 à 1,06 par la Pull Request n° 61 : valeur provisoire, à valider par Elisée après les mesures de l'équipe.
- 15 octobre : sujet S10 enregistré sur le poste d'Elisée, 36 séquences sous les trois éclairages, avec un outil de séance local rangé sous `captures/` et ignoré par Git (enchaînement des séquences, contrôle des trous d'enregistrement et du sens des rotations, fabrication des supports imprimés). Masques portés par un volontaire signataire du formulaire, faute d'autre membre disponible : écart à la section 3.2 du plan de test, noté dans le registre local. Fichier de scores envoyé en privé à Temitayo, avec les observations sur le masque, le sourire et le sens des rotations. Aucun chiffre par sujet n'est publié ici.

## Mesures sur les séquences S10 (issues n° 28, 29 et 30, 19 octobre)

À la demande de PrinceSpecial (issue n° 29), lancées sur le poste d'Elisée depuis `main` (`1d95604`), avec son accord. Seuls les tableaux sont publiés, dans le commentaire de l'issue n° 29 ; les images et les journaux du dispositif restent sur le poste.

- Moteur (`ReplayBenchmarkTest`, une séquence par type, trois éclairages) : environ 56 ms par image analysée en moyenne sur 551 images, sous l'objectif de 100 ms ; la première séquence de chaque lancement est plus lente (73 à 114 ms). Photos, écrans et vidéo rejetés 15 fois sur 15. Masque en papier accepté en contre-jour, sans décision sous les deux autres éclairages. Défis : rotation à droite 3 sur 3, à gauche 2 sur 3, clignement 1 sur 3 (temps écoulé en lumière normale et faible), à analyser par PrinceSpecial.
- Dispositif simulé en mode replay (`replay_scenarios.py`) : avec les réglages de l'issue, 8 essais sur 32, à cause du script (flux demandé pour 10 s et observé 12 s). Avec une observation de 8 s, 24 sur 24 pour les flux, en SBI 0.9.5 et 1.0 ; déconnexion correcte ; image figée correcte côté dispositif, mais refusée par la règle du script. Cinq corrections du script et deux du test signalées sur l'issue n° 29.
- Le dispositif écrit chaque réponse en entier dans son journal, donc les enregistrements signés du visage rejoué. `replay_scenarios.log` a été supprimé après chaque lancement.
- Avast a arrêté le dispositif et mis son JAR en quarantaine sans afficher d'alerte (faux positif), et son analyse HTTPS a fait échouer les téléchargements de Maven (`PKIX path building failed`). Les deux contournements sont dans `docs/dev-setup-windows.md`.
- Masque en papier et deuxième sujet : décisions d'Elisée du 19 octobre, voir Décisions.

## Collab et réponses de MOSIP (21 octobre)

- Configuration WireGuard reçue de Varaniya par e-mail le 21 octobre, gardée hors dépôt : elle contient une clé privée. Tunnel installé et activé par Elisée.
- Nom d'hôte confirmé : `api-internal.collab.mosip.net`, une adresse privée joignable par le tunnel. `https://api-internal.collab.mosip.net/v1/syncdata/actuator/health` répond `UP`. Aucune version du client n'est publiée sur Collab : la vérification de mise à jour échoue en 404, sans bloquer.
- Client modifié configuré en local sans commit (`mosip-application.properties` marqué `skip-worktree`), lancé depuis un dossier hors des dépôts, `regclient-collab/`, où il écrit sa base, ses journaux et ses paquets. Option `-Djavax.net.ssl.trustStoreType=Windows-ROOT` nécessaire à cause de l'analyse HTTPS d'Avast. Premier démarrage en deux minutes environ, le temps de créer la clé TPM et la base chiffrée.
- Première connexion refusée à la demande de jeton : `KER-SYN-AUTH-001`, Invalid Request. Vérifié sur le poste : le client signe avec les clés TPM envoyées à MOSIP (mêmes index que `tpmdetails.txt`, nom de machine `ELISEE`), et l'horloge est à l'heure. Demande envoyée à MOSIP par e-mail et sur le forum : vérifier l'enregistrement et l'activation de la machine, et son rattachement au centre du compte d'opérateur.
- Machine activée par Varaniya le 21 octobre, en réponse. Connexion de l'opérateur réussie, première synchronisation terminée à 11 h 02. L'en-tête affiche le centre comme « null (null) » (centre 10905), sans effet sur la connexion.
- À l'embarquement, `BIO_SDK_007` : le client charge le SDK biométrique de démonstration `io.mosip.mock.sdk.impl.SampleSDK`. Ajoutés au classpath du script de lancement, hors dépôt : `mock-sdk` 1.2.0.2 et `kernel-bio-converter` 1.2.0.1, empreintes SHA-1 vérifiées sur Maven Central.
- Embarquement bloqué ensuite sur la confiance du dispositif de visage. Avec les clés de test publiques du dispositif simulé : `REG-MDM-108`, chaîne de certificats non reconnue. Avec les clés du dispositif simulé de Collab, copiées dans `mock-sbi/target` (ignoré par Git) : `KER-JWS-107`, ces certificats ont expiré le 13 septembre 2025. La validation de signature n'est pas contournée. E-mail envoyé à MOSIP le 21 octobre pour demander des certificats renouvelés ; en attente.
- 22 octobre. Clés de dispositif renouvelées par Varaniya (`Device.p12` et `FTM.p12`, valables jusqu'au 18 août 2027), installées dans `mock-sbi/target` (ignoré par Git). Le client valide la chaîne de confiance du dispositif, sans `KER-JWS-107` ni `REG-MDM-108`. Note de qualité du dispositif simulé portée de 44 à 94, comme dans la configuration d'exemple de MOSIP, pour passer le seuil de 90 % de Collab.
- 23 octobre. Connexion à faire en anglais : le centre 10905 n'a pas de données en français. En français, le client ne trouve pas son centre (affiché « null (null) ») et refuse toute synchronisation après la première (`CENTER_INACTIVE`).
- 23 octobre. L'embarquement capturait le visage sans vivacité (`BiometricsController`, écart n° 1). Vivacité ajoutée (registration-client#15), puis panneau compact et consignes de rotation avec flèche et retour face à la caméra (#16). Sur Collab, vivacité vérifiée et visage capturé à l'embarquement.
- 23 octobre. Le flux du dispositif dure `mosip.registration.capture_time_out`, environ 10 s sur Collab : une vérification plus longue échoue (flux coupé, puis `error 700` du dispositif à la capture). Laissé tel quel, voir Décisions.
- 23 octobre. L'enregistrement de l'embarquement est refusé par le serveur : `RPR-RCT-001`, « Unable to access API », à 8 h 11 min 41 s UTC. Le client 1.2.0.2 n'affichait que « ERROR », à cause d'une exception sur une réponse d'IDA sans objet `response` ; corrigé dans registration-client#17. Signalé à Varaniya par e-mail le 23 octobre, avec les questions de l'AMA du 22 octobre, qui a été manquée (Q15, Q16, Android).
- 23 octobre. Réponses de Varaniya par e-mail :
  - embarquement : MOSIP demande comment le compte opérateur a obtenu son UIN, la comparaison se faisant avec l'identité de ce UIN. Réponse d'Elisée : compte fourni par MOSIP le 16 octobre, sans UIN ; demande de rattacher un UIN au compte ou d'indiquer comment l'obtenir ;
  - paquet (Q15) : quand la vivacité échoue, le résident fait les défis, qui doivent figurer dans les détails du paquet ;
  - emplacement de la vivacité (Q16) : détectée par le dispositif (Mock-MDS) ; si le score n'atteint pas le seuil, le client lance les défis ;
  - Android : la note de conception suffit.
- 23 octobre, au soir. Réponse de Varaniya sur l'embarquement : l'opérateur doit déjà avoir un UIN, à obtenir par le portail d'auto-inscription de Collab. UIN obtenu par Elisée sur ce portail le soir même (gardé hors dépôt), puis rattachement au compte opérateur demandé à MOSIP par e-mail. Sans réponse au 25 octobre au soir : l'enregistrement de l'embarquement, puis les inscriptions de résidents des issues n° 26 et 27, attendent ce rattachement.
- 25 octobre. Scène 9 de la vidéo tournée sur Collab : embarquement de l'opérateur, score du dispositif, deux défis, vivacité vérifiée et visage capturé.
- 27 octobre. Varaniya répond que les données envoyées à IDA sont chiffrées et ne peuvent pas être déplacées, et demande le problème exact. Réponse d'Elisée avec l'appel en cause, relevé dans les journaux du client : `POST /registrationprocessor/v1/registrationstatus/auth`, identifiant de l'opérateur de type `USERID`, réponse `RPR-RCT-001` environ 7 s plus tard (23 octobre à 8 h 11 min 41 s UTC, 25 octobre à 14 h 46 min 50 s UTC). Varaniya transmet le problème à Nandu (MOSIP) comme bug de l'environnement Collab. Sans nouvelle au 29 octobre.
- 29 octobre. Dispositif simulé démarré par `run_reg.bat` dans une session où le JDK 21 est celui par défaut : le flux marche, mais chaque capture échoue (`UnsatisfiedLinkError: no opencv_java490`, le convertisseur ISO de MOSIP charge OpenCV d'une façon refusée à partir de Java 12). Relancé avec le JDK 11, comme le demande son README ; `dev-setup-windows.md`, section 8, le précise désormais. Sur Collab, la plupart des essais échouent sur la durée du flux de 10 s : vivacité établie trop tard, ou capture du dispositif pas terminée avant la coupure. Après trois échecs, le Scan de l'embarquement est bloqué jusqu'au redémarrage du client.
- 29 octobre. Nandu (MOSIP) demande un nouvel essai avec l'heure UTC de l'enregistrement. Essai à 13 h 30 min 51 s UTC : `RPR-RCT-001`, 1,3 s plus tard. Nandu trouve la cause : aucun UIN n'était rattaché au compte opérateur ; MOSIP rattache l'UIN obtenu le 23 octobre. Nouvel essai à 16 h 26 min 52 s UTC, avec le visage de la personne inscrite : la requête atteint IDA, qui répond `500 Internal Server Error`, et le service d'état d'enregistrement renvoie `RPR-RTS-002` 7,8 s plus tard. Heure et erreur transmises à Nandu. Sans réponse au 31 octobre au matin.
- Réponses de Varaniya sur le forum, le 21 octobre, aux questions d'Elisée du 20 octobre :
  - Collab : l'accès est fourni, signaler tout problème ;
  - démonstration : MOSIP compte sur une démonstration sur Collab, l'application de démo reste la solution de repli ;
  - cartes étudiantes : dans le même dossier Drive que les autres éléments de la soumission ;
  - pitch final : date et format communiqués aux équipes présélectionnées après la date limite ; présentation du deck, puis démonstration en direct de préférence, ou enregistrée.
- Les réponses aux autres questions envoyées par e-mail (Q15, Q16) sont promises pour la semaine suivante, donc après la date limite.
- L'AMA du mercredi 21 octobre est reportée au jeudi 22 octobre, de 12 h 30 à 13 h 30 à Cotonou (annonce du forum du 21 octobre).

## Démonstration et vidéo (issue n° 36, 22 au 25 octobre)

- Démo reconstruite le 21 octobre avec le moteur de `main` : seuil de passage à 1,00 par défaut.
- Testée par Elisée le 22 octobre avec la webcam et le dispositif simulé en mode webcam, parcours résident en français, puis opérateur. Les quatre cas prévus donnent le résultat et le message attendus :
  - visage réel accepté après deux défis, puis capture vérifiée ;
  - photo ou écran de téléphone refusés avec le message générique, puis nouvelle tentative ;
  - avec la case « Allow a passive pass (0.85) », le visage passe sans défi, pour la scène de comparaison ;
  - tentatives épuisées : « Continuer sous exception » pour le résident, autre méthode de connexion pour l'opérateur.
- 25 octobre. Dix clips tournés par Elisée d'après `docs/demo-script.md`, dont la scène 9 dans le client modifié sur Collab. Montage en deux versions, sous-titres avec voix de synthèse et sous-titres seuls ; la version sans voix est retenue (voir Décisions) : 4 min 35, 1080p, sous-titres dans un bandeau sous l'image, calés sur les événements de chaque clip et vérifiés image par image. Clips et montage restent dans `captures/` (ignoré par Git). Vidéo déposée dans `01_Video` du dossier Drive ; issue n° 36 fermée par Elisée.
- Les cas vus à l'écran pendant le tournage sont reportés dans `docs/test-results.md`, section 2.1 (#117).

## Soumission (issues n° 37 et 38, 25 octobre)

- Deck « Face Liveness Pitch » mis à jour le 25 octobre d'après `docs/pitch-outline.md` : score du dispositif, défis à chaque session (15 s chacun), paquet, embarquement sur Collab, deux sujets, 193 tests du moteur. Exporté en .pptx et .pdf par Elisée dans `02_Pitch` ; issue n° 37 fermée par Elisée.
- Documentation remise à jour avant les exports (#117) : lignes de statut, vérification de la signature du JAR du moteur, entrée `faceLiveness` du paquet, cas vus à l'écran, maquettes r04, r05, r12, a02 et a03.
- Dossier Drive `MOSIP-Decode-2026_Best_Team_PS04`, partagé en lecture par lien : vidéo, deck, PDF de `03_Source` à `06_Documentation` construits depuis `main` après #117, et `07_Student_IDs` pour la carte étudiante de chaque membre. Les PDF se reconstruisent avec les scripts `drive_*.py` (hors dépôt).
- Reste avant l'envoi du 31 octobre : `03_Source/README.pdf` reconstruit sur le dernier commit, lien du Drive vérifié en navigation privée, e-mail de `docs/submission-email.md` envoyé par Elisée, puis issue n° 38 fermée.

## Nettoyage de l'historique et vérification des commits (13 octobre)

- 9 commits poussés sur `main` le 13 octobre entre 10 h 52 et 11 h 11 portaient une ligne `Co-Authored-By` ajoutée automatiquement par un outil, avec une adresse extérieure à l'équipe. GitHub l'affichait comme contributeur du dépôt. 5 descriptions de Pull Request (n° 51, 53, 54, 55, 56) se terminaient par une mention de génération.
- Avec l'accord d'Elisée, l'historique de `main` a été réécrit à partir du commit `883b0aa` pour retirer ces lignes : mêmes fichiers, mêmes 88 commits, mêmes auteurs et dates. Push forcé lancé par Elisée, avec contrôle préalable que `main` n'avait pas bougé. Nouveau `main` : `9dfe633`. Les 5 descriptions ont été nettoyées.
- Restent hors de portée : les anciens commits visibles dans l'onglet « Commits » des Pull Requests fusionnées, et l'historique des modifications des descriptions (supprimable à la main depuis le menu « edited »).
- Prévention : vérification `Commit messages` sur chaque Pull Request (`.github/workflows/commit-messages.yml`), même contrôle avant commit sur les postes (`.githooks/commit-msg`, à activer avec `git config core.hooksPath .githooks`), règle de protection de `main` (Pull Request obligatoire, vérification obligatoire, ni push forcé ni suppression). Un co-auteur n'est accepté que s'il est membre de l'équipe.
- Chaque membre doit remettre son clone à jour (`git fetch origin`, `git switch main`, `git reset --hard origin/main`).
- Même dispositif sur le fork `registration-client` (Pull Request n° 1 du fork) : script `.github/scripts/check-commit-messages.sh`, workflow `Commit messages`, hook `.githooks/commit-msg`, règle de protection de `feature/face-liveness` (mêmes règles que pour `main`). Ces fichiers restent propres au fork et n'entrent dans aucune proposition vers `mosip/registration-client`. Les deux workflows hérités de MOSIP (`push-trigger.yml`, `clear-artifacts.yml`) ont besoin des secrets de MOSIP : ils se désactivent depuis l'onglet Actions du fork, sans modifier leurs fichiers.
- Un poste qui a déjà un `core.hooksPath` global n'a pas besoin du réglage local : un fichier `.git/hooks/commit-msg` qui appelle `.githooks/commit-msg` suffit, si le hook global relaie vers `.git/hooks`.

## Note de conception Android (issue n° 32)

`docs/android.md`, rédigé le 8 octobre d'après le code de `mosip/android-registration-client` (branche `master`, licence MIT), lu dans un clone partiel hors dépôt. À retenir :

- Le client Android est une application Flutter avec des modules natifs Java, reliés par des API Pigeon. Il appelle l'application SBI par trois intents Android (`io.sbi.device`, `.Info`, `.rCapture`) : c'est elle qui tient la caméra, et le client ne reçoit jamais de flux. Il ne déclare pas la permission `CAMERA`.
- La conception du bureau, fondée sur le flux, ne se transpose pas telle quelle. Recommandation : vivacité dans le client avec la caméra de la tablette (CameraX) avant l'intent `rCapture`, puis contrôle de l'image capturée. Autres options écartées ou reportées : vivacité dans l'application SBI, intent de flux hors spécification.
- Contrainte pour le moteur dès maintenant (issues n° 10 et 11) : `java.awt` et `javax.imageio` n'existent pas sur Android. Le cœur du moteur doit travailler sur des pixels déjà décodés, le décodage JPEG passant par un adaptateur de plateforme. ONNX Runtime a une version Android (`onnxruntime-android`) avec la même API Java, en 1.30.0 comme la version de bureau.
- Contributeurs GitHub : la page du dépôt ne montre que Magloire04, car GitHub ne compte que les auteurs de commits présents sur `main`. Les trois autres membres apparaîtront avec leurs propres commits. Aucun commit ne sera créé à leur nom (règle 4.1.2).

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

## AMA du 14 octobre

Relevé dans la transcription fournie par Elisée. Elle commence à 12 h 52 (heure de Cotonou), le début de la séance manque. Arrivé en retard, Elisée n'a pu poser que la question de l'accès Collab.

- Accès Collab : Varaniya annonce l'envoi des accès par e-mail avant la fin de la journée du 14 octobre.
- Licence de MiniFASNet, question posée par une autre équipe du sujet 04 : pour Jana, le modèle peut servir pour Decode mais pas pour un usage commercial, selon les licences et les jeux de données d'entraînement. Anusha précise par écrit que les modèles sous licence MPL ou Apache sont acceptés, sans restriction sur les poids pré-entraînés. Nos poids MiniFASNet et Face Mesh sont sous Apache 2.0, YuNet sous MIT (`THIRD_PARTY_NOTICES.md`).
- SBI, réponse d'Anusha à un autre participant : découverte, informations du dispositif, puis RCAPTURE, présenté comme l'endpoint où la vérification de vivacité doit avoir lieu, avec la MockMDS pour relier le tout au Registration Client. Notre conception analyse le flux STREAM côté client et contrôle l'image signée de RCAPTURE (`docs/decision-flow.md`, section 6), comme le demande le sujet 04. À confirmer avec Varaniya et Jana (Q16).
- Soumission : date limite inchangée, 26 octobre à 23 h 59 IST, soit 19 h 29 à Cotonou. E-mail avec le nom d'équipe, le sujet et le lien d'un dossier Drive qui contient le code et les fichiers. Au pitch, une démonstration en direct est appréciée, avec la vidéo en secours, dans un fichier séparé.
- Séances : chaque mercredi de 17 h à 18 h IST, soit de 12 h 30 à 13 h 30 à Cotonou. Le 21 octobre sera sans doute la dernière. Entre deux séances : `community.mosip.io` (recommandé, visible de tous) ou `decode@mosip.io`. L'enregistrement est disponible dans le « resource deck » partagé pendant la séance.

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

- 15 octobre. Après la dernière tentative ratée d'un résident : procédure d'exception de la section 7.1 de `docs/decision-flow.md` (option A), décidée par Elisée. L'opérateur continue sous exception, le dossier est marqué et audité, un superviseur s'authentifie à la soumission. Le marquage dans le paquet reste provisoire en attendant la réponse des mentors (Q15). Demandé à Tobi3h dans la Pull Request n° 6 du fork, après la correction de sécurité de la capture et l'authentification opérateur et superviseur (n° 23).
- 16 octobre. Tant que l'authentification du superviseur par le visage ne passe pas par la vérification de vivacité (#23), le visage n'est pas proposé au superviseur pour valider un dossier marqué en exception : mot de passe ou empreinte seulement. Décidé par Elisée à la relecture de la Pull Request n° 9 du fork, qui doit aussi corriger le contrôle du superviseur (casse de l'identifiant, rôle) et la perte possible du marquage.
- 19 octobre. Masque en papier porté par une autre personne : limite connue, documentée avec son taux sur le premier sujet (option 1 proposée par PrinceSpecial sur l'issue n° 7), sans changement du code ni des seuils avant la soumission. Un contrôle dédié reste une piste pour plus tard. Décidé par Elisée.
- 19 octobre. Pas de deuxième sujet enregistré : les résultats de l'issue n° 7 sont publiés comme préliminaires, sur un seul sujet, avec les seuils 0,85 et 0,30 conservés (toujours provisoires). Décidé par Elisée.
- 19 octobre. Sans accès Collab, la vidéo (issue n° 36) et la démonstration en direct reposent sur une application autonome, `demo/` : le moteur, le panneau d'état et les messages du client modifié, le dispositif simulé en webcam par SBI. Les plans du vrai client s'ajouteront si l'accès arrive avant le 21 octobre. Le pitch (issue n° 37) est monté en diapositives exportables en .pptx ou PDF, après `docs/pitch-outline.md`. Décidé par Elisée.
- 21 octobre. Seuil de passage à 1,00 par défaut pour les trois parcours (option B de l'issue n° 7). Avec le deuxième sujet (S30, captures de SilverioMen du 20 octobre), deux photos brillantes et un écran de téléphone passaient au seuil de 0,85. L'analyse passive rejette toujours les attaques nettes (seuil 0,30), et chaque session se termine par les défis aléatoires, pour un coût d'environ 10 s. Le masque en papier reste une limite connue. Décidé par Elisée ; valeurs par défaut, configuration et résultats mis à jour par PrinceSpecial, logique de décision, sécurité, démo et pitch par Magloire04. Cas limite d'une médiane exactement à 1,0 corrigé dans le moteur par Tobi3h (#105) : au seuil de 1,00, aucune session ne passe sur l'analyse passive seule. Valeur par défaut du moteur passée à 1,00 par PrinceSpecial (#108, issue n° 7 fermée) ; le client du fork la lit sans changement de code.
- 23 octobre. Délai par action (`active.challenge_timeout_ms`) porté de 8 à 15 s par défaut pour les trois parcours, dans le moteur et dans le prototype Python. Aux essais de l'embarquement sur Collab, 8 s puis 12 s se sont révélées trop courtes pour lire la consigne et faire l'action. Les résultats déjà publiés restent ceux mesurés à 8 s. Les échecs venaient aussi de la consigne de rotation, qui ne disait pas de revenir face à la caméra (corrigée dans registration-client#16), et de la durée du flux sur Collab (entrée suivante). Décidé par Elisée.
- 23 octobre. Vivacité ajoutée à l'embarquement des opérateurs et des superviseurs (option A), réalisée par Magloire04 : le visage enregistré à l'embarquement sert de référence à toutes les connexions par le visage. Réglages du parcours `OPERATOR`, blocage après la dernière tentative (registration-client#15 et #16, `docs/workflows.md`, section 2.4). Décidé par Elisée.
- 23 octobre. Durée du flux vidéo laissée à `mosip.registration.capture_time_out` de l'environnement, sans changement du client. Sur Collab, environ 10 s : une vérification doit y tenir dans ce temps. La valeur recommandée pour un déploiement est dans `docs/configuration.md`, section 5. Décidé par Elisée.
- 23 octobre. Score passif calculé dans le dispositif, comme MOSIP l'a demandé (option B) : le dispositif simulé note chaque image avec les modèles du moteur et envoie le score avec elle (`X-Liveness-Score`) ; le client l'utilise à la place du sien et lance les défis s'il n'atteint pas le seuil. Sans score du dispositif, le client calcule le sien. Face-liveness-pad#115 et registration-client#18. Décidé par Elisée.
- 23 octobre. La vérification de chaque visage part dans le paquet (entrée `faceLiveness` des métadonnées : décision, tentative, source du score passif, défis et résultat de chacun, version des modèles ; sans image ni score), en plus de `faceLivenessException`. Registration-client#18. Décidé par Elisée.
- 25 octobre. Vidéo de la soumission : version sous-titrée sans voix, sous-titres dans un bandeau sous l'image pour ne jamais cacher l'interface. Décidé par Elisée.
- 26 octobre. Date limite reportée au 2 novembre 2026 par MOSIP (e-mail du 26 octobre ; heure à confirmer dans les consignes de soumission). Envoi interne fixé au samedi 31 octobre. La semaine sert à relancer MOSIP pour le rattachement de l'UIN et, s'il arrive, à terminer les issues n° 26 et 27 sur Collab ; le dossier du 25 octobre reste prêt en solution de repli. Décidé par Elisée.

## Blocages

- Accès Collab. Elisée a envoyé le formulaire de demande, puis un e-mail à `decode@mosip.io` le 7 octobre à 14 h 25. Réponse de MOSIP Decode (Pragya, Community Manager) le 7 octobre à 21 h 58 : la demande est transmise aux mentors du sujet 04, Varaniya et Jana, pour vérifier l'accès Collab et aider à la mise en route du client. En attente de leur retour au 8 octobre. À l'AMA du 14 octobre, Varaniya annonce l'envoi des accès par e-mail le jour même. Toujours aucun accès le 15 octobre après-midi : relance à faire par e-mail, et démonstration autonome (moteur, dispositif simulé, écran de capture) envisagée en solution de repli. Sans enregistrement de la machine, sans identifiants et sans accès WireGuard, le client ne peut pas se synchroniser avec le serveur. La fin de l'étape 0.4 attend cet accès. Les étapes 0.5, 0.6 et 0.8 n'en dépendent pas. Identifiants d'opérateur reçus par e-mail le 16 octobre (gardés hors dépôt) ; Elisée a répondu en demandant la configuration WireGuard et le nom d'hôte de l'environnement. Configuration WireGuard reçue le 21 octobre : connexion et synchronisation fonctionnent depuis l'activation de la machine par MOSIP ; depuis les clés renouvelées le 22 octobre, l'embarquement va jusqu'à l'enregistrement, refusé par le serveur (`RPR-RCT-001`) et signalé à MOSIP le 23 octobre ; après le rattachement de l'UIN le 29 octobre, refusé par IDA (`RPR-RTS-002`) (voir la section Collab).
- Embarquement sur Collab (issues n° 26 et 27). Cause connue depuis le 23 octobre au soir : l'embarquement compare le visage à l'identité de l'UIN rattaché au compte opérateur, et le compte fourni par MOSIP n'en a pas. UIN obtenu par le portail d'auto-inscription ; rattachement demandé à MOSIP. Le 27 octobre, MOSIP confie l'échec à son équipe. Le 29 octobre, MOSIP rattache l'UIN au compte ; l'enregistrement atteint alors IDA, qui répond par une erreur interne (`RPR-RTS-002`), signalée à MOSIP. Sans embarquement enregistré, pas d'inscription de résident sur Collab : la matrice fonctionnelle reste à faire dans le client.

## Écarts relevés par rapport au plan

1. Section 4.1 : le tableau des points d'accroche cite `BiometricsController` pour la capture du résident. `GenericBiometricsController` (lié à `GenericBiometricFXML.fxml`) contient le même enchaînement : `rCaptureTaskService()` puis `streamer.startStream(...)`. Les deux contrôleurs sont à traiter en phase 4. À confirmer à l'exécution : lequel sert le parcours résident. Confirmé sur Collab le 23 octobre : `GenericBiometricsController` sert la capture du résident, `BiometricsController` l'embarquement des opérateurs et des superviseurs, désormais avec la vivacité (registration-client#15).
2. Sections 3, 5.3 et 7.1 : le plan suppose `develop` et Java 21. La base retenue est la 1.2.0.2 en Java 11 (voir Décisions).
3. Étape 0.4 : la classe principale est `Initialization`, pas `ClientApplication`.
4. Sections 1.1 et 9 : le plan situe la date limite en novembre. Elle est fixée au 26 octobre (voir la section sur l'AMA).
5. Section 3 : `Silent-Face-Anti-Spoofing/LICENSE` est supprimé de l'arbre de travail. Le texte reste lisible par `git show HEAD:LICENSE` : Apache License 2.0. Licence relevée à l'étape 0.6, dossier laissé tel quel.
6. Sujet officiel : les livrables citent "Android Registration Client changes" et "Consistent Desktop and Android experience". Le plan limite Android à une conception documentée. À garder en tête pour Q8.
7. Sujet officiel : le message de succès donné en exemple est "Good, face captured successfully". Le plan (7.5) écrit "Face captured successfully".
8. `Silent-Face-Anti-Spoofing/images/sample/pie.jpg` et `pie_result.jpg` ne viennent pas du dépôt d'origine. Point levé le 7 octobre : ce sont des copies de `image_T1.jpg` et de son résultat.

## Questions pour Elisée

- Q1 (close, 7 octobre) : base = 1.2.0.2.
- Q2 (close, 7 octobre) : date limite = 26 octobre 2026, 19 h 29 à Cotonou ; reportée au 2 novembre 2026 par MOSIP le 26 octobre.
- Q9 (close, 7 octobre) : calendrier resserré et périmètre réduit validés.
- Q3 (close, 21 octobre) : accès Collab. Identifiants reçus le 16 octobre, configuration WireGuard le 21 octobre, tous hors dépôt ; nom d'hôte confirmé, `api-internal.collab.mosip.net`. Machine activée par MOSIP le 21 octobre : connexion et synchronisation fonctionnent. L'embarquement attend un UIN rattaché au compte (voir Blocages).
- Q0 (close, 7 octobre) : JDK 21.0.3 et Maven 3.9.6 non installés, ils ne servent pas à la 1.2.0.2.
- Q4 (close, 7 octobre) : membres, comptes GitHub et répartition reçus (voir Équipe).
- Q10 (close, 7 octobre) : issues et étiquettes créées, phase 1 répartie.
- Q11 (close, 7 octobre) : Pull Request n° 1 fusionnée par Elisée.
- Q12 (close, 7 octobre) : Pull Request n° 39 fusionnée.
- Q13 (close, 7 octobre) : Pull Request n° 40 ouverte avec l'accord d'Elisée, puis fusionnée par Elisée.
- Q14 (close, 7 octobre) : clés `passive.timeout_ms` et `active.baseline_ms`, opération de contrôle de l'image capturée et règle de comptage des tentatives validées.
- Q7 (close, 7 octobre) : résident orienté vers la procédure d'exception de MOSIP après la dernière tentative.
- Q15 (close, 23 octobre) : comment la vivacité doit-elle être portée dans le paquet ? Réponse de Varaniya : les défis effectués figurent dans les détails du paquet. Entrée `faceLiveness` des métadonnées, en plus de `faceLivenessException` (registration-client#18).
- Q16 (close, 23 octobre) : où placer la vérification passive ? Réponse de Varaniya : dans le dispositif ; si le score n'atteint pas le seuil, le client lance les défis. Score envoyé par le dispositif simulé (#115) et utilisé par le client (registration-client#18).
- Q6 étendue (étape 2.1) : moteur compilé pour Java 11, nom de paquet `io.mosip.registration.liveness`.
