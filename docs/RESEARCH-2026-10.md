# Recherche multi-agents : ce que les utilisateurs de GLP-1 veulent (30 oct. 2026)

Trois agents ont cherché en parallèle :
1. les discussions Reddit (r/Zepbound, r/Mounjaro, r/Ozempic, r/Semaglutide, r/WegovyWeightLoss…) ;
2. les applications concurrentes de 2025-2026 et leurs avis ;
3. les applications de santé voisines (Happy Scale, MacroFactor, Cronometer, applications de podomètre, de musculation et d'hydratation).

**Limites :** reddit.com, les magasins d'applications et plusieurs sites étaient bloqués par le réseau de l'environnement de recherche. Les preuves viennent des résumés du moteur de recherche, d'une étude publiée sur les messages Reddit (Sehgal et al., 2026 : 410 198 messages de 9 sous-forums), de pages d'éditeurs et de guides cliniques. Beaucoup de comparatifs sont écrits par un concurrent. Les seuils médicaux marqués « à vérifier » doivent l'être avant d'être affichés.

Chaque idée a été vérifiée contre `PARITY.md`, `IDEAS.md`, `ROADMAP.md` et le code.

## Idées retenues, par priorité

| # | Idée | Signal | Hors ligne | Effort | Statut |
|---|---|---|---|---|---|
| 1 | **Podomètre intégré** : pas du jour lus par le capteur du téléphone, sans application tierce (Health Connect en complément) | Demande du propriétaire ; les applications de podomètre sont très populaires | oui | M | ✅ livré |
| 2 | **Compléments et remèdes du jour** : multivitamine, B12, vitamine D, fibres, magnésium, électrolytes, créatine, shake protéiné… à cocher ; remèdes au besoin (anti-nauséeux, laxatif, antiacide…) comptés et reliés aux symptômes | Signalé par les 3 agents ; NutriGLP et Injecta l'ont | oui | M | ✅ livré |
| 3 | **Reprise après une longue pause** (≥ 2 semaines) : carte « parlez-en à votre prescripteur avant de reprendre » | 2 agents ; UCLA Health recommande de revoir le prescripteur après 2 semaines sans dose | oui | S | ✅ livré |
| 4 | **Nouveaux symptômes** : frissons ou sensation de froid, bouffées de chaleur, changements du cycle menstruel | Étude Reddit : symptômes absents des notices | oui | S | ✅ livré |
| 5 | **Alerte de sous-alimentation** : moyenne sur 7 jours sous ~1 200 kcal (femmes) ou ~1 500 kcal (hommes), ou protéines sous 60 % de la cible, en ne comptant que les jours vraiment saisis | 2 agents ; diététistes (Today's Dietitian, WebMD 2026) | oui | S | ✅ livré |
| 6 | **Nouveau minimum** : petite fête quand le poids est le plus bas jamais enregistré, plus une comparaison des moyennes de cette semaine et de la précédente. Rien n'est fêté si la perte est trop rapide ou si l'objectif est dépassé | Happy Scale (mises à jour 2026) | oui | S | ✅ livré |
| 7 | **Boutons dans les rappels d'habitudes** : « +250 ml » dans le rappel d'eau | WaterMinder | oui | S | ✅ livré |
| 8 | Suivi d'assurance et de renouvellement d'autorisation : poids de départ verrouillé, % perdu par rapport à un seuil (souvent 5 % à 6 mois), date d'échéance, page PDF | 2 agents ; formulaire Manulife Wegovy (Canada) | oui | S/M | à faire |
| 9 | Rappels « mangez quelque chose » si rien n'a été saisi depuis N heures | 2 agents ; Cronometer, Obesity Action Coalition | oui | S | à faire |
| 10 | Protéines par repas (combien de repas atteignent 25 g), « protéines d'abord » | Agent 3 | oui | S | à faire |
| 11 | Prévision pour un événement (« mariage le 14 juin ») | Happy Scale | oui | S | à faire |
| 12 | Registre des fioles magistrales : alerte quand la concentration (mg/mL) change | Agent 1 ; rapports de la FDA sur des surdosages | oui | M | à faire |
| 13 | Importer depuis d'autres applications (CSV) et assistant pour saisir d'un coup les doses passées | Agent 2 | oui | M | à faire |
| 14 | Foundayo (orforglipron, comprimé approuvé par la FDA le 1er avril 2026) dans le catalogue | Agent 2 ; paramètres PK à sourcer | oui | S | à faire après vérification |
| 15 | Format de stylo : unidose, fiole, KwikPen 4 doses (jeté 30 jours après la première utilisation) | Agent 1 ; Lilly | oui | S | à faire |
| 16 | Réaction au site d'injection reliée au site, avec photo, et zone à éviter sur la carte | Agent 2 | oui | S/M | à faire |
| 17 | Journal de musculation : quelques exercices de référence, charge ou répétitions, comparés à la perte de poids | Agent 3 ; Lean, Retain, Preserve | oui | M | à faire |
| 18 | Test mensuel de fonction : se lever d'une chaise en 30 s, force de préhension | Agent 3 ; essais sur la sarcopénie | oui | S/M | à faire |
| 19 | Types de boissons (électrolytes, bouillon, café, alcool) et couleur de l'urine | Agent 3 ; WeightWatchers, GoodRx | oui | S/M | en partie (électrolytes dans les compléments) |
| 20 | Minuterie de repas lent (20 min) et note de satiété | Agent 3 | oui | S/M | à faire |
| 21 | Aliments déclencheurs ou mal tolérés, aliments « sûrs » les mauvais jours | Agents 1 et 3 | oui | S | à faire |
| 22 | Mode « arrêt » avec les courbes de reprise de poids des essais (STEP 1, SURMOUNT-4 ; à vérifier) | Agent 3 | oui | S/M | à faire |
| 23 | « Expliquer ce chiffre » au toucher de chaque statistique | Agent 2 ; avis sur Shotsy | oui | S | à faire |
| 24 | Heure d'injection : comparer les effets après les doses du matin et du soir | Agents 1 et 2 | oui | S | à faire |
| 25 | Pas : barres par heure, semaine contre semaine, série de jours | Agent 3 | oui | S/M | à faire |

## Ce que les gens détestent dans les autres applications

- **Fonctions de base payantes et abonnements qui s'accumulent :**
  - Shotwise, TrackShot, Gilly (60 jours d'historique en gratuit) ;
  - MeAgain, Pep ;
  - GLP Compass.
- **Données perdues après une mise à jour ou une déconnexion forcée, compte obligatoire :** Vivy, Shotsy, GLP AI, MeAgain.
- **Android traité en second, ou absent :** Glippy, Lumi, GlucoPal et Regimen sont sur iPhone seulement ; Glapp est en retard sur Android.
- **Repas mal estimés par l'IA :** c'est la première plainte contre Pep.
- **Chiffres difficiles à comprendre, écrans lourds, nutrition trop pauvre :** chez Shotsy, deux variations du poids s'affichent sans qu'on sache d'où vient la seconde.

Ce sont exactement nos points forts : gratuit, sans compte, hors ligne, Android d'abord, sauvegardes locales.

## Sources principales

- Étude Reddit (Sehgal, Tronieri, Ungar, Guntuku, 2026) : https://arxiv.org/abs/2603.12341 · https://www.healthday.com/2026-physicians-briefing/social-media-captures-glp-1-receptor-agonist-side-effects-not-seen-in-clinical-trials-2
- Pause de plus de 2 semaines : https://www.uclahealth.org/news/article/missed-dose-or-more-your-glp-1-medication-heres-what-do
- Apport minimal : https://www.todaysdietitian.com/need-to-know-nutritional-considerations-for-glp-1s/ · https://www.webmd.com/obesity/news/20260206/cm/scary-health-risk-that-can-sneak-up-on-glp-1-users
- Repas toutes les 3 à 4 h : https://www.obesityaction.org/wp-content/uploads/GLP-1s-and-Food.pdf
- Autorisation préalable et renouvellement : https://www.findhonestcare.com/treatments/wegovy/prior-authorization/requirements/ · https://www.manulife.ca/content/dam/consumer-portal/documents/en/forms/gb/wegovy-drug-prior-authorization-form-l5197e.pdf
- Compléments : https://apps.apple.com/us/app/nutriglp/id6758135559 · https://play.google.com/store/apps/details?id=injecta.app
- Happy Scale : https://justuseapp.com/en/app/532430574/happy-scale/reviews
- Zepbound KwikPen : https://www.hcplive.com/view/eli-lilly-launches-zepbound-kwikpen-multi-dose-tirzepatide-pen-for-adults-with-obesity
- Orforglipron : https://www.healio.com/news/endocrinology/20260401/fda-approves-orforglipron-an-oral-glp1-for-adults-with-obesity
- Noom offre gratuitement son compagnon GLP-1 depuis le 24 juin 2026 : https://www.noom.com/in-the-news/noom-launches-free-version-of-its-powerful-glp-1-companion/
