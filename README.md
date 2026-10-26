# GLP-1 Tracker (Suivi GLP-1)

Application Android **gratuite, sans compte ni abonnement** pour suivre un traitement GLP-1
(Ozempic, Wegovy, Mounjaro, Zepbound, Saxenda, Victoza, Trulicity, Rybelsus, sémaglutide /
tirzépatide magistral ou tout autre médicament). Interface en français et en anglais.

## Fonctions

- **Doses** : journal des injections (ou comprimés), médicament, dose, site, notes ; modification et suppression.
- **Prochaine dose** : compte à rebours, retard, rappel par notification avec bouton « C'est fait ».
- **Niveau estimé du médicament** : courbe pharmacocinétique (demi-vie + absorption) sur 14 à 180 jours, avec projection.
- **Rotation des sites** : 6 sites (abdomen, cuisses, bras), dernier usage, nombre d'utilisations et site suggéré.
- **Stylo** : suivi des doses restantes et alerte quand il est presque vide.
- **Balance Bluetooth** : pesée directe avec les balances Renpho (QN, ES-CS20M), FITINDEX et toutes les balances au profil Bluetooth standard ; la balance est mémorisée, il suffit de monter dessus.
- **Health Connect** : import automatique des pesées de Renpho, Withings, Eufy, Fitbit, Samsung Health, Google Fit…
- **Poids** : graphique avec ligne d'objectif, poids de départ / actuel / visé, perte totale et en %, IMC, moyenne par semaine, tour de taille.
- **Journal quotidien** : eau et protéines (objectifs), appétit / « food noise », humeur, notes.
- **Effets secondaires** : 14 symptômes courants avec intensité, résumé des 30 derniers jours.
- **Données** : export CSV (à partager avec le médecin), sauvegarde et restauration JSON. Tout reste sur le téléphone.
- Thème clair / sombre, kg ou lb.

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

Android Studio (ou JDK 17 + SDK Android 35) :

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # tests unitaires
```

## Technique

Kotlin, Jetpack Compose (Material 3), Room, DataStore, AlarmManager, Bluetooth LE, Health Connect. Aucune connexion Internet, aucune publicité.
Voir `CLAUDE.md` pour l'architecture et `docs/ROADMAP.md` pour la feuille de route.
