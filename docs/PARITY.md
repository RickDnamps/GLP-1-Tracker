# Parité fonctionnelle : tout ce que les autres font payer, gratuit et hors ligne

*Version de travail du 26 octobre 2026. Complète `docs/ROADMAP.md` (qui reste la référence pour les P0 déjà livrés).*

**Objectif :** égaler ou dépasser **chaque** fonction, gratuite ou payante, des suivis GLP-1 connus (Shotsy/Shotsy+, Pep, Glapp/Glapp Max, MeAgain, GlucoPal Pro, Wiserapps, et les applications des programmes de télésanté Measured, Calibrate, Noom Med, Found, Ro Body, Juniper, Mochi), plus les nouveaux venus de 2025-2026. Contrainte du propriétaire : **gratuit, hors ligne, sans compte**. Aujourd'hui, l'APK ne déclare même pas la permission `INTERNET`. Il faut garder cet avantage (voir la section 3).

**Méthode et limites :**
- Les fiches de l'App Store et de Google Play, ainsi que reddit.com, étaient **bloquées** depuis l'environnement de recherche (le proxy refuse `apps.apple.com`, `play.google.com` et `reddit.com`). Les informations viennent des résumés du moteur de recherche (pages des éditeurs, extraits des fiches, comparatifs).
- Beaucoup de comparatifs sont **écrits par un concurrent** (Pep, Shotsy, GlucoPal, MeAgain, JabKeeper). Les prix et les murs payants sont des **signaux, pas des faits vérifiés**. Il faut les revérifier dans les magasins avant toute communication publique.
- « Notre statut » a été vérifié dans le code au 26 oct. 2026, y compris le travail non commité en cours (carte du corps, rapport PDF, jalons et prévision d'objectif).

**Légendes :**
- Chez les concurrents : ✅ gratuit · 💲 payant (abonnement ou achat) · 🏥 inclus dans un programme de télésanté payant (de 49 à 299 $ US par mois) · ❓ flou ou contradictoire.
- Chez nous : ✅ on l'a · 🟡 partiel ou en cours · ❌ manquant · ⛔ impossible strictement hors ligne (une alternative hors ligne est proposée).
- Effort (même échelle que la feuille de route) : **S** = moins d'environ 2 h · **M** = environ une demi-journée · **L** = plus d'une journée.

---

## 1. Les applications étudiées et leurs murs payants

| Application | Type | Prix signalé | Ce qui est payant (selon les sources) |
|---|---|---|---|
| **Shotsy / Shotsy+** | Suivi | 9,99 à 19,99 $/mois, 39,99 à 59,99 $/an | Courbe du niveau estimé, synchronisation Apple Health / Health Connect, probablement l'export. Gratuit : doses, rappels, sites, effets secondaires, poids, nutrition manuelle (v3.0). Autres fonctions : mode maintenance, 2 widgets (niveau et compte à rebours), plusieurs médicaments sur une même courbe, poids coloré par dose, graphiques par site et par dose, PDF, 17 langues |
| **Pep** (Shred Apps) | Suivi | Pro ≈ 40 $/an | Scan IA des repas en photo. Gratuit : doses, rappels, effets, poids, photos de progrès, mensurations, titration, code-barres, résumé quotidien, niveau estimé, calculateurs web (horaire, dose oubliée, protéines) |
| **Glapp / Glapp Max** | Suivi + compte | 4,99 $/sem., 9,99 $/mois, 39,99 $/an | Rapports de progrès hebdomadaires, jalons, prédictions (date d'objectif, points à 3 et 6 mois, alerte de stock bas), sans pub, soutien prioritaire. Gratuit : niveau estimé, **6 « phases » de la semaine**, bilan quotidien (food noise, faim, fringales, humeur, énergie, selles, sommeil), stock de stylos et de fioles avec date limite (BUD), **comparaison aux essais cliniques et aux autres utilisateurs**, web + iOS + Android. Aucun journal alimentaire |
| **MeAgain** | Suivi + compte | ≈ 69,99 $/an (de 9,99 à 119,99 $ selon les sources) | Cartes « Journey », photos de progrès, widget mascotte (protéines, eau, fibres, pas), chat IA illimité (Capy), séries ❓. Gratuit : doses ou comprimés, niveau, 5 modes de saisie des repas (photo IA, code-barres, voix, recherche, rapide), protéines, fibres, eau, **29 effets + personnalisés sur 0 à 10**, chronologie quotidienne |
| **GlucoPal Pro** | Suivi (iPhone surtout) | 9,99 $/mois, 29,99 à 34,99 $/an | Prévisions du niveau, rapports PDF. Gratuit : carte du corps avec historique, nutrition complète (calories, protéines, glucides, lipides, fibres, eau ; photo, code-barres, recherche), photos de progrès, Apple Health (pas, activité, repas) |
| **GLP-1 Tracker: Ozempic, Wegovy** (Wiserapps) | Suivi local | Achats intégrés | « Journaux illimités », graphiques avancés, export PDF. Gratuit : doses, sites, poids, IMC, mensurations, base d'aliments avec **index et charge glycémiques**, recettes à IG bas, objectifs eau/pas/sommeil, calendrier, widget, sans compte |
| **Gilly** | Suivi | Premium | Photos et comparaisons, mensurations, PDF, **export FSA/HSA**, analyses avancées, Apple Health, planification des doses, niveau, plusieurs médicaments. Gratuit : **60 jours d'historique**, un seul médicament |
| **JabKeeper** | Suivi local (iPhone) | 9,99 $ une fois | Tout sauf 2 doses : poids, effets liés au **jour de la dose**, repas et protéines, Apple Health, agenda |
| **MyFitnessPal / Lose It / MyNetDiary** | Nutrition | 79,99 $/an ; ≈ 39,99 $/an ; 59,99 $/an | MFP : soutien GLP-1 gratuit, mais code-barres, scan de repas, voix et export payants. Lose It : soutien GLP-1 payant. MyNetDiary : compagnon GLP-1 payant (protéines, fibres, eau, symptômes digestifs, menus GLP-1, jeûne), coach IA en Premium Plus |
| **WeightWatchers** | Programme | Abonnement | Protéines et fibres, injections et effets, vidéos de musculation, scan corporel IA, sommeil, communauté |
| **Calibrate** | Télésanté 🏥 | ≈ 199 $/mois | 4 piliers (alimentation, sommeil, activité, santé émotionnelle), balance connectée, coaching vidéo, cursus, messagerie |
| **Noom Med** (compagnon GLP-1) | Télésanté 🏥 | 179 à 299 $/mois, médicament inclus | Suivi et « guidage » des doses, cibles de protéines et de macros par IA, repas en photo, programme de musculation « Muscle Defense », scan corporel IA, trousse contre les effets secondaires, eau, pas, microdosage |
| **Found** | Télésanté 🏥 | 49 à 149 $/mois | Messagerie avec l'équipe, coach, plans de repas, entraînements, recettes, communauté |
| **Ro Body** | Télésanté 🏥 | ≈ 145 $/mois | Balance connectée, journal des doses, coaching par infirmière, médicaments contre les nausées, aide pour l'assurance, cursus |
| **Juniper** | Télésanté 🏥 | Programme | Balance (poids et tour de taille), analyses personnalisées, recettes, vidéos, accent sur musculation et protéines, communauté |
| **Mochi Health** | Télésanté 🏥 | Programme | Messagerie, suivi des ordonnances, renouvellements et livraisons, comparaison de pharmacies, diététistes |
| **Measured** | Télésanté 🏥 | Abonnement | Consultations en ligne, messagerie illimitée, analyses de laboratoire, communauté (fiche publique peu détaillée) |
| Autres 2025-2026 | Suivis | Variable | **Titrate** et **Regimen** (calculateur de reconstitution U-100, plusieurs composés, labos), **Semaya** (date d'ouverture du stylo ou de la fiole, chronologie des doses), **GLP-1 Journey Tracker**, **GlucoseTrack**, **Steady** (HbA1c, lipides, foie, rein, tension, import de capteur de glycémie), **Gaby** et **Back2Me** (cycle menstruel, payant chez Gaby), **Simple GLP** (Apple Watch), **Glowise** (voix, lecture de menu, menus de 7 jours), **GLPeak+** (repas par IA payants), **Havit** (coach IA, 29,99 $/mois), **Gala** (rappels de renouvellement), **Velto** (intervalle de 3 à 30 j), **Glia** (widgets d'écran verrouillé), **GLP1Suivi** (coût total), **Preserve** (musculation et protéines), **Jabby** (rappel reporté ou sauté) |

**À retenir :**
- Les murs payants des **suivis** portent surtout sur trois choses : l'**analyse** (niveau estimé, prévisions, rapports hebdomadaires, jalons, analyses avancées, PDF), le **corps** (photos, mensurations, composition) et l'**IA** (photo des repas, chat).
- Tout ce qui est analyse ou corps se fait très bien hors ligne : on peut l'offrir gratuitement et en faire mieux que les concurrents.
- Les programmes de télésanté vendent surtout des **humains** (médecin, coach, diététiste) et de la **logistique** (ordonnances, livraison). Ce n'est pas reproductible, mais leurs fonctions d'application (balance, labos, musculation, protéines, cursus) le sont.

---

## 2. Tableau de parité

### A. Doses et traitement

| # | Fonction | Applications | Gratuit / payant chez elles | Nous |
|---|---|---|---|---|
| A1 | Journal des doses (date, heure, dose, site, notes) | Toutes | ✅ partout (JabKeeper : 2 doses puis 💲) | ✅ |
| A2 | Comprimés (Rybelsus, Wegovy oral) | Shotsy (iOS seulement), Pep, Wiserapps, MeAgain | ✅ | ✅ |
| A3 | Rappel de dose avec bouton « C'est fait » | Toutes | ✅ | ✅ |
| A4 | Rappel « Plus tard » (report), veille de dose | Jabby (reporter ou sauter), JabKeeper (veille et jour J), Pep | ✅ / 💲 JabKeeper | 🟡 veille ✅, pas de « Plus tard » → **P01** |
| A5 | Intervalle libre (de 3 à 30 j), microdose | Velto, Shotsy, Noom Med | ✅ / 🏥 | ✅ |
| A6 | Médicament personnalisé (demi-vie) | Shotsy, Titrate | ✅ | ✅ |
| A7 | Plusieurs traitements en parallèle | Shotsy, Gilly, Regimen, Titrate | ✅ Shotsy · 💲 Gilly · 1 gratuit, 💲 illimité chez Regimen | ❌ → **P05** |
| A8 | Plusieurs molécules sur une même courbe | Shotsy | ✅ (courbe 💲) | ❌ → **P05** |
| A9 | Plan de titration et chronologie des paliers | Pep, Semaya, GLP-1 Journey Tracker, Gilly (« shot planning ») | ✅ / 💲 Gilly | ✅ |
| A10 | Aide en cas de dose oubliée | Pep (calculateur web) | ✅ | ✅ |
| A11 | Changer de jour d'injection (écart minimal) | Pep (calculateur web) | ✅ | 🟡 écart affiché, aucun assistant → **P02** |
| A12 | Historique des doses sautées ou en retard | Jabby, rapports Glapp | ✅ / 💲 Glapp Max | 🟡 une seule dose sautée mémorisée → **P02** |
| A13 | Courbe du niveau estimé | Shotsy+, Gilly, Glapp, MeAgain, GlucoPal, Pep | 💲 Shotsy, Gilly · ✅ les autres | ✅ |
| A14 | Prévision du niveau | GlucoPal Pro | 💲 | ✅ projection sur 7 j. Pour dépasser : simulateur → **P09** |
| A15 | « Phases » de la semaine (montée, pic, creux, faim prévue) | Glapp (6 phases) | ✅ | ❌ → **P07** |
| A16 | Rotation des sites et site suggéré | Toutes | ✅ | ✅ |
| A17 | Carte du corps (silhouette) | GlucoPal, Semaya | ✅ | 🟡 en cours (`BodyMap.kt`) |
| A18 | Minuteur « à jeun 30 min » après un comprimé | Aucune trouvée | — | ❌ pour dépasser → **P06** |
| A19 | Rappel de contraception orale (tirzépatide) | Aucune trouvée | — | ❌ pour dépasser → **P29** |

### B. Stylos, fioles, stock et coûts

| # | Fonction | Applications | Gratuit / payant chez elles | Nous |
|---|---|---|---|---|
| B1 | Doses restantes dans le stylo | Glapp | ✅ | ✅ |
| B2 | Date d'ouverture, « à jeter après X jours » (BUD) | Glapp, Semaya | ✅ | 🟡 affiché, aucune notification → **P01** |
| B3 | Stock et date de rupture | Glapp (calcul), Glapp Max (alerte de stock bas) | ✅ / 💲 | 🟡 calcul ✅, alerte ❌ → **P01** |
| B4 | Rappel de renouvellement d'ordonnance | Gala, Mochi | ✅ / 🏥 | 🟡 mention dans la carte, sans notification → **P01** |
| B5 | Fioles magistrales : mg ↔ unités U-100, reconstitution | Titrate, Regimen, Peptide Tracker, Pep AI | ✅ Titrate · 1 gratuit chez Regimen · 💲 Pep AI | ❌ → **P03** |
| B6 | Temps passé hors du frigo (21 j pour Zepbound…) | Aucune trouvée | — | ❌ pour dépasser → **P03** |
| B7 | Coût du traitement, relevé annuel (FSA/HSA, frais médicaux) | Gilly (FSA/HSA), GLP1Suivi | 💲 Gilly · ✅ GLP1Suivi | ❌ → **P04** |
| B8 | Suivi d'ordonnance, livraison, comparaison de pharmacies | Mochi, Ro | 🏥 | ❌ ⛔ (serveur requis) → fiche d'ordonnance locale **P04** |

### C. Poids et corps

| # | Fonction | Applications | Gratuit / payant chez elles | Nous |
|---|---|---|---|---|
| C1 | Poids, IMC, objectif, perte en kg et en % | Toutes | ✅ | ✅ |
| C2 | Poids coloré par dose, repères de changement de palier | Shotsy | ✅ ❓ | ❌ → **P11** |
| C3 | Tendance lissée (moyenne mobile) | Rapports de Glapp | 💲 | 🟡 moyenne hebdo seulement → **P11** |
| C4 | Mensurations | Pep, Wiserapps, Gilly | ✅ / 💲 Gilly | ✅ |
| C5 | Rapport taille/hauteur | Rare | — | ✅ |
| C6 | % de gras, masse maigre | Noom et WW (scan IA), Calibrate, Juniper, Ro (balance), Havit | 🏥 / 💲 | ❌ → **P14** |
| C7 | Photos de progrès et comparaison avant/après | Pep, GlucoPal, Gilly, MeAgain | ✅ Pep, GlucoPal · 💲 Gilly, MeAgain | ❌ → **P12** |
| C8 | Carte de progrès à partager (« Journey Card ») | MeAgain | 💲 | ❌ → **P13** |
| C9 | Jalons et badges | Glapp Max, MeAgain | 💲 | 🟡 en cours (`Milestones.kt`) |
| C10 | Prédiction de la date d'objectif (points à 3 et 6 mois) | Glapp Max, JabKeeper | 💲 | 🟡 en cours (`GoalForecast`). Points à 3 et 6 mois → **P16** |
| C11 | Séries visibles (jours consécutifs) | MeAgain | 💲 ❓ | 🟡 badges 7 j / 30 j, aucun compteur → **P13** |
| C12 | Comparaison aux essais cliniques | Glapp | ✅ | ❌ → **P10** |
| C13 | Comparaison aux autres utilisateurs | Glapp (compte, données anonymisées) | ✅ | ❌ ⛔ (serveur requis) → essais cliniques **P10** |
| C14 | Mode maintenance | Shotsy | ✅ ❓ | ❌ → **P15** |

### D. Nutrition

| # | Fonction | Applications | Gratuit / payant chez elles | Nous |
|---|---|---|---|---|
| D1 | Calories, protéines, fibres (saisie manuelle) | Shotsy, MeAgain, GlucoPal | ✅ | ✅ |
| D2 | Glucides, lipides, sucres, sodium | GlucoPal, Wiserapps, MyFitnessPal | ✅ / 💲 MFP (macros en grammes) | ❌ → **P17** |
| D3 | Favoris et saisie rapide | MeAgain | ✅ | ✅ |
| D4 | Recherche dans une base d'aliments | Pep (FatSecret), Wiserapps, GlucoPal, MeAgain, GLPeak | ✅ (en ligne) | ❌ → base locale **P18** |
| D5 | Code-barres | Pep, MeAgain, GlucoPal, GLPeak, MyFitnessPal | ✅ / 💲 MFP | ❌ → codes-barres appris **P19** (honnêteté : section 3) |
| D6 | Photo des repas analysée par IA | Pep Pro, GLPeak+, MeAgain, GlucoPal, Gala, Noom | 💲 Pep, GLPeak · ✅ ❓ MeAgain · 🏥 Noom | ❌ ⛔ → **P20** |
| D7 | Saisie vocale | MeAgain, Glowise, MyFitnessPal | ✅ / 💲 MFP | ❌ → **P21** |
| D8 | Lecture d'une étiquette ou d'un menu | Glowise (IA) | ❓ | ❌ → lecture d'étiquette sur l'appareil **P19** |
| D9 | Index et charge glycémiques | Wiserapps | ✅ | ❌ → **P22** (licence) |
| D10 | Recettes et plans de repas | Wiserapps, MyNetDiary, Glowise, Juniper, Found | ✅ / 💲 / 🏥 | ❌ → **P23** |
| D11 | Objectif de protéines personnalisé (g/kg) | Noom, MyNetDiary, Pep (calculateur) | 🏥 / 💲 / ✅ | 🟡 objectif fixe en g → **P24** |
| D12 | Eau, objectif et rappels | Toutes | ✅ | ✅ |
| D13 | Jeûne intermittent | MyNetDiary, Havit | 💲 | ❌ → **P25** |

### E. Journal, symptômes et bien-être

| # | Fonction | Applications | Gratuit / payant chez elles | Nous |
|---|---|---|---|---|
| E1 | Effets secondaires et intensité | Toutes | ✅ | ✅ (14 types, 3 niveaux) |
| E2 | Liste longue et symptômes personnalisés | MeAgain (29 + personnalisés, 0 à 10) | ✅ | 🟡 14 types + « Autre » → **P27** |
| E3 | Signaux d'alarme (pancréatite, vésicule, déshydratation) | Aucune trouvée | — | ❌ pour dépasser → **P27** |
| E4 | Effets et faim selon le jour depuis l'injection | JabKeeper, Shotsy, MeAgain, GLP Buddy | 💲 JabKeeper · ✅ ❓ les autres | ❌ → **P07** |
| E5 | Appétit, faim | Glapp, MeAgain | ✅ | ✅ |
| E6 | « Food noise » et fringales notés séparément | Glapp, MeAgain | ✅ | 🟡 une seule échelle → **P26** |
| E7 | Énergie, sommeil, selles | Glapp | ✅ | ❌ → **P26** |
| E8 | Humeur, notes libres | Glapp, plusieurs autres | ✅ | ✅ |
| E9 | Calendrier mensuel et chronologie unifiée | Wiserapps, MeAgain, Gaby | ✅ / 💲 Gaby | 🟡 navigation jour par jour → **P28** |
| E10 | Cycle menstruel | Gaby, Back2Me | 💲 Gaby · ❓ | ❌ → **P29** |
| E11 | Bilan hebdomadaire | Glapp Max | 💲 | ❌ → **P08** |

### F. Santé et activité

| # | Fonction | Applications | Gratuit / payant chez elles | Nous |
|---|---|---|---|---|
| F1 | Glycémie, HbA1c | GlucoseTrack, Steady, Journey Tracker, Regimen | ✅ ❓ | ❌ → **P30** |
| F2 | Tension artérielle | Journey Tracker, GlucoseTrack | ✅ ❓ | ❌ → **P30** |
| F3 | Bilans de labo (lipides, foie, rein) avec plages de référence | Journey Tracker, Steady, Regimen, Measured, Calibrate | ✅ ❓ / 🏥 | ❌ → **P30** |
| F4 | Import d'un capteur de glycémie (Libre, Dexcom) | GlucoseTrack | ❓ | ❌ → **P31** |
| F5 | Pas, activité | GlucoPal (Apple Health), MeAgain (widget), Wiserapps, Noom | ✅ / 💲 MeAgain / 🏥 | ❌ → **P32** |
| F6 | Sommeil | Wiserapps, WW, Calibrate, Glapp | ✅ / 🏥 | ❌ → **P26** + **P32** |
| F7 | Musculation, exercices guidés | Noom (« Muscle Defense »), WW (vidéos), Juniper, Preserve | 🏥 / 💲 | ❌ → **P33** |
| F8 | Rendez-vous médicaux et questions à poser | GLP-1 Journey Tracker (PDF) | ❓ | 🟡 questions dans le PDF en cours → **P34** |

### G. Intégrations et appareils

| # | Fonction | Applications | Gratuit / payant chez elles | Nous |
|---|---|---|---|---|
| G1 | Health Connect / Apple Health : lecture du poids | Shotsy+, Pep, MeAgain, GlucoPal | 💲 Shotsy · ✅ les autres | ✅ |
| G2 | Health Connect : écriture, et lecture des pas, du sommeil, du % de gras, de la glycémie et de la tension | Shotsy+, GlucoPal (Apple Health) | 💲 / ✅ | 🟡 poids en lecture seulement → **P32** |
| G3 | Balance Bluetooth | Calibrate, Ro, Juniper (balance fournie) | 🏥 | ✅ (Renpho, QN, profil standard) |
| G4 | Montre (Wear OS / Apple Watch) | Simple GLP | ❓ | ❌ → **P37** |
| G5 | Export vers l'agenda | JabKeeper | 💲 | ❌ → **P38** |
| G6 | Synchronisation entre appareils ou sur le web | Glapp (compte), GlucoPal et Glone (iCloud) | ✅ (avec compte) | 🟡 sauvegarde automatique dans un dossier · ⛔ aucun serveur → **P40** |
| G7 | Raccourcis d'application, tuile Réglages rapides | Aucune trouvée | — | ❌ pour dépasser → **P39** |

### R. Rapports et données

| # | Fonction | Applications | Gratuit / payant chez elles | Nous |
|---|---|---|---|---|
| R1 | Export CSV | GlucoPal, MyFitnessPal | ✅ / 💲 MFP | ✅ |
| R2 | Rapport PDF pour le médecin | Shotsy, GlucoPal Pro, Gilly, Wiserapps, Semaya, Journey Tracker | 💲 GlucoPal, Gilly, Wiserapps · ❓ Shotsy | 🟡 en cours (`report/`) |
| R3 | Graphiques avancés, analyses | Wiserapps, Gilly | 💲 | 🟡 → **P07**, **P08** |
| R4 | Historique illimité | Gilly (60 j en gratuit), Wiserapps (illimité en Premium) | 💲 | ✅ |
| R5 | Sauvegarde, restauration, sauvegarde automatique | Shotsy, Glapp (nuage) | ✅ (compte) | ✅ JSON + sauvegarde automatique quotidienne |
| R6 | Sauvegarde chiffrée, transfert vers un nouveau téléphone | Aucune trouvée hors nuage | — | ❌ pour dépasser → **P40** |

### X. Expérience

| # | Fonction | Applications | Gratuit / payant chez elles | Nous |
|---|---|---|---|---|
| X1 | Widget « prochaine dose » | Shotsy, MeAgain, Wiserapps | ✅ | ✅ (avec « C'est fait ») |
| X2 | Widget du niveau estimé | Shotsy | ✅ ❓ | ❌ → **P35** |
| X3 | Widget des habitudes (protéines, eau, fibres, pas) | MeAgain (mascotte) | 💲 | ❌ → **P35** |
| X4 | Écran verrouillé, activité en direct | Glia (iOS) | ❓ | ❌ → notification persistante **P36** |
| X5 | Verrouillage de l'application | Rare | — | ✅ (biométrie). Masquer le contenu des notifications ❌ → **P44** |
| X6 | Thèmes, Material You | Shotsy (thèmes) | ✅ | ✅ |
| X7 | Langues | Shotsy (17) | ✅ | 🟡 FR et EN → **P41** |
| X8 | Sans compte, hors ligne | JabKeeper, Wiserapps, Glone, Titrate | ✅ / 💲 JabKeeper | ✅, sans même la permission `INTERNET` |
| X9 | Sans publicité | Glapp Max | 💲 | ✅ |
| X10 | Mascotte, ludification | MeAgain (Capy) | 💲 | ❌ faible priorité → **P43** |
| X11 | Contenu éducatif (fiches, cursus, vidéos) | Pep AI, Juniper, Calibrate, Ro | 💲 / 🏥 | ❌ → **P42** |
| X12 | Compagnon IA (chat) | MeAgain, maxbud, Havit, MyNetDiary Premium Plus | 💲 | ❌ ⛔ → **P42** (+ Gemini Nano expérimental, section 3) |
| X13 | Coaching humain, diététiste, messagerie avec le médecin | Calibrate, Noom Med, Found, Ro, Juniper, Mochi, Measured | 🏥 | ❌ ⛔ → PDF, questions, rendez-vous **P34** |
| X14 | Communauté | WW, Found, Juniper | 🏥 / 💲 | ❌ ⛔ (aucune alternative hors ligne crédible) |

**Bilan :** sur environ 95 fonctions, à peu près 30 sont ✅ et 21 sont 🟡. Plusieurs sont payantes ailleurs : courbe du niveau, historique illimité, Health Connect, sans pub, et bientôt le PDF. Les ❌ se font hors ligne, sauf l'IA générative, les humains, la communauté et la synchronisation par serveur (⛔).

---

## 3. Ce qui est impossible hors ligne, et la meilleure alternative

| Fonction | Pourquoi c'est impossible (ou presque) | Meilleure alternative hors ligne |
|---|---|---|
| **Photo IA des repas** (D6) | Reconnaître un plat **et** estimer la portion demande un grand modèle de vision, sur un serveur chez tous les concurrents. Sur l'appareil, l'étiquetage d'images d'ML Kit ne reconnaît que des catégories générales, sans portion ni calories. Gemini Nano (API Prompt d'ML Kit GenAI) accepte des images, mais seulement sur quelques téléphones haut de gamme (Pixel 9 et 10, certains Samsung, Xiaomi, OnePlus et Honor). Le modèle est téléchargé par AICore, donc il faut le réseau au moins une fois. Il est en alpha et peu fiable pour les calories | **P20** : repères de portions à la main (paume, poing, pouce), lecture d'étiquette par OCR sur l'appareil (P19), repas favoris (déjà là) et base d'aliments locale (P18). Le mode Gemini Nano reste une expérience P2, désactivée par défaut et annoncée comme approximative |
| **Code-barres → valeurs nutritives** (D5) | Le **scan** lui-même fonctionne hors ligne (modèle ML Kit intégré à l'APK, ou ZXing). Mais le code ne contient aucune valeur nutritive. Il faut une base. Open Food Facts publie un export complet d'environ **0,9 Go compressé et 9 Go décompressé**, impossible à mettre dans un APK. Un sous-ensemble Canada et France resterait lourd, devrait être **téléchargé** (réseau), mis à jour (réseau) et respecter la licence ODbL (partage à l'identique). **Ce n'est donc pas « hors ligne », il faut le dire clairement** | **P19** : codes-barres **appris**. On scanne, et si le produit est inconnu, on photographie le tableau de valeurs nutritives. L'OCR sur l'appareil remplit la fiche, qui est gardée en local avec le code. Au 2ᵉ scan, l'ajout est instantané. Un « pack Open Food Facts » téléchargeable de plein gré reste possible plus tard (P2), comme seule exception réseau, expliquée et désactivée par défaut |
| **Chat IA / compagnon** (X12) | Un LLM utile demande un serveur. Gemini Nano sur l'appareil ne couvre que quelques modèles de téléphones, demande un téléchargement initial, et poserait un risque de conseil médical erroné | **P42** : bibliothèque de fiches hors ligne, cherchable et contextuelle (nausées, constipation, protéines, oubli de dose, voyage, chirurgie…). Analyses calculées et vérifiables (P07, P08). Bouton « Ajouter à mes questions pour le médecin ». En P2 et en option : un **résumé de la semaine en langage naturel** par Gemini Nano sur les appareils compatibles, jamais de conseil médical |
| **Coaching humain, diététiste, messagerie avec le médecin** (X13) | C'est un service humain payé, pas une fonction logicielle | Rapport PDF (en cours), questions à préparer, rendez-vous (P34), fiches (P42). On prépare la consultation au lieu de la remplacer |
| **Communauté** (X14) | Il faut un serveur et de la modération | Aucune, en restant cohérent. On peut mentionner dans « À propos » les communautés existantes (lien ouvert dans le navigateur, l'application ne fait toujours aucune requête) |
| **Comparaison avec les autres utilisateurs** (C13) | Données agrégées sur un serveur | **P10** : comparaison aux **courbes moyennes des essais cliniques publiés**. C'est plus rigoureux et sans collecte de données |
| **Synchronisation multi-appareils / web** (G6) | Il faut un serveur ou un compte infonuagique | Déjà là : sauvegarde automatique dans un dossier choisi par l'utilisateur (Drive, Nextcloud…, l'application fournisseur synchronise elle-même). **P40** ajoute le chiffrement et le transfert de fichier vers un nouveau téléphone par Quick Share (Bluetooth ou Wi-Fi direct, sans nuage) |
| **Suivi d'ordonnance, livraison, pharmacie** (B8) | Il faut les systèmes de la pharmacie | **P04** : fiche d'ordonnance locale (pharmacie, téléphone, n° d'ordonnance, renouvellements restants, date d'expiration), bouton « Appeler la pharmacie » (composeur, sans réseau) et rappels de renouvellement (P01) |

**Point de vigilance sur la vie privée (ML Kit) :** les SDK ML Kit envoient des statistiques d'usage et de diagnostic à Google (page « Android data disclosure » d'ML Kit), et la fusion des manifestes ajoute en général la permission `INTERNET`. Si on ajoute ML Kit (P19), il faut :
1. utiliser les modèles **intégrés** (« bundled »), pas ceux de Google Play Services, qui sont téléchargés avant le premier usage ;
2. retirer `INTERNET` et `ACCESS_NETWORK_STATE` du manifeste final avec `tools:node="remove"` ;
3. ajouter dans la CI une vérification qui échoue si le manifeste fusionné contient `INTERNET`.

Pour le code-barres seul, **ZXing** (`com.google.zxing:core`, Java pur, aucune télémétrie) évite complètement le problème.

---

## 4. Spécifications des éléments ❌ et 🟡

**Conventions communes**
- La base Room est en `version = 2`. On regroupe les changements de schéma d'une même version dans **une seule migration v2 → v3** : P02, P03, P04, P12, P17, P26, P27, P30. P05 (plusieurs traitements) aura sa propre **v3 → v4**. À coordonner avec les personnes qui travaillent sur le PDF, la carte du corps et les jalons.
- Chaque nouvelle table ou colonne va dans `Backup.toJson`/`fromJson` (champs facultatifs, valeur par défaut si absents), dans le CSV et dans le PDF quand c'est pertinent.
- La logique pure va dans `domain/` (sans import Android) avec des tests unitaires. Les textes vont en FR **et** en EN. Formulation médicale : des estimations, jamais des conseils.
- Les nouveaux rappels s'ajoutent à `ReminderType` (nouveau `requestCode`) et sont replanifiés dans `BootReceiver`.

### Rappels, stock et traitement

#### P01 : Alertes de stock, stylo à jeter, renouvellement et « Plus tard » (S), sans dépendance
- **UX :**
  - Nouvelles notifications (canal « Stock ») :
    - « Ton stylo est à jeter dans 3 jours (ouvert le 2 oct.) » ;
    - « Plus de dose dans ce stylo : ouvre un nouveau stylo ? », avec une action **Nouveau stylo** qui appelle `refillPen` ;
    - « Il te reste environ 2 semaines de traitement : pense à renouveler ton ordonnance ».
  - La notification de dose gagne une action **Plus tard** (dans 1 h ou à 20 h, au choix dans les réglages).
  - Réglages → Rappels : « Alertes de stock » et « Prévenir X jours avant la rupture » (10 par défaut).
- **Données :** DataStore `supplyAlerts: Boolean`, `refillLeadDays: Int = 10`, `snoozeMinutes: Int = 60`, `lastSupplyAlertKey: String` (pour éviter les doublons). `ReminderType.SUPPLY(14)` vérifie chaque jour à 9 h avec `Supply.discardDate` et `Supply.runsOutOn`, déjà écrits. `ReminderType.SNOOZE(15)` est une alarme unique.
- **Hors ligne :** 100 %.

#### P02 : Doses sautées dans l'historique et assistant « Changer mon jour » (S/M), sans dépendance
- **UX :**
  - « Je saute cette dose » crée une ligne grisée « Dose sautée » dans la liste des doses. Elle compte dans l'observance du PDF et dans le bilan hebdomadaire.
  - Une dose prise après l'heure prévue est marquée « en retard de 26 h ».
  - **Changer mon jour d'injection** : on choisit un nouveau jour. L'application vérifie `MissedDose.minGapHours` depuis la dernière dose et propose la première date possible (« Jeudi 9 h au plus tôt : 72 h après ta dernière dose »), puis met à jour `injectionDay` et replanifie.
- **Données :** `injections.skipped INTEGER NOT NULL DEFAULT 0`. Une ligne sautée a `doseMg = 0`, n'entre pas dans `Pharmacokinetics` et ne décompte pas le stylo. Le retard se calcule à partir de `Schedule`. Le réglage `skippedDueEpochDay` reste en place pour la compatibilité.

#### P03 : Fioles magistrales, unités U-100, reconstitution et temps hors du frigo (M), sans dépendance
- **UX :**
  - **J'ouvre un nouveau stylo ou une nouvelle fiole** : type (stylo multidose, stylo unidose, fiole), dosage, et pour une fiole la concentration en mg/mL ou les « mg dans la fiole + mL d'eau bactériostatique ajoutés ».
  - **Calculatrice** : « 2,5 mg = 0,50 mL = **50 unités** sur une seringue U-100 (fiole de 10 mg + 2 mL = 5 mg/mL) ». Un dessin de seringue graduée montre où arrêter le piston.
  - **Garde-fous :**
    - avertissement rouge si le volume dépasse la seringue choisie (30, 50 ou 100 unités) ou si la dose est plus du double de la précédente ;
    - double saisie de la concentration ;
    - mention permanente « Suis toujours l'étiquette de ta pharmacie ».
  - L'éditeur de dose accepte la saisie en mg **ou** en unités et affiche l'autre valeur.
  - **Hors du frigo** : bouton « Sorti du frigo le… », puis « Encore 18 j à température ambiante (max. 21 j sous 30 °C) ». Une notification part 2 jours avant la limite.
  - La fiole affiche aussi « volume restant ≈ 1,2 mL » et sa date limite d'utilisation (BUD) inscrite par la pharmacie.
- **Données :** nouvelle table `supplies(id, medication, kind: PEN_MULTI/PEN_SINGLE/VIAL/TABLETS, strengthMg, concentrationMgPerMl?, volumeMl?, dosesTotal?, openedAt?, discardAfterDays, outOfFridgeAt?, maxRoomTempDays?, budDate?, costCents?, finishedAt?)`. Les réglages `pen*` et `stockPens` de DataStore migrent vers une ligne `supplies`. Ajouter `injections.supplyId`, `injections.units REAL` et `injections.volumeMl REAL`. Les valeurs par défaut de `maxRoomTempDays` par médicament sont à **revérifier dans les monographies canadiennes** : Mounjaro/Zepbound 21 j, Wegovy unidose 28 j, Trulicity 14 j, Ozempic, Saxenda et Victoza selon `Supply.defaultDiscardDays`.
- **Domaine :** `domain/VialMath.kt` (conversions mg ↔ mL ↔ unités, validations) avec beaucoup de tests (arrondis, unités nulles, valeurs négatives).

#### P04 : Coûts, assurance, relevé annuel et fiche d'ordonnance (S/M), sans dépendance
- **UX :**
  - Coût par stylo ou fiole, et part remboursée (RAMQ, assurance privée ou carte de rabais).
  - Carte « Coût » : ce mois-ci, depuis le début, coût par semaine de traitement.
  - **Relevé annuel** (PDF et CSV) : date, médicament, pharmacie, montant payé et montant remboursé. Utile pour les frais médicaux aux impôts au Canada ou au Québec, ou pour les comptes FSA/HSA aux États-Unis. Mention « Ceci n'est pas un conseil fiscal ».
  - **Fiche d'ordonnance** : pharmacie, téléphone (bouton **Appeler**, `ACTION_DIAL`), n° d'ordonnance, renouvellements restants, date d'expiration. Elle alimente les rappels de P01.
- **Données :** `supplies.costCents` et `supplies.reimbursedCents` (P03). Nouvelle table `prescriptions(id, medication, pharmacy, phone, rxNumber, refillsLeft, expiresOn, notes)`. DataStore `currency = "CAD"`.

#### P05 : Plusieurs traitements en parallèle (L), sans dépendance
- **Pourquoi :** Shotsy (gratuit) et Gilly (payant) le proposent. Cas réels : passage d'Ozempic à Mounjaro, GLP-1 + metformine, peptide magistral en plus.
- **UX :**
  - Écran **Mes traitements**. Chaque traitement a son médicament, son horaire, ses rappels, son plan de titration et son stock. On peut l'archiver tout en gardant l'historique.
  - L'accueil affiche une carte « Prochaine dose » par traitement actif.
  - La courbe du niveau montre une série par molécule, chacune en **% de son propre pic à l'état d'équilibre**, puisque les mg de deux molécules ne se comparent pas.
  - Le PDF et le CSV indiquent le traitement de chaque dose.
- **Données (migration v3 → v4) :** table `treatments(id, medication, customName, customHalfLifeH, doseMg, intervalDays, injectionDay, reminderHour, reminderMinute, remindersEnabled, titrationPlan, titrationStartDay, startedAt, endedAt?, colorIndex)`. Ajouter `injections.treatmentId` et `supplies.treatmentId`. La migration crée le traitement n° 1 à partir de `UserSettings`. Refonte de `Schedule`, de `ReminderScheduler` (`requestCode = 1000 + treatmentId`), du widget et de `MissedDose` (tout passe par le traitement).

#### P06 : Minuteur « à jeun » après un comprimé (S), sans dépendance
- **UX :** après avoir noté une dose de Rybelsus ou de Wegovy oral, une notification compte à rebours affiche « Attends avant de manger, de boire ou de prendre d'autres médicaments : 30 min » (`setUsesChronometer` + `setChronometerCountDown`). Le rappel du matin dit « À jeun, avec au plus 120 mL d'eau ». Ces consignes sont à **revérifier dans la monographie de chaque comprimé**.
- **Données :** aucune (`Medication.isOral` existe), plus un réglage `oralTimer: Boolean = true`.

### Analyses

#### P07 : « Phases » de la semaine et effets selon le jour depuis l'injection (M), sans dépendance
- **Pourquoi :** Glapp offre ses 6 phases gratuitement, JabKeeper fait payer les effets liés au jour de la dose, Gilly et Wiserapps font payer les « analyses avancées ». C'est notre meilleure carte : la courbe pharmacocinétique existe déjà.
- **UX :**
  - **Accueil** : « Jour 2 après l'injection · pic estimé ». Sous l'en-tête, une phrase **tirée de tes données** quand il y en a assez : « D'habitude, ton appétit est au plus bas les jours 1 et 2 ».
  - **Onglet Progrès → Mon cycle** :
    - une **carte de chaleur** J0 à J6 (ou de 0 à l'intervalle − 1) × symptômes (fréquence) ;
    - les moyennes d'appétit, de food noise, d'énergie et d'humeur par jour du cycle ;
    - la courbe du niveau moyen superposée.
  - Une analyse ne s'affiche qu'à partir de **3 cycles** de données. Sinon : « Encore 2 semaines de journal pour voir tes tendances ».
  - Pour les médicaments quotidiens, les phases sont masquées (amplitude trop faible).
- **Domaine :** `domain/CycleInsights.kt` (pur). Entrées : doses (timestamps et paramètres pharmacocinétiques), journaux quotidiens, effets secondaires. Sorties :
  - `dayIndex(date)` ;
  - `phase(now)` : MONTÉE avant le pic, PIC à au moins 90 % du maximum du cycle, DÉCLIN, CREUX dans les 20 % les plus bas de l'amplitude ;
  - agrégats par jour, et comparaisons « × 3 plus fréquent » seulement si l'écart est net (au moins 2 fois et n ≥ 4).
- **Données :** aucune table. Profite de P26 (énergie, food noise).

#### P08 : Bilan hebdomadaire (M), sans dépendance
- **Pourquoi :** payant chez Glapp Max, et ses utilisateurs le jugent trop rigide (voir la plainte n° 5 de ROADMAP).
- **UX :**
  - Notification le dimanche à 18 h (jour et heure réglables) : « Ta semaine : −0,6 kg (tendance), 1/1 dose, eau 5/7 jours ».
  - **Carte Bilan** en tête de l'accueil pendant 48 h, et **historique** des bilans dans Progrès (calculés à la volée).
  - Contenu :
    - doses (prises, en retard, sautées) ;
    - poids selon la tendance lissée de P11, ce qui tolère une semaine sans pesée ;
    - les 3 effets les plus fréquents et leur jour dans le cycle ;
    - eau, protéines et fibres (jours où l'objectif est atteint) ;
    - badges débloqués ;
    - aperçu de la semaine à venir : dose, changement de palier, stylo à jeter, rendez-vous.
  - Bouton **Partager** (image, comme P13).
- **Domaine :** `domain/WeeklyReport.kt` (pur), avec une période glissante de 7 jours qui ne plante jamais si des données manquent. `ReminderType.WEEKLY(16)`.

#### P09 : Simulateur « Et si… » du niveau (S/M), sans dépendance, pour dépasser GlucoPal Pro
- **UX :** sur la courbe du niveau, un bouton **Et si…** propose « Je saute la prochaine dose », « Je décale de N jours » ou « Je passe à X mg ». Une courbe projetée en pointillés apparaît, avec le texte « Visualisation théorique, ne change rien à ton traitement sans ton médecin ».
- **Données :** aucune. Doses hypothétiques passées à `Pharmacokinetics.curve`.

#### P10 : Comparaison aux essais cliniques (S/M), sans dépendance
- **UX :**
  - Progrès → Poids : interrupteur « Comparer à l'essai clinique ». Une courbe en pointillés montre la perte moyenne en % de l'essai de référence de la molécule, alignée sur la semaine de ta première dose.
  - Texte : « Semaine 20 : tu es à −11 % · moyenne de l'essai à cette semaine : ≈ −10 % ».
  - Avertissement permanent : « Moyenne d'un groupe dans des conditions d'essai (programme de mode de vie, doses du protocole). Les résultats individuels varient beaucoup ».
  - Masqué pour le diabète de type 2 : les essais sur le diabète montrent des pertes plus faibles et une autre courbe est requise.
- **Domaine :** `domain/TrialBenchmarks.kt`, avec une liste `(molécule, dose, essai, référence, points [(semaine, % moyen)])`. Valeurs finales à **revérifier dans les publications avant publication** :
  - STEP 1 (sémaglutide 2,4 mg) : ≈ −14,9 % à 68 semaines ;
  - SURMOUNT-1 (tirzépatide 5/10/15 mg) : ≈ −15,0 / −19,5 / −20,9 % à 72 semaines ;
  - SURMOUNT-5 (tirzépatide vs sémaglutide) : ≈ −20,2 % vs −13,7 % à 72 semaines ;
  - SCALE (liraglutide 3,0 mg) : ≈ −8,0 % à 56 semaines ;
  - OASIS 4 (sémaglutide oral 25 mg) : ≈ −13,6 % à 64 semaines ;
  - STEP UP (sémaglutide 7,2 mg) : ≈ −19 à −21 % à 72 semaines.

  Les points intermédiaires (semaines 4, 8, 12, 20, 28, 36, 52…) sont à relever à la main dans les figures publiées.

### Poids et corps

#### P11 : Repères de dose sur les graphiques et tendance lissée (S), sans dépendance
- **UX :**
  - Graphique du poids : chaque segment prend la couleur du palier de dose en cours, avec une légende « 2,5 · 5 · 7,5 mg ». Une ligne verticale en tirets marque chaque changement de dose. Même chose sur la courbe du niveau.
  - **Tendance** (moyenne mobile exponentielle, facteur 0,1 par jour) : une ligne lisse par-dessus les points. La carte affiche « Tendance : 84,2 kg (−0,5 kg/sem.) ».
- **Données :** aucune. `LineChart` reçoit `markers: List<Pair<Long, String>>` et `segmentColors`. `domain/Trend.kt` est pur.

#### P12 : Photos de progrès privées et comparaison (M), sans dépendance au départ
- **UX :**
  - Progrès → **Photos** : prise de vue de face, de profil ou de dos (`ActivityResultContracts.TakePicture` vers un fichier interne) ou import par le sélecteur de photos système (`PickVisualMedia`, hors ligne).
  - Grille par date, avec le poids du jour en filigrane.
  - **Comparaison** : deux dates avec un curseur glissant (avant/après) ou côte à côte.
  - Les photos ne vont **jamais** dans la galerie. Elles sont protégées par le verrouillage de l'application.
  - V2 en option : silhouette « fantôme » de la photo précédente pour garder le même cadrage, avec CameraX (`androidx.camera:camera-camera2`, `camera-lifecycle`, `camera-view`).
- **Données :** table `photos(id, timestamp, pose: FRONT/SIDE/BACK/OTHER, fileName, notes)`. Fichiers dans `filesDir/photos/`, réduits à 1600 px avec `ImageDecoder`. Le JSON ne contient que les métadonnées. Nouveau **« Sauvegarde complète (.zip) »** (JSON + photos, `java.util.zip`). La suppression d'une photo efface aussi le fichier.

#### P13 : Carte de progrès à partager et séries (S), sans dépendance
- **UX :**
  - **Carte de progrès** : une image au format portrait avec « −12,4 kg · 16 semaines », un badge, et en option une photo avant/après (P12). Interrupteur « Masquer mes kilos (afficher seulement %) ». Partage par la feuille système.
  - **Séries** : des pastilles « 🔥 12 j de journal », « 💧 8 j d'objectif d'eau », « 💉 10 doses à l'heure ».
- **Technique :** capture d'un composable avec `rememberGraphicsLayer()` puis `toImageBitmap()` (Compose 1.7, déjà dans le BOM). PNG dans `cacheDir`, partagé par le `FileProvider` du rapport PDF. Les séries sont calculées dans `domain/Milestones.kt`.

#### P14 : Composition corporelle (M), sans dépendance
- **UX :**
  - Mensurations : « % de gras estimé » et « masse maigre ». Graphique : **part de la perte qui vient de la masse maigre** (« 78 % de ta perte est du gras »). C'est le meilleur argument pour les protéines et la musculation.
  - Sources, au choix :
    - une balance Bluetooth au profil standard Body Composition (service 0x181B, caractéristique 0x2A9C) ;
    - Health Connect (`BodyFatRecord`, `LeanBodyMassRecord`, écrits par l'application Renpho, Withings…) ;
    - la **méthode US Navy** à partir de la taille, du cou et des hanches (formule publique de Hodgdon et Beckett), affichée comme « estimation ».
  - Le profil gagne un champ « Sexe pour les formules » (facultatif).
- **Données :** colonnes `measurements.bodyFatPct` et `measurements.leanMassKg`, plus une `source`. Le décodage 0x2A9C va dans `ScaleProtocols.kt` (pur, testé). Les formules d'impédance des balances QN/Renpho sont propriétaires : on ne copie pas le code GPL d'openScale.

#### P15 : Mode maintenance (S/M), sans dépendance
- **UX :**
  - Réglages → Traitement → **Phase** : Perte de poids ou Maintenance. En maintenance :
    - la carte Poids montre une **plage** (cible ± 2 kg, réglable) au lieu de « reste à perdre » ;
    - le graphique ombre cette plage ;
    - une alerte douce s'affiche si la tendance (P11) en sort pendant 14 jours ;
    - de nouveaux badges arrivent (« 8 semaines dans ta plage ») ;
    - l'intervalle peut être allongé (10 ou 14 jours) ;
    - le bilan hebdomadaire et le PDF parlent de stabilité.
- **Données :** DataStore `phase = LOSS|MAINTENANCE`, `maintenanceTargetKg`, `maintenanceBandKg = 2.0`, `maintenanceSince`.

#### P16 : Compléter les jalons et la prévision en cours : points à 3 et 6 mois (S), sans dépendance
- **UX :** sous la date d'objectif prévue : « À ce rythme : ≈ −6 kg dans 3 mois, ≈ −10 kg dans 6 mois (fourchette) ». La fourchette vient de l'erreur de la régression déjà calculée dans `GoalForecast`.
- **Données :** aucune. À intégrer **après** que le travail en cours sur `Milestones.kt` et `MilestonesCard.kt` est commité.

### Nutrition

#### P17 : Macros complets (S), sans dépendance
- **UX :** l'éditeur de repas gagne les champs glucides, lipides, sucres et sodium, facultatifs et repliés par défaut. Objectifs facultatifs. La carte affiche ces jauges seulement si un objectif est défini.
- **Données :** colonnes `food_entries.carbsG REAL`, `fatG REAL`, `sugarsG REAL`, `sodiumMg REAL` (nullables). DataStore `carbGoalG` et `fatGoalG` (0 = aucun).

#### P18 : Base d'aliments locale, en français et en anglais (L), sans dépendance
- **Source :**
  - **Fichier canadien sur les éléments nutritifs (FCÉN)** de Santé Canada : **5 993 aliments** (édition 2026), noms **en français et en anglais**, mesures domestiques (« 1 tasse », « 1 tranche »), sous la *Licence du gouvernement ouvert – Canada* (attribution).
  - En complément, si besoin : CIQUAL de l'ANSES (≈ 3 000 aliments, Licence Ouverte Etalab) ou USDA SR Legacy (≈ 7 800 aliments, domaine public).
  - Les nombres et les licences sont **à revérifier** au moment de l'import.
- **Construction :** un script `tools/build_food_db.py` (exécuté par le développeur, pas dans l'application) transforme les CSV en `assets/foods.db`. Prévoir environ 2 à 4 Mo, avec seulement kcal, protéines, lipides, glucides, fibres, sucres et sodium pour 100 g, plus les portions.
- **UX :**
  - Dans l'éditeur de repas, un champ **Rechercher un aliment** : « pomme », « yogourt grec », « toast ». La recherche ignore les accents. Les récents et les favoris s'affichent en premier.
  - On choisit une portion (« 1 tasse, 245 g ») et une quantité, et les valeurs se calculent.
  - Mention dans « À propos » : « Contient des données du Fichier canadien sur les éléments nutritifs, Santé Canada ».
- **Données :** une **base Room séparée** en lecture seule (`Room.databaseBuilder(...).createFromAsset("foods.db")`), pour que les migrations de la base utilisateur n'y touchent jamais. Tables `foods(id, nameFr, nameEn, groupId, kcal, protein, fat, carbs, fibre, sugars, sodium)`, `portions(foodId, labelFr, labelEn, grams)` et `foods_fts` (`@Fts4` avec le tokenizer `unicode61` et `remove_diacritics`). Le journal garde une **copie** des valeurs dans `food_entries` (l'historique reste stable si la base change).

#### P19 : Codes-barres appris et lecture d'étiquette sur l'appareil (M), nouvelles dépendances
- **UX :**
  - Bouton **Scanner** dans l'éditeur de repas. Si le code est déjà connu, le produit est ajouté en un toucher.
  - Sinon : « Produit inconnu : photographie le tableau de la valeur nutritive ». L'OCR lit l'étiquette bilingue canadienne (« Calories 220 », « Lipides / Fat 8 g », « Glucides / Carbohydrate », « Fibres / Fibre », « Protéines / Protein », « pour 1 tasse (250 mL) ») et préremplit une fiche **modifiable**, enregistrée avec le code.
  - Sans caméra : saisie manuelle avec les mêmes champs.
- **Dépendances :**
  - CameraX (`androidx.camera:*`) ;
  - code-barres avec **ZXing** (`com.google.zxing:core`, sans télémétrie) ou ML Kit `barcode-scanning` en modèle intégré ;
  - OCR avec ML Kit `text-recognition` en **modèle intégré** (latin).
  - Avec ML Kit, appliquer les 3 règles de la section 3 (retirer `INTERNET`, vérification dans la CI).
- **Données :** table `custom_foods(id, barcode TEXT UNIQUE NULL, name, servingLabel, servingG?, kcal, protein, fat, carbs, fibre, sugars, sodium, createdAt)`, cherchable avec P18. L'analyse du texte OCR va dans `domain/NutritionLabelParser.kt` (pur, testé sur des exemples d'étiquettes FR/EN).

#### P20 : Repères de portions, alternative à la photo IA (S), sans dépendance
- **UX :** dans l'éditeur de repas, un onglet **Estimer à l'œil** avec la méthode de la main : 1 paume de viande ou poisson, 1 poing de féculents, 1 main en coupe de fruits, 1 pouce de gras. Chaque toucher ajoute une valeur moyenne (par exemple « 1 paume de poulet ≈ 25 g de protéines, 140 kcal », valeurs tirées du FCÉN). Le résultat est marqué « estimation ».
- **Données :** table de repères dans le code (`domain/PortionGuide.kt`). Les entrées sont des `food_entries` ordinaires.

#### P21 : Saisie vocale hors ligne (M), sans dépendance, nécessite P18
- **UX :** micro dans l'éditeur. On dit « deux œufs et une rôtie au beurre d'arachide », et l'application propose les aliments trouvés dans P18 avec les quantités. L'utilisateur confirme.
- **Technique :** `SpeechRecognizer.createOnDeviceSpeechRecognizer` (API 31+), ou `RecognizerIntent` avec `EXTRA_PREFER_OFFLINE`. Le bouton n'apparaît que si `isOnDeviceRecognitionAvailable` est vrai (pack de langue FR-CA ou EN installé). Un analyseur simple des nombres et des mots va dans `domain/`.

#### P22 : Index glycémique (S, surtout du contenu), sans dépendance, faible priorité
- **Honnêteté :** les tables internationales d'IG (Université de Sydney) ne sont **pas sous licence libre**, on ne peut pas les copier. **Alternative :** une étiquette qualitative « IG bas, moyen ou élevé » rédigée à la main pour environ 50 aliments de base, à partir de guides publics (droits à vérifier), et l'affichage du ratio fibres/glucides de P17/P18.

#### P23 : Idées de repas et recettes hors ligne (M, surtout du contenu), sans dépendance
- **UX :** une trentaine de recettes **riches en protéines, en petites portions, faciles à digérer** (pour les jours de nausée), en FR et en EN, avec les valeurs calculées à partir du FCÉN. Bouton **Ajouter au journal** et filtres « Nausée », « Rapide », « +30 g de protéines ».
- **Données :** `assets/recipes.json`. Il faut écrire nos propres recettes (aucune copie).

#### P24 : Objectif de protéines en g/kg (S), sans dépendance
- **UX :** Réglages → Objectifs → Protéines : « Fixe (g) » ou « Selon mon poids : 1,2 g/kg (de 1,0 à 1,6) × poids visé 75 kg = **90 g** ». Une courte explication sur la préservation musculaire est affichée.
- **Données :** DataStore `proteinMode = FIXED|PER_KG`, `proteinPerKg = 1.2`. L'objectif effectif est calculé dans `domain/`.

#### P25 : Jeûne intermittent (S), sans dépendance, faible priorité
- **UX :** bouton **Commencer un jeûne** avec les préréglages 12, 14 et 16 h, et une notification chronomètre en cours. Historique dans le journal. Avertissement : « Avec un GLP-1, tu manges déjà peu : parles-en à ton médecin, surtout avec de l'insuline ou un sulfonylurée (risque d'hypoglycémie) ».
- **Données :** table `fasts(id, startAt, endAt?, targetHours)`.

### Journal

#### P26 : Journal enrichi (S/M), sans dépendance
- **UX :** la carte « Comment te sens-tu ? » gagne une section **Plus de détails**, repliable :
  - **Énergie** de 1 à 5 ;
  - **Sommeil** en heures et qualité de 1 à 5 ;
  - **Selles** : nombre et type sur l'échelle de Bristol (7 petits dessins) ;
  - **Fringales** de 1 à 5 ;
  - **Food noise** de 1 à 5, distinct de l'appétit ;
  - case **Musculation aujourd'hui** ;
  - **Pas**, saisis à la main ou par Health Connect (P32).

  Tout est facultatif, en un toucher par ligne.
- **Données :** colonnes `daily_logs.energy INTEGER DEFAULT 0`, `sleepMinutes INTEGER`, `sleepQuality INTEGER DEFAULT 0`, `bowelCount INTEGER`, `bristol INTEGER DEFAULT 0`, `cravings INTEGER DEFAULT 0`, `foodNoise INTEGER DEFAULT 0`, `strength INTEGER DEFAULT 0`, `steps INTEGER`. Moyennes dans le PDF et dans P07.

#### P27 : Symptômes étendus, personnalisés et signaux d'alarme (S/M), sans dépendance
- **UX :**
  - Environ 12 types de plus dans `SideEffectType` : reflux, rots soufrés, gaz, déshydratation, insomnie, anxiété, humeur basse, bouche sèche, goût modifié, frilosité, faiblesse musculaire, peau sensible, palpitations, hypoglycémie.
  - **Mes symptômes** : on peut créer ses propres types.
  - **Signaux d'alarme** :
    - douleur au ventre intense qui irradie dans le dos ;
    - douleur en haut à droite avec fièvre ou jaunisse ;
    - vomissements depuis plus de 24 h ou incapacité à garder les liquides ;
    - perte de vision soudaine ;
    - enflure du visage ou de la gorge ;
    - signes d'hypoglycémie.

    Choisir l'un de ces signaux, ou mettre « sévère » sur un symptôme apparenté, affiche un bandeau « **Consulte rapidement** : Info-Santé 811 ou 911 en cas d'urgence ». Formulation à faire relire par un professionnel.
- **Données :** nouvelles valeurs d'enum (aucune migration). Table `custom_symptoms(id, name, archived)` et `side_effects.type = "CUSTOM:<id>"`. Indicateur `urgent` dans l'enum.

#### P28 : Calendrier mensuel unifié (M), sans dépendance
- **UX :** onglet Journal → **Calendrier**. Une grille du mois avec des icônes par jour : 💉 dose (prise, en retard ou sautée), ⚖️ pesée, pastille de symptôme colorée selon l'intensité maximale, objectif d'eau atteint, et rendez-vous (P34). Les jours **futurs** montrent les doses prévues et les changements de palier. Un toucher ouvre le résumé du jour, puis l'éditeur.
- **Données :** aucune (calcul à partir des tables existantes).

#### P29 : Cycle menstruel et rappel de contraception orale (M), sans dépendance
- **UX :**
  - Activé seulement sur demande (« Suivre mon cycle »). On note le début et la fin des règles et le flux. Prochaines règles estimées (médiane des 6 derniers cycles). Le calendrier (P28) et P07 peuvent comparer les symptômes selon la phase du cycle (au moins 3 cycles).
  - **Tirzépatide + contraceptif oral** (case dans le profil) : un bandeau pendant les 4 semaines après la première dose et après **chaque augmentation de dose**, détectées à partir des injections. Texte : « Selon la monographie, ajoute une méthode barrière ou utilise une contraception non orale pendant 4 semaines. Parles-en à ton pharmacien ». À revérifier dans la monographie canadienne.
- **Données :** table `periods(id, startDate, endDate?, flow?)`. DataStore `cycleTracking` et `oralContraceptive`.

### Santé et activité

#### P30 : Signes vitaux et bilans de laboratoire (M), sans dépendance
- **UX :**
  - Progrès → **Santé**, avec des cartes :
    - glycémie (à jeun ou après un repas) et HbA1c (%) ;
    - tension (systolique, diastolique, pouls) ;
    - lipides (cholestérol total, LDL, HDL, triglycérides) ;
    - ALT, AST, DFGe, créatinine, lipase, TSH ;
    - « Autre ».
  - Unités en mmol/L par défaut (Canada), mg/dL en option. Plages de référence **saisies d'après le rapport de ton laboratoire** (préremplies avec des valeurs indicatives et marquées comme telles).
  - Graphique par mesure et section dans le PDF.
- **Données :** table `vitals(id, timestamp, type, value, value2?, unit, context?, refLow?, refHigh?, notes)`. Conversions dans `domain/Units.kt` (glucose ×18,016, cholestérol ×38,67, triglycérides ×88,57).

#### P31 : Import d'un capteur de glycémie (M), sans dépendance
- **UX :** **Importer un export LibreView ou Dexcom Clarity (CSV)** par le sélecteur de fichiers. On affiche la moyenne quotidienne, le temps dans la cible (3,9 à 10,0 mmol/L) et le GMI (3,31 + 0,02392 × moyenne en mg/dL), avec la mention « indicateur estimé, pas une HbA1c de laboratoire ». Lecture de `BloodGlucoseRecord` dans Health Connect (P32), par exemple depuis xDrip+.
- **Données :** table `glucose_readings(timestamp, mgdl, source)` (environ 26 000 lignes pour 90 jours à une mesure toutes les 5 min). L'analyse des CSV va dans `domain/` et est testée.

#### P32 : Health Connect élargi (M), sans nouvelle dépendance
- **UX :** Réglages → Connexions → Health Connect, avec un interrupteur **par type de donnée** :
  - **Lire** : pas (total du jour), sommeil, % de gras, masse maigre, glycémie, tension, séances d'exercice ;
  - **Écrire** : poids (manuel et balance Bluetooth), hydratation, nutrition (kcal, protéines, fibres).

  La permission est demandée au moment où on active chaque interrupteur.
- **Technique :** `aggregate(AggregateRequest(StepsRecord.COUNT_TOTAL, …))` par jour. Pour l'écriture, `clientRecordId = "glp1-<table>-<id>"` rend la mise à jour et la suppression idempotentes. On garde le filtre `dataOrigin` qui ignore nos propres écritures à l'import. Il faut déclarer les permissions dans le manifeste et dans `PermissionsRationaleActivity`.
- **Données :** `daily_logs.steps` et `sleepMinutes` (P26), `measurements.bodyFatPct` (P14), `vitals` (P30), plus des interrupteurs dans DataStore.

#### P33 : Musculation et préservation musculaire (S/M), sans dépendance
- **UX :** objectif « 2 séances par semaine » à partir de la case de P26, compteur dans le bilan hebdomadaire et badge « 4 semaines de musculation ». Une **routine illustrée hors ligne** de 6 exercices au poids du corps (texte et dessins simples, aucune vidéo), avec un minuteur de repos. Lien avec P14 : « part de la perte en masse maigre ».
- **Données :** `daily_logs.strength` (P26). Table facultative `workouts(id, date, kind, minutes, notes)`.

#### P34 : Rendez-vous médicaux et questions (S), sans dépendance
- **UX :**
  - Liste des rendez-vous avec un rappel la veille. Carte « Prochain rendez-vous dans 5 j ». Liste **Mes questions**, alimentée de partout (fiches P42, symptômes) et imprimée dans le PDF.
  - Le PDF propose la période « depuis le dernier rendez-vous ».
  - Après le rendez-vous : « Ta dose a changé ? », qui ouvre le plan de titration.
- **Données :** tables `appointments(id, dateTime, kind, title, location, notes)` et `questions(id, text, createdAt, answeredAt?)`.

### Appareils et expérience

#### P35 : Widgets du niveau et des habitudes (S/M), sans nouvelle dépendance (Glance existe)
- **Widget Niveau :** « ≈ 72 % du pic · en déclin », avec une mini-courbe dessinée dans un `Bitmap` (Canvas Android) puis affichée par `Image(ImageProvider(bitmap))`. Mis à jour par `WidgetUpdater` et toutes les 3 h.
- **Widget Habitudes :** barres eau, protéines et fibres, avec un bouton **+250 mL** (`actionRunCallback` qui écrit dans le dépôt).

#### P36 : Notification persistante, l'équivalent Android des activités en direct (S), sans dépendance
- **UX :** en option, le jour de la dose, une notification silencieuse et persistante « Dose aujourd'hui · cuisse G » avec **C'est fait**. Elle sert aussi au minuteur de P06 et au jeûne (P25). Sur Android 16, utiliser les notifications « Live Updates » (`ProgressStyle` mis en avant) si elles sont disponibles, ce qui est à vérifier avec `compileSdk 36`. Sinon, notification standard.

#### P37 : Wear OS : tuile et complication (L), dépendances Google et androidx
- **UX :** une tuile « Prochaine dose dans 2 j · cuisse G » avec **C'est fait**, une complication pour le cadran, et une vibration au moment du rappel.
- **Technique :** module `wear/` avec `androidx.wear.tiles`, `androidx.wear.protolayout`, `androidx.wear.watchface.complications.datasource`. La communication passe par la Data Layer de `play-services-wearable` (Bluetooth local, sans Internet). Un APK séparé est publié dans la même Release.

#### P38 : Export vers l'agenda (S), sans dépendance
- **UX :** **Ajouter mes prochaines doses à mon agenda** génère un fichier `.ics` avec les N prochaines doses une par une (pas de règle de récurrence, car le plan de titration change la dose), un rappel et les rendez-vous de P34. On l'enregistre (`CreateDocument("text/calendar")`) ou on le partage. Pour une seule dose : `Intent.ACTION_INSERT` vers `CalendarContract`, sans permission.

#### P39 : Raccourcis d'application et tuile Réglages rapides (S), sans dépendance
- **UX :** un appui long sur l'icône propose « Noter une dose », « Noter mon poids », « Me peser (balance) » et « +250 mL ». Une tuile Réglages rapides **+250 mL** est aussi disponible.
- **Technique :** `res/xml/shortcuts.xml` (statique), extras lus par `MainActivity`, et un `TileService`.

#### P40 : Sauvegarde chiffrée et transfert vers un nouveau téléphone (M), sans dépendance
- **UX :**
  - « Protéger mes sauvegardes par un mot de passe » s'applique à la sauvegarde manuelle et à la sauvegarde automatique.
  - **Nouveau téléphone** : on crée un fichier chiffré, puis on le partage par Quick Share (Bluetooth ou Wi-Fi direct, géré par Android, sans nuage).
  - Au premier lancement : « Restaurer depuis une sauvegarde ? ».
- **Technique :** AES-256-GCM, clé dérivée par PBKDF2WithHmacSHA256 (au moins 210 000 itérations, sel de 16 octets), seulement `javax.crypto`. En-tête JSON `{format: "glp1-enc", v: 1, salt, iv}`. Le ZIP de P12 suit le même chiffrement.

#### P41 : Autres langues (S chacune), sans dépendance
- Espagnol, puis portugais du Brésil, allemand et italien. Il faut les pluriels et une **relecture médicale par une personne de langue maternelle**. Shotsy en a 17.

#### P42 : Bibliothèque hors ligne et FAQ, l'alternative au chat IA (M, surtout du contenu), sans dépendance
- **UX :**
  - Une vingtaine de fiches courtes en FR et en EN : technique d'injection, conservation et voyage avec les stylos, dose oubliée, nausées, constipation, reflux, protéines, hydratation, alcool, chirurgie et anesthésie (prévenir l'anesthésiste), contraception, grossesse, arrêt du traitement.
  - Recherche plein texte. **Suggestions selon le contexte** : nausée notée → fiche « Nausées » ; première semaine d'un nouveau palier → fiche « Changement de dose ».
  - Chaque fiche cite la monographie et a un bouton « Ajouter à mes questions » (P34).
- **Données :** `assets/library/{fr,en}/*.md` avec un petit rendu Markdown maison. Relecture par un professionnel de la santé avant publication.

#### P43 : Mascotte (S/M), facultatif, faible priorité
- Un petit personnage en vecteurs sur l'accueil et dans le widget Habitudes (P35), qui réagit aux objectifs d'eau et de protéines. Il faut le désactiver facilement, car certains adultes trouvent ça infantilisant.

#### P44 : Masquer le contenu des notifications et des aperçus (S), sans dépendance
- **UX :** Réglages → Confidentialité : « Masquer le médicament sur l'écran verrouillé », qui donne `VISIBILITY_PRIVATE` avec une version publique « Rappel ». Option « Masquer l'aperçu dans les applications récentes » (`FLAG_SECURE`).
- **Données :** DataStore `hideNotificationContent` et `secureScreen`.

---

## 5. Top 15 à faire ensuite (par priorité)

**Étape 0, avant tout :** commiter et livrer ce qui est déjà en cours (rapport PDF, carte du corps, jalons et prévision), puis préparer la **migration v2 → v3 unique** pour les éléments ci-dessous.

| Rang | Élément | Ce qu'on égale ou dépasse | Effort |
|---|---|---|---|
| 1 | **P01** Alertes de stock, stylo à jeter, renouvellement, « Plus tard » | Alerte de stock bas (payante chez Glapp Max), Gala, Mochi | S |
| 2 | **P11** Repères de dose sur les graphiques et tendance lissée | Poids coloré par dose de Shotsy, rapports de Glapp | S |
| 3 | **P26 + P24** Journal enrichi (énergie, sommeil, selles, food noise, fringales, musculation) et protéines en g/kg | Bilan quotidien de Glapp, Noom, MyNetDiary (payant) | S/M |
| 4 | **P07** Phases de la semaine et effets selon le jour depuis l'injection | Phases de Glapp, JabKeeper (payant), analyses avancées de Gilly et Wiserapps (payantes) | M |
| 5 | **P08** Bilan hebdomadaire | Rapports de progrès de Glapp Max (payants) | M |
| 6 | **P12 + P13** Photos de progrès, comparaison, carte partageable, séries | Gilly et MeAgain (payants), Pep | M |
| 7 | **P03** Fioles magistrales, mg ↔ unités U-100, reconstitution, temps hors du frigo | Titrate, Regimen, Pep AI (payant) ; le temps hors du frigo, personne ne l'a | M |
| 8 | **P10** Comparaison aux essais cliniques | Glapp (sans compte ni collecte chez nous) | S/M |
| 9 | **P15** Mode maintenance | Shotsy | S/M |
| 10 | **P35** Widgets du niveau et des habitudes | Shotsy, mascotte de MeAgain (payante) | S/M |
| 11 | **P27** Symptômes étendus, personnalisés, signaux d'alarme | 29 symptômes de MeAgain ; les signaux d'alarme, personne ne les a | S/M |
| 12 | **P32** Health Connect élargi (lecture et écriture) | Synchronisation de Shotsy+ (payante), GlucoPal | M |
| 13 | **P30** Signes vitaux et bilans de laboratoire | Journey Tracker, GlucoseTrack, Steady, Calibrate | M |
| 14 | **P18 + P17** Base d'aliments locale (FCÉN, FR/EN) et macros complets | Recherche d'aliments de Pep, GlucoPal et Wiserapps (en ligne chez eux) | L |
| 15 | **P05** Plusieurs traitements en parallèle | Shotsy, Gilly (payant) | L |

**Ensuite, dans l'ordre :** P19 (codes-barres appris et lecture d'étiquette, M), P02 (doses sautées et changement de jour, S/M), P04 (coûts et ordonnance, S/M), P34 (rendez-vous, S), P28 (calendrier, M), P14 (composition corporelle, M), P09 (simulateur, S/M), P06 (minuteur des comprimés, S), P39 (raccourcis et tuile, S), P44 (confidentialité des notifications, S), P40 (sauvegarde chiffrée, M), P42 (bibliothèque, M), P29 (cycle et contraception, M), P38 (agenda, S), P16 (points à 3 et 6 mois, S), P33 (musculation, S/M), P20 (repères de portions, S), P23 (recettes, M), P31 (capteur de glycémie, M), P36 (notification persistante, S), P41 (langues, S chacune), P21 (voix, M), P37 (Wear OS, L), P25 (jeûne, S), P22 (IG, S), P43 (mascotte, S/M).

**Effort total du top 15 :** 2 S, 5 S/M, 6 M et 2 L. Cela représente environ 8 à 10 jours de travail pour dépasser tous les murs payants des suivis GLP-1, sauf l'IA générative et les services humains.

---

## 6. Sources

**Concurrents :**
[Shotsy, version gratuite](https://shotsyapp.com/free-glp-1-tracker/) ·
[Shotsy, comparatif des 5 meilleures applications](https://shotsyapp.com/best-glp-1-tracker-apps/) ·
[Shotsy vs Pep (mode maintenance, widgets)](https://shotsyapp.com/alternatives/pep/) ·
[Shotsy, effets secondaires](https://shotsyapp.com/glp-1-side-effects-tracker/) ·
[Prix de Shotsy (Pep)](https://pepglp1.com/pep-vs-shotsy-which-glp-1-medication-tracking-app-reigns-supreme/) ·
[Prix de Shotsy (GLP3 Planner)](https://glp3planner.com/resources/shotsy-alternatives) ·
[Test de Shotsy (glp1muscleloss)](https://www.glp1muscleloss.com/articles/shotsy-review/) ·
[Contenu de Glapp Max](https://glapp.io/blog/whats-included-glapp-max-premium-membership/) ·
[FAQ de Glapp](https://glapp.io/faq) ·
[Glapp, gratuit vs Max](https://glapp.io/reviews) ·
[Glapp sur iOS (essais cliniques)](https://glapp.io/blog/glapp-launches-ios-smart-glp1-tracker/) ·
[Glapp et SURMOUNT-5](https://glapp.io/blog/glp1-results-improve-with-tracking-app/) ·
[Bild Health, comparatif](https://www.bildhealth.com/best-glp-1-tracker-apps) ·
[Pep, gratuit vs Pro](https://pepglp1.com/glp1-tracking-app-ozempic-wegovy-mounjaro/) ·
[Pep, comparatif 2026](https://pepglp1.com/best-glp-1-tracker-apps/) ·
[Pep vs MeAgain](https://pepglp1.com/pep-vs-meagain/) ·
[MeAgain gratuit](https://meagain.com/free-glp-1-tracker) ·
[MeAgain, ludification](https://meagain.com/gamify-weight-loss) ·
[MeAgain, Journey Cards](https://meagain.com/glp-1-progress-tracker-app) ·
[Test de MeAgain (Longeviters)](https://longeviters.com/apps/meagain-app-review) ·
[GlucoPal sur l'App Store](https://apps.apple.com/us/app/glp-1-tracker-by-glucopal/id6670317407) ·
[GlucoPal vs Shotsy](https://glucopal.com/blog/shotsy-alternative) ·
[GlucoPal vs Glapp](https://glucopal.com/blog/glapp-alternative) ·
[BodyBuddy, comparatif](https://bodybuddy.app/blog/best-glp-1-tracking-apps) ·
[Wiserapps sur Google Play](https://play.google.com/store/apps/details?id=co.wiserapps.glpTracker&hl=en) ·
[Gilly sur l'App Store](https://apps.apple.com/us/app/gilly-glp-1-shot-tracker/id6755789451) ·
[Gilly](https://trygilly.com/) ·
[JabKeeper](https://jabkeeper.com/) ·
[Shotsy vs JabKeeper](https://jabkeeper.com/blog/shotsy-vs-jabkeeper) ·
[MyFitnessPal, soutien GLP-1](https://www.globenewswire.com/news-release/2026/04/28/3282728/0/en/MyFitnessPal-Launches-Comprehensive-GLP-1-Support-Helping-Users-Stay-Consistent-and-Build-Habits-Alongside-Medication-to-Maximize-Their-Experience.html) ·
[Test de MyFitnessPal (CravingWise)](https://www.cravingwise.com/reviews/myfitnesspal-weight-loss-app/) ·
[Comparatif LearnMuscles (Lose It)](https://learnmuscles.com/?p=169359) ·
[Compagnon GLP-1 de MyNetDiary](https://digital-release.kxan.com/business/press-releases/cision/20260505FL48085/mynetdiary-launches-glp-1-companion-for-ozempic-wegovy-and-mounjaro-users) ·
[Programme GLP-1 de WW (NZ)](https://weightwatchers.com/nz/how-it-works/glp-1-program) ·
[Programme GLP-1 de WW (AU)](https://www.weightwatchers.com/au/how-it-works/glp-1-program) ·
[Calibrate, fonctionnement](https://www.joincalibrate.com/how-it-works) ·
[Calibrate, prix](https://www.joincalibrate.com/pricing) ·
[Test de Calibrate](https://clearmetabolic.com/reviews/calibrate-review/) ·
[Compagnon GLP-1 de Noom](https://www.noom.com/med/glp1-companion/) ·
[Prix de Noom Med](https://nutriscan.app/blog/posts/noom-med-pricing-2026-glp1-program-cost-cae274c166) ·
[Found (U.S. News)](https://health.usnews.com/best-diet/medication/found) ·
[Ro Body](https://ro.co/weight-loss/glp-1/) ·
[Lancement de Ro Body](https://ro.co/press/ro-launches-body-program/) ·
[Ro (U.S. News)](https://health.usnews.com/best-diet/medication/ro) ·
[Juniper (Odycy)](https://www.odycy.com/en-gb/health-apps/lifestyle-wellness-and-fitness-apps/nutrition/juniper) ·
[Juniper (HeySlim)](https://www.heyslim.co.uk/blog/juniper-weight-loss-uk-guide) ·
[Mochi Health sur Google Play](https://play.google.com/store/apps/details?id=com.mochihealth.mochihealth&hl=en) ·
[Measured (Dealroom)](https://app.dealroom.co/companies/measured_1) ·
[Semaya](https://apps.apple.com/us/app/id6743040801) ·
[Titrate](https://titrate.health/) ·
[Regimen](https://apps.appfollow.io/ios/regimen-glp-1-peptide-tracker/6753905449?country=ca) ·
[Peptide Tracker](https://apps.apple.com/us/app/peptide-tracker-glp-1-dose/id6772795048) ·
[GLP-1 Journey Tracker](https://apps.apple.com/app/id6760089056) ·
[GlucoseTrack](https://appgoblin.info/apps/6763995508) ·
[Steady](https://chrome-stats.com/d/id6761083351) ·
[Gaby](https://apps.apple.com/app/id6760694482) ·
[Back2Me](https://mwm.ai/apps/back2me-glp-1-tracker/6753613121) ·
[Simple GLP](https://appshunter.io/ios/app/simple-glp-glp-1-shot-tracker/id6770137909) ·
[Glowise](https://apps.apple.com/us/app/glowise-glp-1-tracker-diary/id6730116702) ·
[GLPeak](https://play.google.com/store/apps/details?id=ai.glpeak.app&hl=en_US) ·
[maxbud](https://apps.apple.com/us/app/maxbud-glp-1-ai-tracker/id6479300175) ·
[Havit](https://appshunter.io/ios/app/havit-glp-1-and-weight-loss-coach/id6755166023) ·
[Gala](https://apps.apple.com/app/id6504834561) ·
[Jabby](https://apps.apple.com/app/id6746419318) ·
[Velto](https://apps.apple.com/app/apple-store/id6745454932) ·
[Glia](https://apps.appfollow.io/ios/glp-1-tracker-glia/6738976100?country=ca) ·
[GLP1Suivi](https://mwm.ai/apps/glp1suivi-injection-tracker/6758305182) ·
[Preserve](https://appgoblin.info/apps/6766536655) ·
[GLP Buddy](https://apps.apple.com/app/id6757923165)

**Technique et données :**
[ML Kit, modes d'installation (intégré ou Play Services)](https://developers.google.com/ml-kit/tips/installation-paths) ·
[ML Kit, données collectées sur Android](https://developers.google.com/ml-kit/android-data-disclosure) ·
[ML Kit GenAI (Gemini Nano)](https://developers.google.com/ml-kit/genai) ·
[API Prompt d'ML Kit (alpha)](https://android-developers.googleblog.com/2025/10/ml-kit-genai-prompt-api-alpha-release.html) ·
[Fichier canadien sur les éléments nutritifs](https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/nutrient-data/nutrient-code-listing.html) ·
[FCÉN sur le portail du gouvernement ouvert](https://open.canada.ca/data/en/dataset/90a31d6a-9131-4f31-a156-cd1f3b2717fe) ·
[Données d'Open Food Facts (taille, licence)](https://world.openfoodfacts.org/data) ·
[USDA FoodData Central](https://catalog.data.gov/dataset/fooddata-central) ·
[Zepbound hors du frigo (21 jours)](https://www.doctronic.ai/blog/how-long-can-zepbound-be-out-of-the-fridge/) ·
[Clue, GLP-1 et cycle menstruel](https://helloclue.com/science/ask-the-experts/glp-1s-and-your-cycle-the-impact-of-weight-loss-medications-on-hormonal-health)
