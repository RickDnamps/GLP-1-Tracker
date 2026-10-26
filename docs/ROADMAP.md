# Feuille de route : Suivi GLP-1

*Version de travail du 26 octobre 2026. Base : v1.0.0 (journal des doses, rappels avec « C'est fait », niveau estimé, rotation des 6 sites, suivi du stylo, poids/IMC/objectif, journal quotidien, 14 effets secondaires, CSV/JSON, FR/EN).*

**Objectif :** devenir le meilleur suivi GLP-1 **gratuit** sur Android. Tout ce que les concurrents mettent derrière un abonnement doit être gratuit chez nous, et tout doit fonctionner hors ligne, sans compte.

> **Déjà livré depuis :** import des pesées Health Connect et pesée directe par balance Bluetooth (Renpho QN / ES-CS20M, FITINDEX, profil standard).

---

## 1. Comparaison des concurrents

Légende : ✅ gratuit · 💲 payant (abonnement ou achat intégré) · ➖ absent · ❓ non vérifié ou flou

| Fonction | Shotsy | Pep (Shred Apps) | Glapp | MeAgain | GLP-1 Tracker: Ozempic, Wegovy (Wiserapps) | GlucoPal | **Nous v1.0** |
|---|---|---|---|---|---|---|---|
| Journal des doses + rappels | ✅ | ✅ | ✅ | 💲¹ | ✅ | ✅ | ✅ |
| Comprimés (Rybelsus, Wegovy oral) | ✅ (iOS seulement selon un test) | ✅ | ❓ | ❓ | ✅ | ❓ | ✅ |
| Rotation des sites | ✅ | ✅ | ✅ | ❓ | ✅ | ✅ (carte) | ✅ (6 sites) |
| **Courbe du niveau de médicament** | 💲 Shotsy+ | ✅ (selon Pep) | ✅ | 💲¹ | ❓ | ✅ ; 💲 prévisions | ✅ |
| Poids / IMC | ✅ | ✅ | ✅ | 💲¹ | ✅ | ✅ | ✅ |
| Mensurations (hanches, bras…) | ✅ (selon un utilisateur) | ✅ | ➖ | ➖ | ✅ | ❓ | ➖ (tour de taille seulement) |
| Photos de progrès | ❓ | ✅/💲 | ➖ | 💲¹ (« journey card ») | ❓ | ❓ | ➖ |
| Calories / macros | ✅ | ✅ (FatSecret) | ➖ (demande fréquente) | 💲¹ (photo IA, code-barres) | ✅ (+ index glycémique) | ✅ (photo) | ➖ (protéines, eau seulement) |
| Effets secondaires | ✅ | ✅ | ✅ (bilan quotidien) | 💲¹ | ✅ | ✅ | ✅ (14 + intensité) |
| Faim / « food noise », humeur | ❓ | ❓ | ✅ (+ énergie, sommeil, selles) | ❓ | ➖ | ❓ | ✅ (appétit, humeur) |
| Stock / stylos ouverts | ❓ | ❓ | ✅ (« supply management ») | ❓ | ❓ | ❓ | ⚠️ partiel (doses restantes) |
| Plan de titration (paliers) | ❓ | ❓ | ➖ | ❓ | ➖ | ❓ | ➖ |
| Rapports hebdo / jalons / prédictions | ✅ graphiques | ❓ | 💲 Glapp Max | 💲¹ | ➖ | 💲 Pro | ➖ |
| Export pour le médecin | 💲 (export) | ❓ | ❓ | ❓ | ✅ PDF | 💲 PDF (Pro) | ✅ CSV seulement |
| Widget écran d'accueil | ✅ (iOS, écran verrouillé) | ❓ | ❓ | ✅ | ✅ | ❓ | ➖ |
| Sync santé (Apple Health / Health Connect) | 💲 (selon un concurrent) | ✅ (selon Pep) | ❓ | ✅ (Apple Health) | ➖ | ❓ | ✅ (Health Connect + balances Bluetooth) |
| Plusieurs médicaments en même temps | ✅ | ❓ | ➖ (sémaglutide/tirzépatide seulement) | ❓ | ❓ | ❓ | ➖ |
| Sans compte / données locales | ➖ (connexion) | ❓ | ❓ | ➖ | ✅ (mais « données non chiffrées ») | ✅ | ✅ |
| Prix de l'abonnement | 39,99 à 59,99 $ US/an ou 9,99 à 19,99 $/mois | abonnement annuel + essai | Glapp Max (prix non publié) | achats intégrés de 14,99 à 119,99 $ | achats intégrés | 9,99 $/mois ou 34,99 $/an | **0 $** |
| Note Google Play (fin 2026) | 4,7 (≈12,7 k avis) | 4,7 (≈2 k) | 5,0 (≈320) | 4,6 (≈3 k) | 4,6 (175) | n.d. | — |

¹ Les avis Google Play de MeAgain disent qu'un abonnement est exigé après le questionnaire d'accueil. Le détail exact du mur payant n'est pas publié.

**À retenir :**
- Shotsy, le leader, met la **courbe de niveau** et l'**export** derrière Shotsy+. Glapp a ajouté en 2026 un palier payant (Max) pour les **rapports, jalons et prédictions**. GlucoPal fait payer les **rapports PDF** et les **prévisions**. Ce sont nos meilleures occasions : on les offre gratuitement.
- Le **plan de titration** et l'**aide en cas de dose oubliée** sont presque absents chez les concurrents. On peut s'en servir pour se démarquer.
- La seule application Android qui ressemble à la nôtre (Wiserapps : locale, sans compte, widget, PDF, mensurations, repas) n'a que 175 avis. Le créneau « gratuit, privé, complet » reste libre.

**Sources du tableau :**
[Shotsy sur Google Play](https://play.google.com/store/apps/details?id=com.shotsy.app&hl=en_US) ·
[Shotsy sur l'App Store](https://apps.apple.com/us/app/shotsy-glp-1-tracker/id6499510249) ·
[Shotsy : prix et alternatives (GLP3 Planner)](https://glp3planner.com/resources/shotsy-alternatives) ·
[Pep vs Shotsy (pepglp1.com)](https://pepglp1.com/pep-vs-shotsy-which-glp-1-medication-tracking-app-reigns-supreme/) ·
[Teardown de Shotsy (ScreensDesign)](https://screensdesign.com/showcase/shotsy-glp-1-tracker) ·
[Comparatif 2026 (Hello Regimen)](https://helloregimen.com/blog/best-glp1-tracker-apps-2026) ·
[Pep sur Google Play](https://play.google.com/store/apps/details?id=com.shredapps.glp1&hl=en_US) ·
[Pep sur l'App Store](https://apps.apple.com/us/app/glp-1-tracker-pep/id6504788281) ·
[Glapp sur Google Play](https://play.google.com/store/apps/details?id=io.glapp) ·
[Contenu de Glapp Max (blogue Glapp)](https://glapp.io/blog/whats-included-glapp-max-premium-membership/) ·
[MeAgain sur Google Play](https://play.google.com/store/apps/details?id=app.meagain.app&hl=en) ·
[MeAgain sur l'App Store](https://apps.apple.com/app/apple-store/id6744178534) ·
[Wiserapps sur Google Play](https://play.google.com/store/apps/details?id=co.wiserapps.glpTracker&hl=en) ·
[GlucoPal vs Shotsy](https://glucopal.com/blog/shotsy-alternative)

*Méthode : les prix et les murs payants viennent des fiches des magasins d'applications, des pages des éditeurs et de comparatifs, qui sont souvent écrits par des concurrents. Ce sont des signaux, pas des faits vérifiés : à revérifier dans les magasins avant toute communication publique.*

---

## 2. Ce que les utilisateurs reprochent ou demandent le plus

> **Limite de la recherche :** les outils utilisés n'avaient pas accès directement à reddit.com (accès bloqué). Les signaux de r/Mounjaro, r/Ozempic, r/Zepbound et r/CompoundedGLP1Drugs viennent de miroirs et de sources secondaires. Une lecture manuelle des fils récents est recommandée pour confirmer les priorités.

| # | Plainte ou demande | Ce que ça implique pour nous | Sources |
|---|---|---|---|
| 1 | **Fonctions de base payantes, ou qui le deviennent avec le temps** (courbe de niveau et export chez Shotsy, rapports hebdo passés à Glapp Max, abonnement obligatoire chez MeAgain, facturation abusive chez Gala) | Tout gratuit, sans mur payant, et le dire clairement dans la fiche Play | [Hello Regimen](https://helloregimen.com/blog/best-glp1-tracker-apps-2026) · [GLP3 Planner](https://glp3planner.com/resources/shotsy-alternatives) · [GlucoPal sur Glapp](https://glucopal.com/blog/glapp-alternative) · [MeAgain sur Play](https://play.google.com/store/apps/details?id=app.meagain.app&hl=en) · [Gala sur Play](https://play.google.com/store/apps/details?id=org.name.galacoach&hl=en_US) |
| 2 | **Données perdues** : des entrées disparaissent, ou l'application redemande la première dose après une reconnexion (Shotsy Android). Problèmes de rétention des données et de notifications | Sauvegarde automatique, fiabilité des rappels, aucune dépendance au nuage | [Shotsy sur Play](https://play.google.com/store/apps/details?id=com.shotsy.app&hl=en_US) · [Rapport Kimola sur Shotsy](https://kimola.com/reports/unlock-key-insights-from-shotsy-shot-tracker-user-reviews-app-store-us-156046) |
| 3 | **Android traité en citoyen de seconde zone** : moins de fonctions que sur iOS, plus de bogues, comprimés sur iOS seulement, Glapp arrivé tard sur Android | Être « Android d'abord » : widget, Health Connect, Material You | [Pep vs Shotsy](https://pepglp1.com/pep-vs-shotsy-which-glp-1-medication-tracking-app-reigns-supreme/) · [Avis Glapp (Trustpilot)](https://www.trustpilot.com/review/glapp.io) · [Teardown Marlvel](https://marlvel.ai/apps/com-everyday-shottracker/vs) |
| 4 | **Calories, macros et exercice dans la même application** (manque de Glapp le plus cité). Plusieurs utilisent Carb Manager ou Cronometer en plus | Journal alimentaire léger hors ligne : calories, fibres, repas favoris | [Avis Glapp (Trustpilot)](https://www.trustpilot.com/review/glapp.io?page=4) · [r/Mounjaro, recommandations (miroir)](https://reddit.rtrace.io/r/Mounjaro/comments/1e79sk4/tech_recommendations) |
| 5 | **Rapports trop rigides** : pas de rapport si on prend sa dose un jour plus tôt ou si on ne se pèse pas pendant un voyage | Bilans basés sur des périodes, qui tolèrent les données manquantes | [Glapp sur Play](https://play.google.com/store/apps/details?id=io.glapp) |
| 6 | **Stylos et fioles ouverts** : voir les fioles ouvertes en premier, suivre la date d'ouverture d'un stylo (durée de conservation) | Gestion du stock et des stylos ouverts, avec la date d'expiration après ouverture | [Glapp sur Play](https://play.google.com/store/apps/details?id=io.glapp) · [Semaya](https://apps.apple.com/us/app/id6743040801) · [ShotClock](https://apps.apple.com/app/id6751446970) · [FAQ Lilly Canada, conservation du KwikPen](https://www.lilly.com/en-CA/resources/faq/mounjaro-kwikpen/storage-mounjaro-kwikpen) |
| 7 | **Notes libres, plus de médicaments et peptides, doses personnalisées** | On a déjà les notes et « Autre ». Ajouter plusieurs médicaments en parallèle et les fioles magistrales | [Avis Glapp (Trustpilot)](https://nz.trustpilot.com/review/glapp.io?page=3) |
| 8 | **Magistral : confusion entre unités et mg** (seringues U-100), risque d'erreur de dose | Saisie en unités et en mg, calculatrice avec avertissements | [GLPme : unités vs mg](https://www.getglpme.com/guides/compounded-glp1-units/) · [Fonctions de ShotLog](https://shotlogapp.com/features) · [r/CompoundedGLP1Drugs (miroir)](https://redlib.hackliberty.org/r/CompoundedGLP1Drugs/comments/1ipepw5/shot_tracking_app) |
| 9 | **Préserver la masse musculaire** : protéines et musculation. De 25 à 40 % du poids perdu peut être de la masse maigre. Shotsy ne couvre pas la musculation | Objectif de protéines en g/kg, case « musculation », mensurations | [Comparatif (LearnMuscles)](https://learnmuscles.com/?p=169359) · [FitnessDrum](https://fitnessdrum.com/best-glp-1-apps/) |
| 10 | **Vie privée** : comptes liés à l'identité, données santé « collectées », Wiserapps déclare des données non chiffrées | Verrouillage biométrique, zéro réseau, sauvegarde chiffrée en option | [Wiserapps sur Play](https://play.google.com/store/apps/details?id=co.wiserapps.glpTracker&hl=en) · [GLPal](https://apps.apple.com/us/app/id6784454292) · [DoseDay](https://apps.apple.com/app/id6751439680) |
| 11 | **Préparer la dose et la rendre visible** : liste de préparation avant l'injection, widget, rappels de pesée, d'hydratation et de repas | Widget, rappels supplémentaires, liste « veille de dose » | [MeAgain sur l'App Store](https://apps.apple.com/app/apple-store/id6744178534) · [Pep sur l'App Store](https://apps.apple.com/us/app/glp-1-tracker-pep/id6504788281) · [GLP-1.COM](https://apps.apple.com/app/id6749719804) |
| 12 | **Dose oubliée : que faire ?** Beaucoup de contenus « règle des 5 jours / des 4 jours », signe que la question revient sans cesse | Assistant de dose oubliée, propre à chaque médicament | [Medical Letter](https://secure.medicalletter.org/system/files/private/TML-article-1708c.pdf) · [Pillo, Mounjaro](https://pillo.care/blog/mounjaro-missed-dose) · [Pillo, Ozempic](https://pillo.care/blog/missed-dose-of-ozempic) |
| 13 | **Paliers de dose** : l'étiquette de Zepbound prévoit 2,5 mg pendant 4 semaines, puis +2,5 mg après au moins 4 semaines par palier. Une seule application met en avant une « chronologie d'escalade » | Plan de titration avec le prochain palier et un rappel d'ordonnance | [Étiquette FDA de Zepbound](https://www.accessdata.fda.gov/drugsatFDA_docs/label/2025/217806s031lbl.pdf) · [GLP-1 Journey Tracker](https://appshunter.io/ios/app/glp-1-journey-tracker/id6760089056) |

---

## 3. Backlog priorisé

**Échelle d'effort :** **S** = moins d'environ 2 h · **M** = environ une demi-journée · **L** = plus d'une journée, ou plusieurs sous-systèmes.

### P0-0 (prérequis technique) : migrations Room et format de sauvegarde (S)

- **Pourquoi :** la base est en `version = 1` sans migration. Plusieurs P0 ajoutent des tables ou des colonnes. Sans migration, une mise à jour plante l'application ou efface les données, l'exact contraire de notre promesse (voir la plainte n° 2).
- **Ce qu'il faut faire :** passer à `version = 2` avec des `Migration` écrites à la main (ou `AutoMigration`). Versionner les schémas exportés dans `app/schemas/` et ajouter un test `MigrationTestHelper`. Ajouter un champ `formatVersion` au JSON de `Backup.kt`, avec une restauration compatible avec les anciennes versions (les champs absents prennent une valeur par défaut). Regrouper toutes les modifications de schéma des P0 dans **une seule migration v1 → v2**.
- **À coordonner** avec la personne qui travaille sur Health Connect (colonne `source` ou `externalId` sur `weights` ?) pour ne pas créer deux « version 2 » en conflit.

### P0 : prochaine version

| # | Élément | Effort |
|---|---|---|
| P0-1 | Widget d'écran d'accueil « Prochaine dose » | M |
| P0-2 | Plan de titration (paliers de dose) | M |
| P0-3 | Assistant « dose oubliée » et changement de jour | S |
| P0-4 | Stylos et stock : date d'ouverture, expiration, renouvellement | M |
| P0-5 | Rapport PDF pour le médecin | M |
| P0-6 | Mensurations corporelles et rapport taille/hauteur | S |
| P0-7 | Rappels supplémentaires (pesée, hydratation, journal, veille de dose) | S |
| P0-8 | Sauvegarde automatique | S/M |
| P0-9 | Calories et fibres, saisie rapide hors ligne | S |

---

#### P0-1 : Widget d'écran d'accueil « Prochaine dose » (M)

- **Pourquoi :** MeAgain, Wiserapps et Shotsy (écran verrouillé sur iOS) en ont un. C'est une fonction visible tous les jours, qui retient les utilisateurs, et elle montre qu'on prend Android au sérieux (plainte n° 3).
- **UX :**
  - **Petit (2×1) :** icône du médicament, « Dans 2 j · lun. 9 h », ou « En retard de 1 j » en couleur d'erreur.
  - **Moyen (4×2) :** même contenu, plus la dose prévue (palier P0-2), le site suggéré, le niveau estimé (« ≈ 68 % du pic ») et un bouton **« C'est fait »**. Ce bouton enregistre la dose prévue au site suggéré, comme l'action de notification, puis affiche un toast « Dose notée ». Un toucher ailleurs ouvre l'accueil.
  - Le widget suit le thème clair ou sombre et les couleurs dynamiques sur Android 12+.
- **Technique :** Jetpack Glance (`androidx.glance:glance-appwidget`). Mise à jour après chaque écriture dans le dépôt (`GlanceAppWidget.updateAll`) et par une alarme quotidienne à minuit pour le compte à rebours. Réutiliser `ReminderReceiver` pour l'action « C'est fait ».
- **Modèle de données :** aucun changement. Une nouvelle dépendance.

#### P0-2 : Plan de titration (paliers de dose) (M)

- **Pourquoi :** les paliers de 4 semaines rythment les premiers mois (étiquette de Zepbound : 2,5 mg pendant 4 semaines, puis +2,5 mg). La question « quand je monte ? » est l'une des plus fréquentes. Les concurrents s'y attardent peu, alors c'est un vrai facteur de différenciation.
- **UX :**
  - Écran **Traitement → Plan de doses**. On choisit un plan prérempli selon la monographie (Ozempic, Wegovy, Mounjaro/Zepbound, Saxenda, Trulicity) ou un plan personnalisé. Chaque palier a une dose, une durée en semaines (ou « jusqu'à nouvel ordre ») et une note. Mention bien visible : « Ton médecin décide des paliers. »
  - **Accueil :** « Semaine 3 sur 4 à 2,5 mg · Prochain palier : 5 mg vers le 12 nov. »
  - L'éditeur de dose préremplit la dose du palier en cours. Si la dose saisie diffère, il propose « Mettre à jour le plan ? ».
  - **Rappel 7 jours avant un changement de palier :** « Vérifie ton ordonnance pour 5 mg. »
  - Les graphiques de poids et de niveau montrent des repères verticaux à chaque changement de dose. Ces repères sont calculés à partir des doses enregistrées.
- **Modèle de données :** nouvelle table `dose_plan_steps(id, orderIndex, doseMg, durationWeeks: Int?, startDate: String?, notes)`. Le palier en cours est calculé à partir de la première dose de chaque palier. Les plans types vivent dans le code, à côté de `Medication`. À inclure dans le JSON et le CSV.

#### P0-3 : Assistant « dose oubliée » et changement de jour (S)

- **Pourquoi :** c'est la question angoissante n° 1 en cas de retard (« règle des 5 jours »). Aujourd'hui, l'accueil affiche seulement « en retard ». Aucun grand concurrent ne met en avant une aide propre à chaque médicament.
- **UX :**
  - Quand la dose est en retard, la carte affiche un encadré qui dépend du médicament. Exemple : « Ozempic : tu peux encore la prendre jusqu'au jeudi 9 h (5 jours après l'heure prévue). Après, saute-la et reprends ton jour habituel. »
  - Trois boutons : **Je la prends maintenant** (ouvre l'éditeur), **Je saute cette dose** (enregistre un saut et recale le compte à rebours), **Changer mon jour d'injection** (vérifie l'écart minimal avec la dernière dose).
  - Si le retard est très long (plus de 2 semaines), afficher : « Parle à ton médecin, il faudra peut-être recommencer à une dose plus faible. »
  - Toujours afficher le lien « Selon la monographie · demande à ton pharmacien ».
- **Règles à encoder** (à revérifier dans les monographies canadiennes avant publication) :

| Médicament | Fenêtre pour une dose en retard | Écart minimal pour changer de jour |
|---|---|---|
| Ozempic | ≤ 5 j (120 h) après l'heure prévue | ≥ 2 j (48 h) |
| Wegovy | à prendre si la prochaine dose est dans plus de 48 h, sinon sauter | ≥ 2 j (48 h) |
| Mounjaro, Zepbound | ≤ 4 j (96 h) | ≥ 3 j (72 h) |
| Trulicity | à prendre si la prochaine dose est dans 72 h ou plus | ≥ 3 j (72 h) |
| Quotidiens (Saxenda, Victoza, Rybelsus, Wegovy oral) | sauter la dose, ne jamais doubler. Saxenda/Victoza : après plus de 3 j d'arrêt, avertir du risque de devoir recommencer la titration | — |

- **Modèle de données :** ajouter `missedDoseWindowHours`, une règle (`WITHIN_WINDOW` ou `NEXT_DOSE_AWAY`) et `minHoursBetweenDoses` à l'enum `Medication`. Ajouter une colonne `skipped: Boolean = false` à `injections` : une ligne de dose « sautée » n'entre pas dans le modèle pharmacocinétique et ne décompte pas le stylo. Elle apparaît dans le rapport PDF.

#### P0-4 : Stylos et stock : date d'ouverture, expiration, renouvellement (M)

- **Pourquoi :** Glapp offre gratuitement un suivi du stock. Ses utilisateurs demandent de voir les fioles ouvertes en premier, et Semaya suit la date d'ouverture. Le KwikPen de Mounjaro se jette 30 jours après la première utilisation (FAQ de Lilly Canada). Pour Ozempic, plusieurs sources indiquent 56 jours. Nous ne suivons aujourd'hui que les doses restantes d'un seul stylo.
- **UX :**
  - La carte « Stylo » devient **« Stylo en cours »** : « 2 doses sur 4 restantes · ouvert il y a 26 j · à jeter dans 4 j ». Une barre de progression passe à l'orange à 7 jours de l'expiration.
  - **« J'ouvre un nouveau stylo »** demande le dosage, le nombre de doses et la date d'ouverture (aujourd'hui par défaut).
  - Section **Stock** : nombre de stylos ou de fioles neufs au frigo, puis **« Il te reste environ 5 semaines de traitement »**. Le calcul tient compte du plan de titration : un changement de palier peut rendre le stock inutilisable.
  - Notifications : stylo bientôt expiré, stylo vide, et **renouvellement d'ordonnance** X jours avant la fin du stock (7 par défaut, réglable).
- **Modèle de données :** nouvelle table `pens(id, medication, strengthMg, dosesTotal, openedDate, discardAfterDays, finishedDate?)`. La valeur par défaut de `discardAfterDays` dépend du médicament et reste modifiable. Ajouter `injections.penId` (nullable). Ajouter `stockUnopened: Int` et `refillLeadDays: Int` dans DataStore. Migrer les réglages actuels `penDosesTotal` et `penDosesRemaining` vers une ligne `pens`.

#### P0-5 : Rapport PDF pour le médecin (M)

- **Pourquoi :** Shotsy fait payer l'export et GlucoPal fait payer les rapports PDF. DoseUp et Wiserapps se vendent avec ce rapport. Un fichier CSV n'est pas lisible en 10 minutes de consultation, alors qu'un PDF propre l'est.
- **UX :**
  - **Paramètres → Rapport pour mon médecin.**
  - On choisit la période (depuis le début, 3 derniers mois, depuis le dernier rendez-vous) et les sections. On peut écrire des **« Questions pour mon médecin »** en texte libre.
  - On choisit la langue (français ou anglais). Utile au Québec : le médecin n'utilise pas forcément la langue de l'application.
  - Une page d'aperçu s'affiche, puis **Partager** ou **Enregistrer**.
- **Contenu :**
  - En-tête : prénom (facultatif), taille, poids de départ, poids actuel, objectif.
  - Traitement : médicament, paliers et dates.
  - Tableau des doses : date, dose, site, retards et doses sautées.
  - Graphique du poids avec la perte en kg et en %, l'IMC et le tour de taille.
  - Effets secondaires : fréquence × intensité moyenne par semaine.
  - Moyennes du journal : eau, protéines, appétit.
  - Pied de page : « Estimations, ne remplace pas un avis médical ».
- **Technique :** `android.graphics.pdf.PdfDocument` et `Canvas`, sans dépendance. Réutiliser la logique de `LineChart` sur le `Canvas`. Enregistrement avec `CreateDocument("application/pdf")`. Partage par un `FileProvider`, à ajouter au manifeste.
- **Modèle de données :** aucun changement. On peut garder `lastAppointmentDate` dans DataStore.

#### P0-6 : Mensurations corporelles et rapport taille/hauteur (S)

- **Pourquoi :** Pep, Wiserapps et Gilly (en partie payant) suivent les mensurations. Quand la balance stagne, les centimètres continuent souvent de baisser, ce qui encourage à continuer. C'est aussi un indicateur de préservation musculaire (plainte n° 9). Nous n'avons aujourd'hui que le tour de taille.
- **UX :**
  - L'écran Progrès gagne un onglet **Mensurations** : taille, hanches, poitrine, cou, bras, cuisse, et % de gras en option.
  - Une puce par mesure ouvre un mini graphique. Un total « −14 cm au total » s'affiche.
  - **Rapport taille/hauteur** avec une zone cible sous 0,5. C'est un indicateur simple et reconnu.
  - Saisie : une seule feuille pour toutes les mesures du jour, en cm ou en po selon les unités.
- **Modèle de données :** nouvelle table `measurements(id, timestamp, type: String, valueCm: Double)`. La migration copie `weights.waistCm` vers `type = WAIST`. On garde la colonne pour la compatibilité, sans plus y écrire. À inclure dans le CSV, le JSON et le PDF.

#### P0-7 : Rappels supplémentaires (S)

- **Pourquoi :** Pep propose des rappels de pesée, d'hydratation et de repas, et MeAgain une liste de préparation avant l'injection. Avec la fatigue et la baisse d'appétit, les utilisateurs oublient de boire et de noter leur journal. Sans données, il n'y a rien à mettre dans les bilans ni dans le PDF.
- **UX :** **Paramètres → Rappels**, avec un interrupteur et une heure pour chaque rappel.
  - **Pesée hebdomadaire :** jour et heure, par défaut le matin du jour de la dose. Texte : « Pèse-toi à jeun, après la toilette. »
  - **Hydratation :** jusqu'à 3 rappels entre 10 h et 18 h, **envoyés seulement si l'objectif n'est pas atteint**. Action « +250 ml » dans la notification.
  - **Journal du soir :** « Comment était ta journée ? » ouvre l'onglet Journal.
  - **Veille de dose :** « Demain c'est ta dose : sors ton stylo 30 min avant, site suggéré : cuisse gauche. »
- **Modèle de données :** nouvelles clés DataStore (interrupteur, heure, jour). Un canal de notification par type et des codes de requête distincts dans `ReminderScheduler`. À replanifier dans `BootReceiver`.

#### P0-8 : Sauvegarde automatique (S/M)

- **Pourquoi :** la plainte Android la plus grave contre Shotsy, c'est la perte de données. Chez nous, tout est local : si on perd le téléphone ou si on doit réinstaller (le README avertit déjà qu'une nouvelle clé de signature oblige à désinstaller), tout est perdu sans sauvegarde JSON manuelle. La sauvegarde Google d'Android n'est pas garantie.
- **UX :**
  - **Paramètres → Données → Sauvegarde automatique.** On choisit un dossier une seule fois (Drive, Nextcloud, stockage local…), avec le choix « chaque semaine » ou « chaque jour ». L'application garde les N dernières copies (5 par défaut).
  - Affichage « Dernière sauvegarde : il y a 3 j ✓ ». Une bannière apparaît si la dernière sauvegarde date de plus de 14 jours ou si le dossier est devenu inaccessible.
  - Au premier lancement après une réinstallation, l'application propose « Restaurer depuis une sauvegarde ? ».
- **Technique :** `ACTION_OPEN_DOCUMENT_TREE` avec une permission persistante. Tâche périodique `WorkManager`, une nouvelle dépendance, ou alarme existante. Réutiliser `Backup.toJson`.
- **Modèle de données :** clés DataStore `autoBackupUri`, `autoBackupFrequency`, `lastAutoBackupAt`. Le chiffrement par mot de passe est prévu en P1.

#### P0-9 : Calories et fibres, saisie rapide hors ligne (S)

- **Pourquoi :** c'est la demande n° 1 adressée à Glapp. Shotsy offre gratuitement calories, protéines et eau, et MeAgain met l'accent sur les protéines et les fibres. Les fibres comptent contre la constipation, un effet secondaire fréquent. On reste **sans base d'aliments** (hors ligne). Un compteur rapide suffit pour la majorité, et les mordus continuent d'utiliser Cronometer.
- **UX :**
  - La carte « Aujourd'hui » et l'onglet Journal gagnent deux jauges : **Calories** (objectif facultatif) et **Fibres** (25 g par défaut), à côté de l'eau et des protéines.
  - Boutons +/− avec des pas réglables.
  - **Repas favoris :** « Yogourt grec + petits fruits : 180 kcal, 20 g protéines, 4 g fibres ». Un toucher ajoute les valeurs aux jauges du jour.
  - Graphique de 30 jours dans Progrès.
- **Modèle de données :** ajouter les colonnes `caloriesKcal: Int = 0` et `fiberG: Int = 0` à `daily_logs`. Ajouter les objectifs `calorieGoal` et `fiberGoalG` dans DataStore. Nouvelle table `meal_favorites(id, name, kcal, proteinG, fiberG)`.

---

### P1 : versions suivantes

| Élément | Pourquoi | UX concrète | Modèle de données | Effort |
|---|---|---|---|---|
| **Verrouillage biométrique** | Données de santé sensibles. Les concurrents ont des données liées à l'identité ou non chiffrées (plainte n° 10) | Interrupteur « Verrouiller l'application ». Empreinte, visage ou NIP au retour après X minutes. Option pour masquer le nom du médicament dans les notifications sur l'écran verrouillé, et `FLAG_SECURE` en option (aperçu masqué dans les applications récentes) | DataStore `appLock`, `lockTimeout`, `hideNotifContent`. Dépendance `androidx.biometric` | S/M |
| **Photos de progrès** | Pep, MeAgain (« journey card ») et Gilly (payant) en ont. Très motivant | Photo de face, de profil et de dos via `TakePicture`, avec une silhouette fantôme pour garder le même cadrage. **Comparaison côte à côte avec curseur** entre deux dates. Photos privées : stockage interne, exclues de la galerie | Table `photos(id, timestamp, pose, filePath)`. Fichiers dans `filesDir/photos`. Archive ZIP en option dans la sauvegarde | M/L |
| **Bilan hebdomadaire et tendances** | Payant chez Glapp Max, et jugé trop rigide (plainte n° 5) | Carte et notification du dimanche : poids sur 7 jours (moyenne mobile), doses, effets secondaires les plus fréquents, objectifs atteints. **Effets secondaires selon le jour après l'injection** (carte de chaleur J0 à J6) et appétit comparé au niveau estimé. Tolère les données manquantes | Calculé, sans table. DataStore pour l'heure du bilan | M |
| **Jalons et prédiction d'objectif** | Glapp Max fait payer les jalons et les prédictions | Badges sobres (−5 %, −10 %, −15 %, changement de classe d'IMC, 12 semaines de régularité). « Au rythme actuel, objectif vers mars 2027 » (régression sur les 6 dernières semaines, avec une fourchette) | Table `milestones(type, achievedAt)` ou calcul à la volée | S |
| **Plusieurs médicaments en parallèle ou changement de molécule** | Shotsy l'offre. Cas réels : passage d'Ozempic à Mounjaro, GLP-1 + metformine, comprimé + injection | Liste « Mes traitements », chacun avec son horaire, ses rappels, son plan et son stylo. Courbe de niveau par molécule. Historique conservé lors d'un changement | Table `treatments` et `injections.treatmentId`. Migration des réglages actuels vers un traitement par défaut. Grosse refonte de `Schedule` et des rappels | L |
| **Fioles magistrales et unités U-100** | Le magistral est très répandu sur Reddit, et confondre unités et mg est dangereux (plainte n° 8) | Mode « fiole » : concentration en mg/mL, saisie de la dose en mg **ou** en unités avec conversion affichée (« 0,5 mg = 20 unités à 2,5 mg/mL »). Calculatrice de reconstitution avec de grands avertissements, et suivi du volume restant | Table `pens` étendue : `type` (PEN ou VIAL), `concentrationMgPerMl`, `volumeMl`. `injections.units` (nullable) | M |
| **Mode maintenance et intervalles souples** | Shotsy 3.0 a ajouté un mode maintenance. Doses espacées aux 10 ou 14 jours, microdosage | Intervalle en jours libres (déjà possible en partie) et mode « Maintenance » : la perte de poids n'est plus l'objectif principal, on suit la stabilité dans une plage de ± 2 kg | DataStore `phase` (PERTE ou MAINTENANCE), `maintenanceRangeKg` | S/M |
| **Journal enrichi** | Le bilan quotidien de Glapp suit l'énergie, le sommeil et les selles | Ajouter énergie (1 à 5), sommeil (h), selles (échelle de Bristol, dessins simples), fringales, case « musculation aujourd'hui » | Nouvelles colonnes dans `daily_logs` | S |
| **Protéines et préservation musculaire** | De 25 à 40 % du poids perdu peut être de la masse maigre (plainte n° 9) | Objectif de protéines proposé selon le poids visé (1,2 à 1,6 g/kg, réglable), compteur de séances de musculation par semaine, conseils courts | DataStore `proteinPerKg`. Colonne `strengthTraining` dans le journal | S |
| **Carte corporelle des sites** | GlucoPal, GLP-1 Log et d'autres ont une silhouette. 6 sites, c'est grossier pour l'abdomen | Silhouette avec 8 à 12 zones (4 quadrants abdominaux, haut et bas des cuisses, bras). Carte de chaleur selon la dernière utilisation. Option pour noter une réaction locale par site | Nouvelles valeurs de `InjectionSite`, avec correspondance des anciennes. Aucune migration de table | M |
| **Glycémie, HbA1c et tension (diabète de type 2)** | Beaucoup d'utilisateurs d'Ozempic ou de Mounjaro sont diabétiques, et nos concurrents visent surtout la perte de poids | Saisie rapide (mmol/L par défaut au Canada), graphique, valeurs dans le PDF. **À coordonner avec Health Connect** (lecture de BloodGlucose et BloodPressure) | Table `vitals(id, timestamp, type, value, value2?)` | M |
| **Rendez-vous médicaux** | Le PDF « depuis le dernier rendez-vous » et les questions à poser ont besoin de dates | Liste des rendez-vous, rappel la veille, questions à préparer qui alimentent le PDF | Table `appointments(id, date, title, notes)` | S |
| **Raccourcis d'application et tuile Réglages rapides** | Noter une dose en 2 touchers sans ouvrir l'application | Appui long sur l'icône : « Noter une dose », « Noter mon poids », « +250 ml ». Tuile Réglages rapides « +250 ml » | Aucun | S |
| **Sauvegarde chiffrée** | Complète P0-8 pour la vie privée | Mot de passe facultatif. JSON chiffré en AES-GCM, clé dérivée du mot de passe (PBKDF2) | Format de fichier v2 | S/M |

### P2 : plus tard / à explorer

| Élément | Pourquoi | UX / notes | Modèle de données | Effort |
|---|---|---|---|---|
| Code-barres des aliments (Open Food Facts) | MeAgain et Pep le proposent, mais il faut le réseau | **Activation volontaire**, seule requête réseau de l'application, expliquée clairement. Cache local des produits scannés | Table `foods` (cache) | L |
| Tuile et complication Wear OS | Rappels et « C'est fait » au poignet | Module `wear` séparé | Aucun (Data Layer) | L |
| Coût et assurance | Au Québec : RAMQ ou assurance privée, franchise, prix par dose | Coût par stylo ou fiole, total mensuel, rappel de renouvellement d'assurance | Colonnes `cost` dans `pens` | S |
| Thèmes et Material You complets, mise en page tablette ou pliable | Qualité perçue sur Android | Couleurs dynamiques partout, deux panneaux sur grand écran | Aucun | M |
| Espagnol et autres langues | Élargir l'audience | `strings.xml` es, pt-BR | Aucun | S chacune |
| Audit d'accessibilité | TalkBack, contrastes, grandes polices (tranche d'âge 40-65 ans) | Descriptions des graphiques, cibles de 48 dp | Aucun | M |
| Prédictions de « phases » (faim, énergie selon le jour du cycle) | Argument marketing de Glapp | Seulement si P1 « Bilan hebdo » montre des tendances personnelles fiables | Calculé | M |
| Transfert direct entre appareils | Changer de téléphone sans nuage | Code QR ou partage de fichier chiffré (repose sur la sauvegarde chiffrée) | Aucun | M |
| Export FHIR ou format clinique | Intégration future avec un DMÉ | JSON FHIR (Observation, MedicationAdministration) | Aucun | M |

---

## 4. Principes à garder pour chaque fonction

1. **Gratuit, sans compte, hors ligne par défaut.** Toute requête réseau doit être volontaire et expliquée.
2. **Aucune perte de données.** Chaque modification de schéma vient avec sa migration et son test. Chaque nouvelle table est ajoutée à la sauvegarde JSON, à l'export CSV et à la restauration.
3. **Sécurité clinique.** Pas de conseil de dose. On affiche les règles de la monographie avec « demande à ton médecin ou pharmacien ». Les valeurs des monographies sont revérifiées (version canadienne) avant chaque publication.
4. **Français québécois naturel d'abord**, anglais complet ensuite. Unités métriques par défaut (mmol/L, cm, kg) et impériales en option.
5. **Deux touchers maximum** pour les actions quotidiennes : noter une dose, de l'eau ou son poids.
