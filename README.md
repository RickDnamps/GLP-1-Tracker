# GLP-1 Tracker (Suivi GLP-1)

Application Android **gratuite, sans compte ni abonnement** pour suivre un traitement GLP-1
(Ozempic, Wegovy, Mounjaro, Zepbound, Saxenda, Victoza, Trulicity, Rybelsus, sémaglutide /
tirzépatide magistral ou tout autre médicament). Interface en français et en anglais.

## Fonctions

Tout est **gratuit**, **hors ligne** et **sans compte** — y compris ce que les autres apps font payer.

**Traitement**
- Journal des doses (injections ou comprimés) avec **silhouette du corps interactive** pour choisir le site et rotation suggérée.
- **Prochaine dose** : anneau de compte à rebours, rappel avec boutons « C'est fait » et « Plus tard », rappel la veille.
- **Dose oubliée** : conduite à tenir selon la monographie de chaque médicament, bouton « Sauter cette dose ».
- **Protection double dose** : avertit si une dose est saisie trop tôt après la précédente.
- **Plan de paliers** (titration) avec plans habituels par médicament et rappel du prochain palier.
- **Niveau estimé du médicament** dans le corps (modèle pharmacocinétique, biodisponibilité des comprimés).
- **Stylos et réserve** : doses restantes, stylos neufs, date à laquelle jeter le stylo entamé, alerte de renouvellement d'ordonnance.
- **Widget** « Prochaine dose » sur l'écran d'accueil du téléphone.

**Corps et progrès**
- Poids avec graphique animé, ligne d'objectif et repères de changement de dose ; IMC, % perdu, moyenne par semaine.
- **Balance Bluetooth** (Renpho QN / ES-CS20M, FITINDEX, profil standard) et **Health Connect** (Renpho, Withings, Fitbit, Samsung…).
- Mensurations (taille, hanches, poitrine, bras, cuisse, cou) et ratio taille/hauteur.
- **Badges** avec confettis, **prédiction de la date d'atteinte de l'objectif**.

**Analyses personnelles**
- Effets secondaires et appétit **selon le jour après l'injection** (« vos nausées culminent au jour 2 »).
- **Poids tendance** et détecteur de vrai plateau, jauge de rythme de perte, **coach protéines**.

**Journal quotidien**
- Eau (verre animé), protéines, repas avec calories/fibres et favoris, appétit, humeur, énergie, sommeil, « food noise », selles, activité et musculation.
- 14 effets secondaires avec intensité et résumé des 30 derniers jours.

**Outils et sécurité**
- **Calculateur de seringue U-100** pour les fioles (mg ↔ mL ↔ unités) avec alerte d'erreur de virgule.
- **Symptômes d'alerte** (pancréatite, vésicule, déshydratation, allergie…) avec appel direct 911 / 811 / 988.
- **Rapport PDF pour le médecin**, export CSV, sauvegarde JSON et **sauvegarde automatique quotidienne**.
- **Verrouillage** par empreinte / visage / NIP.

**Apparence** : thèmes clair/sombre, 5 couleurs d'accent, Material You, polices Plus Jakarta Sans et Outfit. Français et anglais.

> Cette application ne remplace pas l'avis d'un professionnel de la santé. Les niveaux de médicament
> sont des estimations théoriques.

## Installer l'APK

Chaque push compile l'application avec GitHub Actions et publie une **Release** :

1. Sur le téléphone, ouvrir l'onglet **Releases** du dépôt (la plus récente est « Latest »).
2. Télécharger `GLP-1-Tracker.apk`, l'ouvrir et autoriser l'installation depuis cette source.

L'APK est aussi disponible dans l'onglet **Actions** (artefact **GLP-1-Tracker-apk**).

### Clé de signature (recommandé)

Sans configuration, l'APK est signé avec une clé de débogage temporaire générée à chaque compilation :
Android refusera alors d'installer une nouvelle version par-dessus l'ancienne (il faudrait désinstaller et perdre les données,
sauf sauvegarde JSON). Pour des mises à jour sans perte, créer une clé une seule fois :

```bash
keytool -genkeypair -keystore release.keystore -alias glp1 -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.keystore   # copier le résultat
```

puis, dans **Settings → Secrets and variables → Actions** du dépôt, ajouter les secrets
`SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` (`glp1`) et `SIGNING_KEY_PASSWORD`.
Garder `release.keystore` en lieu sûr et ne jamais le publier dans le dépôt.

## Compiler localement

Android Studio (ou JDK 17 + SDK Android 36) :

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # tests unitaires
```

## Technique

Kotlin, Jetpack Compose (Material 3), Room, DataStore, AlarmManager, Bluetooth LE, Health Connect. Aucune connexion Internet, aucune publicité.
Voir `CLAUDE.md` pour l'architecture et `docs/ROADMAP.md` pour la feuille de route.
