# Idées « wow » : Suivi GLP-1

*Atelier produit du 26 octobre 2026. Ce document complète `docs/ROADMAP.md` : la feuille de route dit quoi livrer ensuite, ce document cherche ce qui donnerait envie à chaque personne sous GLP-1 d'installer l'app et d'en parler.*

## L'idée directrice

L'app collecte déjà presque tout : l'heure, la dose et le site de chaque injection, les effets secondaires avec leur intensité, l'appétit, l'humeur, l'eau, les protéines, les fibres, le poids, les mensurations, le plan de paliers, et elle a un modèle pharmacocinétique. Les concurrents se contentent d'afficher ces données, et font souvent payer les graphiques.

**Le saut, c'est de répondre avec les données de la personne aux questions qu'elle se pose vraiment :** « Pourquoi j'ai la nausée tous les mercredis ? », « Est-ce un vrai plateau ? », « Est-ce que je mange assez de protéines pour garder mes muscles ? ». Tout se calcule sur le téléphone, sans IA en ligne ni serveur, avec des statistiques simples, honnêtes et expliquées.

**Contraintes non négociables** (voir ROADMAP §4) :
- **Hors ligne.** Le manifeste ne demande même pas la permission `INTERNET`. C'est un argument à mettre en avant dans la fiche Play.
- **Aucun conseil de dose.** On montre, on explique, et on renvoie au médecin ou au pharmacien.
- **FR et EN.** Chaque nouvelle table va dans la sauvegarde JSON, dans le CSV et dans le PDF, avec sa migration Room.

**Échelle d'effort** (la même que celle du ROADMAP) : **S** = moins d'environ 2 h · **M** = environ une demi-journée · **L** = plus d'une journée, ou plusieurs sous-systèmes.

---

## Ce que vivent les personnes sous GLP-1 en 2025-2026 (recherche rapide)

| Difficulté | Ce qu'on en sait (sources secondaires, à revérifier) | Idées |
|---|---|---|
| **Nausée selon le jour de l'injection** | Elle serait plus forte de J+1 à J+3, surtout après un changement de palier. Plus de 80 % des effets digestifs surviendraient pendant la titration (STEP 1, chiffre rapporté par des blogues). Elle dépend de la dose et du temps écoulé | A1, A2, B9 |
| **Faim et « food noise » qui reviennent en fin de semaine** | Plainte fréquente. On manque de données publiées sur le moment exact | A3 |
| **Perte musculaire** | De 25 à 40 % du poids perdu peut être de la masse maigre. Un avis conjoint publié en 2025 par 4 sociétés savantes recommande de 1,2 à 1,6 g de protéines par kg et par jour, avec de la musculation | A5, D2 |
| **Constipation, déshydratation** | Les étiquettes mettent en garde : vomissements et diarrhée → déshydratation → insuffisance rénale aiguë, surtout au début et après une hausse de dose | A9, E1 |
| **Plateaux, fluctuations de la balance** | Source d'anxiété et d'abandon | A4 |
| **Chute de cheveux** | Dans un sondage (auto-déclaré), environ 7 utilisateurs sur 10 rapportent une chute, et environ 40 % des personnes touchées disent avoir arrêté le traitement pour cette raison. La chute est liée à la vitesse de perte de poids et aux carences | A10, A5 |
| **« Ozempic face », peau relâchée, image corporelle** | Liés à la vitesse de perte. Des troubles de l'image corporelle sont rapportés | A10, D3, E6 |
| **Coût, assurance, pénuries** | Prix « comptant » de 149 à 349 $ US par mois en 2026 selon le produit, offres d'introduction qui expirent, cartes d'économie réservées à l'assurance privée | C4, C5 |
| **Magistral : unités, mg et mL** | La FDA a signalé en 2024 des surdoses de **5 à 20 fois** la dose prescrite, dues à la confusion entre mg, mL et unités, chez les patients comme chez les prescripteurs | C3 |
| **Voyage avec les stylos** | KwikPen de Mounjaro : jusqu'à 30 °C pendant 30 jours au maximum. Ozempic : 56 jours après la première utilisation. Toujours en cabine. Les fuseaux horaires ne sont traités nulle part | C1, C2 |
| **Arrêt et maintien** | De 47 % (diabète de type 2) à 65 % (sans diabète) des personnes arrêtent dans l'année (JAMA, 2025). Le poids revient après l'arrêt. Une diminution progressive est à l'étude | C6 |
| **Alcool et repas en société** | L'envie de boire diminue, la tolérance change. Repas de famille, sorties | C8, A8 |
| **Santé mentale** | 66 % des répondants à un grand sondage rapportent des effets émotionnels dans les 3 premiers mois. La nourriture ne sert plus de soupape | E6, D6 |

*Limite : reddit.com n'était pas accessible directement. La plupart des sources sont des blogues de cliniques ou des articles de presse (liste à la fin).*

---

## Socle commun : le moteur de « découvertes »

Le groupe A repose sur un même petit moteur en Kotlin pur, dans `domain/insights/`, testé unitairement comme `Milestones`. Il fournit trois choses :
- le **jour du cycle** de chaque événement (nombre d'heures depuis la dose précédente) ;
- des **associations décalées** (« X un jour, Y 0 à 2 jours plus tard ») ;
- une **tendance lissée** du poids.

Une seule carte d'interface, `InsightCard`, et un fil **« Découvertes »** sur l'accueil (« 1 nouvelle découverte ») servent à toutes les idées du groupe.

**Règles d'honnêteté**, qui rendent ces découvertes crédibles et nous distinguent :
1. Toujours afficher l'effectif (« sur 9 semaines, 23 épisodes »). Sous le seuil minimal, la carte affiche plutôt « Encore 3 semaines de données ».
2. Qualifier chaque découverte : **nette**, **possible** ou **probablement le hasard**. Un test de permutation de 2 000 tirages prend quelques millisecondes sur le téléphone.
3. Parler de ce qu'on observe chez la personne, jusqu'à maintenant. Jamais de « tu devrais changer ta dose ».
4. Mettre un bouton « Comment c'est calculé ? » sur chaque carte.
5. Reprendre les découvertes dans le PDF, section « Observations du patient » : c'est le médecin qui décide.

---

## A. Comprendre son corps avec ses propres données

### A1. Carte de chaleur « effets × jour après l'injection » · **S**
- **Problème :** « Pourquoi je suis à plat tous les mercredis ? » Les gens ne relient pas leurs symptômes au jour de leur cycle d'injection et ne savent pas si les choses s'améliorent.
- **Moment « wow » :** une grille de 14 symptômes × J0 à J6 qui s'allume, avec une phrase au-dessus : « Ta nausée culmine à J+2 (61 % des épisodes). Elle a diminué de moitié depuis 4 semaines à 5 mg. »
- **Hors ligne :** jour du cycle = heures écoulées depuis la dose précédente ÷ 24. On additionne les intensités, puis on divise par le nombre de cycles où ce jour a déjà eu lieu ; sans cette normalisation, le cycle en cours fausse le résultat. Le pic est le jour qui regroupe au moins 35 % du total, avec au moins 5 épisodes sur 3 cycles ou plus. On peut comparer « avant / après le palier ».
- **Données :** `injections`, `side_effects`. **Tout existe déjà**, aucune nouvelle saisie.

### A2. « Ta semaine type » et alerte de la veille · **M**
- **Problème :** on subit la nausée au lieu de la prévoir (repas, sorties, réunion importante).
- **Moment « wow » :** la veille de J+2, à 19 h : « Demain, c'est ton jour de nausée habituel. Prévois des repas légers et de petites gorgées, garde des craquelins à portée de main. » Sur l'accueil, un ruban de 7 jours en icônes météo (soleil, nuages, pluie) : la météo de ton corps.
- **Hors ligne :** profil moyen par jour du cycle (effets d'A1, appétit, humeur), avec un intervalle. La prévision ne s'affiche que si le pic revient dans au moins 3 des 4 derniers cycles. Un nouveau palier remet la confiance à zéro. Les rappels passent par `AlarmManager`, comme ceux qui existent déjà.
- **Données :** existantes, plus un nouveau `ReminderType`. Va plus loin que « Prédictions de phases » (ROADMAP P2).

### A3. Faim × niveau estimé : « ta faim revient à J+5 » · **S**
- **Problème :** en fin de semaine, la faim et les envies reviennent. Beaucoup croient que « le médicament ne marche plus » ou réclament une hausse de dose trop tôt.
- **Moment « wow » :** un nuage de points « appétit selon le niveau estimé », avec la phrase : « Ton appétit passe de "faible" à "normal" quand le niveau estimé descend sous environ 60 % de ton pic, en général vers J+5. » Des bandes de couleur apparaissent sur la courbe de niveau existante.
- **Hors ligne :** `Pharmacokinetics.levelAt` calculé à midi pour chaque jour du journal, corrélation de Spearman, puis seuil trouvé par découpage en quintiles. La carte se termine par « À montrer à ton médecin si ça te dérange », jamais par « fractionne ta dose ».
- **Données :** `daily_logs.appetite`, `injections`. Tout existe déjà.

### A4. Poids de tendance et détecteur de « vrai » plateau · **S/M**
- **Problème :** la balance varie de ±1 kg d'un jour à l'autre (eau, sel, transit, cycle menstruel). Résultat : panique, pesées compulsives, abandon. La plupart des « plateaux » ne sont que du bruit.
- **Moment « wow » :**
  - Le chiffre principal devient le **poids de tendance** : « 84,2 kg, en baisse de 0,4 kg/sem ».
  - Si la balance stagne mais pas la tendance : « Ce n'est pas un plateau : ta tendance baisse encore. Ta pesée de ce matin reste dans ta variation habituelle (± 0,7 kg). »
  - S'il s'agit d'un vrai plateau (3 semaines) : « Ton tour de taille a quand même perdu 2 cm. Ce qui a changé : protéines −20 %, eau sous l'objectif 4 jours sur 7, même palier depuis 10 semaines. »
- **Hors ligne :** moyenne mobile exponentielle adaptée aux pesées irrégulières (méthode « Hacker's Diet », α = 0,1 par jour). Pente de régression sur 21 jours, avec son intervalle de confiance. Le bruit est l'écart-type des résidus. `GoalForecast` utilise la tendance au lieu des pesées brutes.
- **Données :** `weights`, `measurements`, `daily_logs`, `food_entries` et le plan de paliers. Tout existe déjà.

### A5. Coach protéines selon la masse maigre · **M**
- **Problème :** de 25 à 40 % du poids perdu peut être du muscle. Quand on n'a plus faim, atteindre 100 g de protéines est difficile. Et l'objectif de 80 g pour tout le monde (notre réglage par défaut actuel) n'a pas de sens.
- **Moment « wow » :**
  - « Ton objectif : 105 à 125 g par jour, selon ta masse maigre estimée à 62 kg. Il te manque 28 g aujourd'hui : un yogourt grec (17 g), un œuf (6 g) et un verre de lait (8 g). »
  - Si la composition corporelle est connue : « Sur 9,1 kg perdus, environ 2,4 kg de masse maigre (26 %). C'est dans la cible. »
- **Hors ligne :**
  - **Masse maigre :** formule de Boer (sexe, taille, poids), ou % de gras importé de Health Connect (`BodyFatRecord`, `LeanBodyMassRecord`) ou saisi à la main.
  - **Objectif :** dans la fourchette de l'avis de 2025, réglable, avec la mention « à confirmer avec ton équipe soignante ».
  - **Suggestions :** les favoris de l'utilisateur et une petite table d'aliments riches en protéines intégrée à l'APK, par exemple un extrait du Fichier canadien sur les éléments nutritifs (licence ouverte, à vérifier). On choisit ceux qui comblent l'écart sans dépasser les calories qui restent.
- **Données :** nouveaux réglages facultatifs `sex` et `birthYear`, colonne `bodyFatPct` dans `measurements`, case « musculation » dans `daily_logs`.

### A6. Quel site me réussit le mieux ? · **S**
- **Problème :** certaines personnes jurent que la cuisse donne moins de nausée que le ventre, mais personne ne le vérifie sur ses propres données.
- **Moment « wow » :** sur la carte du corps : « Chez toi, les doses dans la cuisse sont suivies de 35 % moins de nausée dans les 72 h que celles dans l'abdomen (12 doses contre 14). Différence possible, pas certaine. » Les réactions locales (rougeur, bosse) sont épinglées sur la zone.
- **Hors ligne :** fenêtre de 72 h après chaque dose, somme des intensités par site, puis test de permutation. La carte n'apparaît qu'à partir de 4 doses par site. Elle rappelle que, selon les monographies, les sites sont équivalents.
- **Données :** `injections.site`, `side_effects`. Tout existe déjà.

### A7. Bilan de chaque palier · **S/M**
- **Problème :** « Est-ce que 7,5 mg vaut ces effets-là ? » La décision de rester au même palier ou de monter se prend vite, en consultation.
- **Moment « wow » :** une carte par palier : « 7,5 mg (6 sem.) : −0,6 kg/sem, contre −0,4 à 5 mg. Nausée 1,8 fois plus fréquente les 12 premiers jours, puis revenue au niveau d'avant. Appétit moyen : faible. » Les paliers se comparent côte à côte, aussi dans le PDF.
- **Hors ligne :** `ReportMath.doseSteps` existe déjà. Pour chaque palier, on calcule la pente de tendance (A4), la charge d'effets (A1), l'appétit moyen et le nombre de jours avant le retour au niveau de départ.
- **Données :** tout existe déjà.

### A8. Détecteur de déclencheurs (alcool, gras, gros repas, eau, sommeil) · **M**
- **Problème :** on soupçonne la poutine ou le vin, sans preuve. Les conseils génériques ne disent pas ce qui compte *pour toi*.
- **Moment « wow » :** « Les lendemains de soirée avec alcool, ta nausée est 2,4 fois plus fréquente (5 fois sur 6). Les repas gras n'ont pas d'effet visible chez toi. »
- **Hors ligne :** étiquettes du jour en un toucher (alcool, repas gras ou frit, gros repas, repas tardif, moins de 6 h de sommeil, ou sommeil lu dans Health Connect). Risque relatif calculé avec un décalage de 0 à 2 jours. La carte ne s'affiche qu'à partir de 5 occurrences, et avec le test de permutation.
- **Données :** nouvelle colonne `daily_logs.tags` (texte séparé par des virgules). En option, lecture de `SleepSessionRecord` dans Health Connect.

### A9. Tableau du transit (constipation) · **S/M**
- **Problème :** la constipation est un des effets les plus fréquents et les moins avoués. On s'en rend compte au 4ᵉ jour.
- **Moment « wow » :**
  - Un compteur discret, « 3 jours sans selle », et l'échelle de Bristol en dessins simples.
  - Une découverte : « Tes épisodes de constipation arrivent 2 jours après les journées sous 15 g de fibres et 1,2 L d'eau (7 fois sur 9). »
  - Une escalade en douceur : au jour 3, rappel fibres et eau ; au jour 5, avec ventre gonflé ou douleur, carte « Appelle ton pharmacien ou ton médecin » (voir E1).
- **Hors ligne :** même moteur décalé qu'A8. Les fibres viennent de `food_entries`, l'eau de `daily_logs`.
- **Données :** nouvelle colonne `daily_logs.bristol` (0 = rien noté) ou table `bowel_events(timestamp, bristol)`. Précise le « Journal enrichi » (ROADMAP P1).

### A10. Compteur de vitesse de perte · **S**
- **Problème :** perdre trop vite augmente le risque de chute de cheveux, de calculs biliaires, de perte musculaire et de « visage Ozempic ». La chute de cheveux fait arrêter une partie des personnes touchées.
- **Moment « wow » :**
  - Une jauge façon compteur de vitesse, en % du poids par semaine, avec une zone verte réglable (par défaut d'environ 0,5 à 1 % par semaine, à ajuster avec le médecin).
  - Au-delà de 1,5 % par semaine pendant 4 semaines : « Tu perds vite. La chute de cheveux arrive souvent 2 à 3 mois plus tard : surveille tes protéines (tu es à 62 % de ton objectif) et parles-en à ton médecin. »
  - Quand on note « chute de cheveux », l'app surligne la période de perte rapide de 2 à 3 mois plus tôt.
- **Hors ligne :** pente de la tendance (A4) divisée par le poids, et suivi des protéines (A5).
- **Données :** tout existe déjà.

### A11. Bilan du dimanche, rédigé en phrases · **M**
- **Problème :** personne ne lit les graphiques, et Glapp fait payer ses rapports. On veut savoir en 10 secondes comment s'est passée sa semaine.
- **Moment « wow » :** une notification le dimanche : « Semaine 14 : dose prise à l'heure, tendance −0,5 kg, nausée surtout jeudi et moins forte que la semaine passée, protéines atteintes 5 jours sur 7. Victoire notée : un cran de ceinture. » Le bilan se partage en image.
- **Hors ligne :** texte produit à partir de gabarits FR/EN (accords et pluriels avec `plurals`). On retient les 3 faits les plus marquants selon un score simple (écart à la moyenne × confiance). Aucun modèle de langue.
- **Données :** tout existe déjà. Concrétise le « Bilan hebdomadaire » (ROADMAP P1).

---

## B. Une expérience qui fait plaisir

### B1. Saisie éclair : la journée en 3 secondes · **S**
- **Problème :** avec la fatigue et la nausée, on abandonne le journal au bout de 2 semaines. Sans données, il n'y a pas de découvertes (groupe A).
- **Moment « wow » :** une seule carte « Comment ça va ? » avec 5 visages pour l'humeur, 5 niveaux de faim et une rangée de puces de symptômes **classées selon ton jour du cycle** : à J+2, la nausée vient en premier. Trois touchers, une vibration légère, c'est fini. Un « Annuler » en snackbar remplace les confirmations.
- **Hors ligne :** réorganisation de `FeelingCard`. Les puces sont triées selon la fréquence de chaque symptôme ce jour-là du cycle (A1).
- **Données :** tout existe déjà.

### B2. Tuiles Réglages rapides, raccourcis et widget « Aujourd'hui » · **S/M**
- **Problème :** ouvrir l'app pour noter 250 ml d'eau, c'est trop d'effort.
- **Moment « wow » :**
  - Une tuile « +250 ml » dont le sous-titre affiche « 1,25 L sur 2 L ».
  - Une tuile « Dose » qui affiche « J+3 · niveau 84 % ».
  - Un appui long sur l'icône : Noter une dose, Mon poids, +20 g de protéines.
  - Un widget 4×2 avec des anneaux eau et protéines et des boutons +.
- **Hors ligne :** `TileService` (et `requestAddTileService` à partir d'Android 13), `shortcuts.xml`, un nouveau `GlanceAppWidget` (Glance est déjà dans le projet).
- **Données :** aucune. Reprend ROADMAP P1, avec des sous-titres qui se mettent à jour en plus.

### B3. Compte à rebours sur l'écran verrouillé le jour de la dose · **S**
- **Problème :** l'oubli du jour J, et la question « je l'ai prise ou pas ? ».
- **Moment « wow » :** le jour J, une notification continue et discrète : « Dose à 9 h · cuisse gauche », avec un compte à rebours. Sur Android 16, c'est une « Live Update » (`Notification.ProgressStyle`) qui montre la progression de la semaine, avec un bouton « C'est fait ». Elle disparaît dès que la dose est notée.
- **Hors ligne :** `setUsesChronometer` et `setChronometerCountDown` (Android 7 et plus), `ProgressStyle` à partir de l'API 36. La notification respecte l'option « masquer le nom du médicament ».
- **Données :** aucune.

### B4. Rituel du jour J avec minuteur à vibrations · **S**
- **Problème :** le stress de la piqûre. On retire l'aiguille trop tôt (les notices demandent de compter quelques secondes après le clic) ou on oublie de sortir le stylo du frigo.
- **Moment « wow » :** mode plein écran « J'y vais », en trois étapes :
  1. le site suggéré, sur la silhouette ;
  2. « Stylo sorti depuis 30 min ? » ;
  3. un gros bouton « Aiguille en place » qui lance un **compte à rebours vibré** (une vibration par seconde, durée selon le stylo).

  À la fin, un « clic » vibré satisfaisant, et la dose est notée automatiquement avec le site. En option, une respiration guidée de 4 secondes pour l'anxiété.
- **Hors ligne :** `VibrationEffect.createWaveform`. Les durées par stylo vont dans le catalogue `Medication`, à revérifier dans chaque notice.
- **Données :** nouveau champ `holdSeconds` dans le catalogue.

### B5. Tuile et complication Wear OS · **L**
- **Problème :** on oublie, et le téléphone est au fond du sac.
- **Moment « wow » :** « C'est fait » noté depuis le poignet, une complication « dose dans 2 j » ou un anneau du niveau estimé, et +250 ml au poignet.
- **Hors ligne :** module `wear` séparé. Le Wearable Data Layer passe par Bluetooth entre le téléphone et la montre, sans serveur. Tiles API et complications.
- **Données :** aucune nouvelle. Reprend ROADMAP P2.

### B6. Silhouette animée du progrès · **M/L**
- **Problème :** on ne « voit » pas son corps changer, et les photos mettent beaucoup de gens mal à l'aise.
- **Moment « wow » :** une silhouette stylisée, pas une photo. Sa taille, ses hanches, sa poitrine, ses bras et ses cuisses suivent les vraies mensurations. Un curseur la fait évoluer du premier jour à aujourd'hui, avec le contour de départ en pointillé.
- **Hors ligne :** tracé vectoriel paramétré (chaque largeur est proportionnelle à la mesure actuelle ÷ la mesure de départ), animé avec `animateFloatAsState` dans Compose.
- **Données :** `measurements`. Tout existe déjà.

### B7. « Mon année GLP-1 », façon Wrapped · **M**
- **Problème :** rien ne fait autant parler d'une app qu'une image qu'on a envie de partager.
- **Moment « wow » :** à l'anniversaire du traitement, et en décembre, une série de cartes plein écran :
  - « 52 doses, dont 51 à l'heure » ;
  - « Ton site préféré : la cuisse gauche » ;
  - « Tu as bu 640 L d'eau » ;
  - « Ta semaine la plus dure : le passage à 10 mg… et tu as tenu bon » ;
  - « −18 kg, l'équivalent de 2 pneus d'auto ».

  Chaque carte s'exporte en image 9:16. Par défaut, **aucun poids ni nom de médicament** n'apparaît, seulement des pourcentages et des icônes. On peut les afficher si on le veut.
- **Hors ligne :** rendu Compose en image (`rememberGraphicsLayer().toImageBitmap()`) et partage par le `FileProvider` déjà utilisé pour le PDF.
- **Données :** tout existe déjà.

### B8. Saisie vocale sur l'appareil · **M**
- **Problème :** noter quelque chose quand on a les mains occupées ou qu'on cuisine.
- **Moment « wow » :** « 30 grammes de protéines, un litre d'eau, nausée légère » : les trois jauges bougent.
- **Hors ligne :** `SpeechRecognizer.createOnDeviceSpeechRecognizer` (Android 12 et plus, si le modèle de langue est installé ; sinon le bouton n'apparaît pas). Une grammaire simple reconnaît les nombres, les unités et le dictionnaire des symptômes FR/EN. Rien ne sort du téléphone.
- **Données :** aucune.

### B9. Mode nausée · **S**
- **Problème :** les jours de nausée, l'app affiche les mêmes objectifs culpabilisants (1 500 kcal, 100 g de protéines).
- **Moment « wow » :** dès qu'on note une nausée modérée ou plus forte, l'accueil passe en « mode doux » :
  - de l'eau en petites gorgées, avec des rappels plus fréquents ;
  - des protéines « faciles » (boisson protéinée, yogourt, bouillon) ;
  - de courts conseils (petites portions, pas de gras, gingembre, ne pas s'allonger après le repas) ;
  - la carte d'A2 : « D'habitude, ça passe vers J+3 ».
- **Hors ligne :** état calculé dans `UiState`, contenus fixes en FR et en EN.
- **Données :** tout existe déjà.

---

## C. Des outils pratiques

### C1. Mode voyage : fuseaux horaires et stylos · **M**
- **Problème :** vol Montréal–Paris, dose prévue le dimanche à 9 h. À quelle heure la prendre ? Le rappel sonne à 3 h du matin. Le stylo peut-il aller dans la valise ?
- **Moment « wow » :** on entre « Voyage à Paris du 3 au 17 mars ». L'app propose deux plans :
  - garder l'heure de Montréal (dose à 15 h à Paris) ;
  - ou décaler la dose en respectant l'écart minimal du médicament (48 h pour le sémaglutide, 72 h pour le tirzépatide ; ces règles sont déjà dans `MissedDose`).

  Les rappels se recalent tout seuls à l'heure locale. Une liste de bagages s'ajoute : stylos en cabine, ordonnance ou lettre du médecin, aiguilles, sac isotherme, et « Il te faut 2 doses, apportes-en 3 ».
- **Hors ligne :** base de fuseaux d'Android (`ZoneId`), calcul de l'écart. `ReminderScheduler` doit planifier en heure absolue (`Instant`) plutôt qu'en heure locale.
- **Données :** nouvelle table `trips(id, zoneId, start, end, strategy)`.

### C2. Chaîne du froid du stylo · **S**
- **Problème :** le stylo oublié dans l'auto ou hors du frigo. Les limites varient selon le stylo : KwikPen de Mounjaro jusqu'à 30 °C pendant 30 jours au maximum, Ozempic 56 jours après la première utilisation, quelques semaines pour d'autres.
- **Moment « wow » :** « Stylo hors du frigo depuis 9 jours sur 30 », avec un bouton « Je l'ai remis au frigo ». Pendant un voyage, une alerte : si le téléphone lui-même dépasse 35 °C (température de la batterie), « Ton téléphone a chaud. Si ton stylo est dans le même sac, vérifie qu'il n'a pas dépassé 30 °C. »
- **Hors ligne :** `BatteryManager.EXTRA_TEMPERATURE`, un indice grossier présenté comme tel. Limites par médicament dans `Supply` (`defaultDiscardDays` existe déjà), à revérifier dans les monographies.
- **Données :** réglage `penOutOfFridgeSince`.

### C3. Calculatrice visuelle pour le magistral (seringues U-100 et stylos à clics) · **M**
- **Problème :** en 2024, la FDA a signalé des surdoses de 5 à 20 fois la dose prescrite de sémaglutide magistral, dues à la confusion entre mg, mL et unités.
- **Moment « wow » :** on entre la concentration (ou la quantité de poudre et le volume d'eau bactériostatique) et la dose en mg. L'app dessine une **seringue U-100 à l'échelle**, le piston arrêté sur la bonne graduation : « 0,5 mg = 0,20 mL = 20 unités ». Elle détecte les erreurs :
  - un facteur 10 (virgule mal placée) ;
  - une dose plus de 2 fois supérieure à la précédente ;
  - un volume qui dépasse la seringue choisie (30, 50 ou 100 unités) ;
  - une demi-unité sur une seringue qui n'a pas ces graduations.

  Un bouton « Montrer à mon pharmacien » affiche l'écran en grand.
- **Mode stylo « à clics » :** il fonctionne seulement avec un ratio clics/mg **donné par le pharmacien ou le prescripteur**. Aucune valeur tirée des forums n'est intégrée, car les fabricants ne les valident pas. L'écran affiche « Utilisation non prévue par le fabricant ».
- **Hors ligne :** calcul simple : mL = mg ÷ (mg/mL), unités = mL × 100.
- **Données :** table `vials(concentrationMgPerMl, volumeMl, openedAt, budDays)`, colonnes `injections.units` et `injections.vialId`. Concrétise « Fioles magistrales » (ROADMAP P1).

### C4. Coûts, assurance, coupons… et impôts · **S/M**
- **Problème :** les prix changent, les cartes d'économie expirent, il y a la franchise et la coassurance. Personne ne sait ce que le traitement coûte vraiment.
- **Moment « wow » :**
  - « Ce mois-ci : 94,50 $ de ta poche, 380 $ payés par l'assureur. Depuis le début : 1 120 $, soit 61 $ par kg perdu. »
  - Des alertes : « Ta carte d'économie expire le 31 décembre », « Ta franchise annuelle est atteinte ».
  - En février, un **relevé annuel pour le crédit d'impôt pour frais médicaux** (fédéral et Québec), exportable en PDF ou en CSV avec les photos des reçus.
- **Hors ligne :** tables locales, photos des reçus en stockage privé.
- **Données :** table `purchases(date, pharmacy, product, qty, totalCents, insurerCents, outOfPocketCents, couponId?, receiptPath?)` et table `coupons(name, expiresOn, notes)`. Version plus complète de « Coût et assurance » (ROADMAP P2).

### C5. Renouvellement, ordonnance et plan anti-pénurie · **S**
- **Problème :** on découvre que le stylo est vide le jour de la dose, les ordonnances expirent, et en pénurie on appelle six pharmacies.
- **Moment « wow » :**
  - « Commande d'ici jeudi : ta pharmacie demande 3 jours et il te reste 1 dose. »
  - « Ordonnance : 2 renouvellements restants, elle expire le 14 février. Prends rendez-vous. »
  - « Ton stock est en 5 mg et ton prochain palier sera 7,5 mg : demande une nouvelle ordonnance. »
  - Un carnet de pharmacies (« appelée le…, en stock : oui ou non ») et un message prérempli pour la pharmacie, que la personne envoie elle-même par texto.
- **Hors ligne :** `Supply.runsOutOn` existe déjà ; on y ajoute le délai de la pharmacie et la date de l'ordonnance. Envoi par `ACTION_SENDTO` vers `sms:`.
- **Données :** réglages `pharmacyLeadDays`, `rxExpiresOn` et `rxRefillsLeft`, et table `pharmacies(name, phone, lastCalled, hadStock)`.

### C6. Plan de maintien et de diminution · **M**
- **Problème :** la reprise de poids après l'arrêt est la grande crainte. Les plans d'espacement (7 → 10 → 14 jours) ou de baisse de dose se suivent aujourd'hui sur papier.
- **Moment « wow » :** le médecin propose un plan, l'app le rend visible :
  - la courbe de niveau projetée qui descend en douceur ;
  - une « zone de maintien » de ± 2 kg autour du poids atteint ;
  - une **sentinelle** : « Ta tendance a pris 1,3 kg en 3 semaines et ta faim est "élevée" 4 jours sur 7. Parles-en à ton médecin avant la prochaine étape. »
- **Hors ligne :** `Titration` généralisé (chaque étape a une dose, un intervalle et une durée), `Pharmacokinetics.curve` pour la projection, la tendance d'A4 et l'appétit d'A3.
- **Données :** format de plan étendu en `dose:jours:intervalle`, qui reste compatible avec l'ancien, et réglages `phase` et `maintenanceRangeKg`. Va plus loin que le « Mode maintenance » (ROADMAP P1).

### C7. Préparer le rendez-vous : les questions s'écrivent toutes seules · **S**
- **Problème :** la consultation dure 10 minutes et on oublie la moitié de ce qu'on voulait dire.
- **Moment « wow » :** « Questions suggérées à partir de tes données : 1) Nausée sévère 3 fois depuis le passage à 7,5 mg, est-ce normal ? 2) Plateau de 4 semaines malgré 110 g de protéines par jour. 3) Chute de cheveux depuis août. » On coche, on ajoute ses propres questions, et le tout va dans le PDF.
- **Hors ligne :** des règles tirées des découvertes A1 à A10, mises en phrases par gabarits.
- **Données :** réglage `lastAppointmentDate` ou table `appointments` (ROADMAP P1).

### C8. Sorties, restos et alcool · **S/M**
- **Problème :** souper de famille, 5 à 7, Noël. On a peur d'être malade, on est mal à l'aise de refuser, et l'alcool frappe plus fort ou n'attire plus.
- **Moment « wow » :**
  - « Souper au resto samedi » : l'app sait que samedi tombe à J+2 chez toi et conseille : « Commande léger, mange les protéines d'abord, évite la friture, demande une boîte pour emporter. »
  - Des phrases toutes faites pour refuser poliment.
  - Un compteur de consommations, avec un rappel : l'alcool peut être moins bien toléré et, si tu prends de l'insuline ou un sulfonylurée, augmenter le risque d'hypoglycémie.
- **Hors ligne :** contenus fixes, et jour du cycle (A1, A2) calculé pour la date de la sortie.
- **Données :** étiquette « alcool » (A8), nombre de consommations, table `events(date, kind)`.

---

## D. Motivation

### D1. Séries bienveillantes, avec des jokers · **S**
- **Problème :** les séries punitives font abandonner au premier écart (grippe, voyage).
- **Moment « wow » :** « 18 doses à l'heure d'affilée », « protéines atteintes 9 jours de suite ». Deux jokers par mois se placent tout seuls les jours où on note un effet sévère : « Jour de nausée : ta série est protégée. »
- **Hors ligne :** extension de `Milestones.streak`.
- **Données :** tout existe déjà, plus un réglage `jokersUsed`.

### D2. Défis de 7 à 30 jours · **M**
- **Problème :** les bons conseils (protéines, musculation, fibres) restent théoriques.
- **Moment « wow » :** des défis comme « 30 g de protéines au déjeuner pendant 7 jours », « 2 séances de musculation par semaine pendant 4 semaines », « 25 g de fibres 5 jours sur 7 » ou « 7 000 pas » (pas lus dans Health Connect). Le journal remplit la barre de progression tout seul, et un badge marque la fin du défi.
- **Hors ligne :** défis définis dans le code et évalués sur les données existantes.
- **Données :** table `challenges(id, type, startedOn, completedOn)`, permission `READ_STEPS` en option.

### D3. Photos avec silhouette fantôme et accéléré · **L**
- **Problème :** les photos avant/après sont mal cadrées et impossibles à comparer, et on ne veut pas les voir dans la galerie.
- **Moment « wow » :** au moment de la photo, la précédente apparaît en transparence (30 %) pour reprendre la même pose. Ensuite, un **accéléré** fait défiler toutes les photos au même cadrage, ou un curseur compare avant et après. Une pose « visage » permet de suivre l'effet « Ozempic face », sans jugement.
- **Hors ligne :** CameraX (`PreviewView` avec l'image superposée), stockage dans `filesDir` (hors galerie), protégé par le verrou de l'app. Export MP4 en option avec `MediaCodec` et `MediaMuxer`.
- **Données :** table `photos(id, timestamp, pose, path)`, archive ZIP en option dans la sauvegarde. Reprend ROADMAP P1, en ajoutant le fantôme et l'accéléré.

### D4. Journal des victoires hors balance · **S**
- **Problème :** la balance stagne mais la vie change. On ne le note pas, et on l'oublie.
- **Moment « wow » :** une liste à cocher (« J'ai croisé les jambes », « 3 étages sans pause », « un cran de ceinture », « plus besoin de rallonge de ceinture en avion ») et un **historique des tailles de vêtements** (« du 18 au 12 »). Pendant un plateau (A4), l'app rappelle : « Pendant ce plateau, tu as noté 4 victoires. »
- **Hors ligne :** liste fixe et texte libre.
- **Données :** tables `victories(id, date, key?, text)` et `clothing_sizes(date, type, size)`.

### D5. Équivalents en objets · **S**
- **Problème :** « −12,5 kg », c'est abstrait.
- **Moment « wow » :** « −12,5 kg : un sac de 25 lb de farine », « un pneu d'auto », « 2,5 chats », avec une animation de l'objet qui se remplit. On peut le partager sans dévoiler son poids.
- **Hors ligne :** table d'objets (poids et image vectorielle).
- **Données :** tout existe déjà.

### D6. Capsule « mon pourquoi » · **S**
- **Problème :** les jours de nausée ou de découragement, on oublie pourquoi on a commencé.
- **Moment « wow » :** au début, on écrit (ou on enregistre en audio, sur le téléphone) pourquoi on commence. Le message revient au bon moment : après une nausée sévère, après 3 jours d'humeur basse, pendant un plateau ou à −10 %. « Le 3 mars, tu as écrit : "Pour courir avec Léa." »
- **Hors ligne :** texte ou audio dans `filesDir`, déclencheurs calculés dans `UiState`.
- **Données :** réglage `whyText` et fichier audio facultatif.

---

## E. Sécurité

### E1. Vérificateur de signaux d'alarme (et sentinelle de déshydratation) · **M**
- **Problème :** « J'ai mal au ventre : est-ce normal, ou je dois aller à l'urgence ? » Les étiquettes mettent en garde contre la pancréatite, les problèmes de vésicule biliaire et l'insuffisance rénale aiguë par déshydratation, mais personne ne lit la notice à 2 h du matin.
- **Moment « wow » :**
  - Quand on note une douleur au ventre « sévère » ou des vomissements, l'app pose 2 ou 3 questions oui/non, par exemple : « La douleur est-elle forte et continue, ou va-t-elle vers le dos ? »
  - Si oui, une carte rouge : « Possible pancréatite : consulte d'urgence. Ne prends pas ta prochaine dose avant d'en avoir parlé à un médecin. » Boutons [Appeler le 911] et [Info-Santé 811].
  - Une **sentinelle automatique** : vomissements ou diarrhée 2 jours de suite et moins de 1 L d'eau → « Signes de déshydratation à surveiller : urine foncée ou rare, étourdissements, bouche sèche… »
- **Règles** (tirées des monographies, à faire relire par un pharmacien) :
  - pancréatite ;
  - vésicule biliaire : douleur en haut à droite, fièvre, jaunisse ;
  - déshydratation et reins ;
  - allergie grave : 911 ;
  - masse au cou, voix enrouée, difficulté à avaler (mise en garde sur la thyroïde) ;
  - baisse soudaine de la vue (NAION, effet très rare du sémaglutide selon l'EMA en 2025) ;
  - constipation sévère avec ventre gonflé et vomissements (occlusion) ;
  - hypoglycémie sévère en cas de diabète ;
  - pensées suicidaires : 9-8-8.
- **Hors ligne :** arbre de décision fixe. Numéros selon le pays : 911, 811 et 9-8-8 au Canada ; 15 ou 112 et 3114 en France ; 911 et 988 aux États-Unis. `ACTION_DIAL` ouvre le composeur sans demander de permission d'appel.
- **Données :** table `safety_checks(timestamp, ruleId, answers, outcome)`, reprise dans le PDF. L'effort est surtout du contenu à valider.

### E2. Anti-doublon : « Est-ce que je l'ai prise ? » · **S**
- **Problème :** l'angoisse classique du matin, avec le risque de prendre deux doses ou de noter la même dose deux fois.
- **Moment « wow » :**
  - À l'ouverture, en gros : « Dernière dose : hier à 9 h 02, cuisse gauche. »
  - Si on note une dose moins de 48 h (sémaglutide) ou 72 h (tirzépatide) après la précédente : « Tu as déjà noté une dose il y a 20 h. Est-ce un doublon ? Si tu t'es vraiment injecté deux doses, appelle ton pharmacien ou le Centre antipoison (1 800 463-5060 au Québec). »
  - En option, une photo de la fenêtre du stylo à chaque dose, comme preuve.
- **Hors ligne :** `minGapHours` existe déjà dans `MissedDose`.
- **Données :** colonne facultative `injections.photoPath`.

### E3. Interactions et rappels liés aux autres médicaments · **M**
- **Problème :** beaucoup ignorent les interactions qui comptent :
  - la pilule contraceptive est moins fiable sous tirzépatide pendant les 4 semaines qui suivent le début du traitement et chaque hausse de palier (selon l'étiquette) ;
  - risque d'hypoglycémie avec l'insuline ou les sulfonylurées ;
  - absorption retardée des médicaments pris par la bouche (lévothyroxine, warfarine…) ;
  - règles de prise du Rybelsus : à jeun, avec au plus 120 mL d'eau, 30 minutes avant le reste ;
  - arrêt avant une grossesse planifiée : au moins 2 mois avant pour le sémaglutide.
- **Moment « wow » :** on coche ses autres médicaments dans une courte liste. À chaque palier de Mounjaro : « Ta pilule peut être moins efficace pendant 4 semaines : ajoute une méthode d'appoint (selon l'étiquette). » Un mode « Projet de grossesse » rappelle d'en parler au médecin.
- **Hors ligne :** petite base JSON intégrée à l'APK (une vingtaine d'entrées vérifiées), reliée aux paliers de `Titration`.
- **Données :** table `other_meds(name, class)`. L'effort est surtout du contenu à valider.

### E4. Glycémie et hypoglycémies (diabète) · **M**
- **Problème :** une grande partie des personnes sous Ozempic ou Mounjaro sont diabétiques, alors que les apps visent surtout la perte de poids.
- **Moment « wow » :** un bouton « Je fais une hypo » lance un minuteur pour la règle du 15 (15 g de glucides, puis on revérifie dans 15 minutes) et note l'épisode dans le journal. S'ajoutent un graphique de la glycémie superposée au niveau estimé et l'HbA1c dans le PDF.
- **Hors ligne :** saisie manuelle (mmol/L par défaut au Canada, mg/dL en option) et lecture de `BloodGlucoseRecord` dans Health Connect, où écrivent les lecteurs de glycémie.
- **Données :** table `vitals(timestamp, type, value)` (ROADMAP P1) et table `hypo_events`.

### E5. Carte d'urgence et chirurgie · **S**
- **Problème :** les GLP-1 ralentissent la vidange de l'estomac, ce qui compte pour l'anesthésie. Les recommandations ont changé en 2024, et beaucoup de patients oublient d'en parler.
- **Moment « wow » :** une carte plein écran, ouverte depuis la notification ou le widget (version réduite quand le téléphone est verrouillé) : médicament, dose, date de la dernière injection, autres médicaments, allergies. Un mode « Chirurgie ou endoscopie prévue le… » envoie des rappels : « Dis à l'équipe d'anesthésie que tu prends du tirzépatide et que ta dernière dose date du… Elle te dira quoi faire. »
- **Hors ligne :** écran local, aucune donnée transmise.
- **Données :** réglages `allergies` et `procedureDate`.

### E6. Bien-être mental et mode « sans chiffres » · **S/M**
- **Problème :** effets émotionnels fréquents au début du traitement, nourriture qui ne sert plus de soupape, image corporelle, troubles alimentaires.
- **Moment « wow » :**
  - Un bilan mensuel de 2 questions (PHQ-2, libre d'utilisation). Si l'humeur est basse 5 jours sur 7, une carte douce propose des ressources (9-8-8, Info-Social 811).
  - Un **mode « sans chiffres »** masque le poids et l'IMC, garde les tendances et les victoires, et ne célèbre pas une perte trop rapide.
- **Hors ligne :** calcul sur le téléphone, aucune donnée transmise.
- **Données :** table `checkins(date, phq2)` et réglage `hideNumbers`.

---

## TOP 10 : impact × faisabilité

**Impact** (1 à 5) : à quel point l'idée règle une vraie difficulté, combien de personnes elle touche, et si elle fait parler de l'app. **Faisabilité** (1 à 5) : 5 quand les données existent déjà et que l'effort est S, 1 pour un nouveau module ou un effort L. En cas d'égalité, on classe d'abord par impact, puis par potentiel de bouche-à-oreille.

| Rang | Idée | Impact | Faisab. | Score | Pourquoi elle passe devant |
|---|---|---|---|---|---|
| 1 | **A1** Carte de chaleur « effets × jour après l'injection » | 5 | 5 | **25** | Zéro nouvelle saisie, réponse immédiate à la question n° 1 (« quand vais-je être malade ? »), et c'est la base d'A2, A3, A6, A7 et B1 |
| 2 | **A4** Poids de tendance et détecteur de vrai plateau | 5 | 5 | **25** | Calme l'anxiété de la balance tous les jours ; `GoalForecast` fait déjà une régression |
| 3 | **E1** Vérificateur de signaux d'alarme et de déshydratation | 5 | 4 | **20** | La fonction qui inspire confiance, et que les médecins et pharmaciens recommanderont |
| 4 | **C3** Calculatrice visuelle U-100 avec détection des erreurs ×10 | 5 | 4 | **20** | Prévient les surdoses signalées par la FDA, et le magistral est massivement utilisé |
| 5 | **A5** Coach protéines selon la masse maigre | 5 | 4 | **20** | Le sujet santé de 2025-2026 (préserver ses muscles), personnalisé et concret (« il te manque 28 g ») |
| 6 | **A3** Faim × niveau estimé (« ta faim revient à J+5 ») | 4 | 5 | **20** | Le modèle PK existe déjà ; explique la « fin de semaine » dont tout le monde parle |
| 7 | **E2** Anti-doublon et « Est-ce que je l'ai prise ? » | 4 | 5 | **20** | Une angoisse quotidienne réglée en une ligne ; `minGapHours` existe déjà |
| 8 | **A10** Compteur de vitesse de perte | 4 | 5 | **20** | Parle de la chute de cheveux et du « visage Ozempic » avant qu'ils arrivent |
| 9 | **B1** Saisie éclair en 3 secondes | 4 | 5 | **20** | Alimente tout le groupe A : sans journal, pas de découvertes |
| 10 | **C1** Mode voyage (fuseaux horaires et stylos) | 4 | 4 | **16** | Personne ne le fait, et c'est une vraie source de stress ; les règles d'écart existent déjà |

*Juste sous la barre :* **B7** « Mon année GLP-1 » (5 × 3 = 15), le meilleur pari pour faire parler de l'app, à sortir pour décembre. Ensuite **E3** Interactions (4 × 4 = 16, mais le contenu doit être validé) et **C5** Renouvellement (4 × 4 = 16).

**Ordre de livraison suggéré :**
1. **« Découvertes »** : A1, A3, A4, A10 et B1, **sans aucune migration**.
2. **« Sécurité »** : E1, E2 et C3, avec **une seule migration** v2 → v3 qui regroupe `safety_checks`, `vials`, `injections.units`, `injections.vialId` et `injections.photoPath`.
3. A5 et C1.

---

## Esquisses d'implémentation des 5 premières idées

### 1. A1 : carte de chaleur « effets × jour après l'injection »
- **Domaine.** Créer `domain/insights/CycleAnalysis.kt` en Kotlin pur, avec ses propres types d'entrée sur le modèle de `MilestoneInput` (`domain/` ne doit pas dépendre des entités Room) : `CycleInput(doseTimes: List<Long>, events: List<Event(timestamp, type: SideEffectType, severity)>, intervalDays, nowMillis)`.
- **Fonctions.**
  - `cycleDay(t)` : recherche binaire de la dernière dose avant `t`. Les jours au-delà de l'intervalle sont regroupés dans une case « en retard ».
  - `exposure(day)` : nombre de cycles où ce jour a déjà eu lieu.
  - `heatmap()` : `Map<SideEffectType, DoubleArray>` des intensités normalisées par l'exposition.
  - `peak(type)` : jour du pic, part, n, nombre de cycles et qualificatif « nette », « possible » ou « hasard » (test de permutation sur les jours).
  - `compare(beforeMillis)` pour l'avant/après d'un palier, qui reprend `ReportMath.doseSteps`.
  - Tests dans `app/src/test/.../domain/CycleAnalysisTest.kt` : cycles incomplets, dose en retard, aucune dose.
- **Interface.**
  - `ui/components/Heatmap.kt` : grille dessinée sur un Canvas comme `LineChart`, couleurs tirées du thème, toucher d'une case pour voir le nombre d'épisodes, description TalkBack ligne par ligne.
  - `JournalScreen.SideEffectSummaryCard` : bascule « 30 derniers jours / par jour du cycle ».
  - `HomeScreen` : une `InsightCard` avec la phrase du pic.
  - `report/ReportLayout.kt` et `DoctorReport.kt` : section « Effets selon le jour du cycle » dessinée sur le Canvas du PDF.
- **État et textes.** Le calcul est fait dans `AppViewModel` (champ `cycleInsights` de `UiState`, calculé une fois par changement de données), pas dans les composables. Textes dans `strings_insights.xml`, en EN et en FR, avec `plurals`.
- **Base de données :** aucun changement.

### 2. A4 : poids de tendance et détecteur de vrai plateau
- **Domaine.** Créer `domain/Trend.kt` :
  - `smooth(weights)` : moyenne mobile exponentielle à pas irrégulier. Avec α = 0,1 par jour, on prend α_eff = 1 − (1 − α)^Δjours entre deux pesées.
  - `slope(window = 21 j)` : moindres carrés sur la tendance, avec l'erreur type.
  - `noise()` : écart-type des résidus entre les pesées et la tendance.
  - `classify()`, qui renvoie l'un de ces états :
    - `INSUFFICIENT` : moins de 6 pesées sur 21 jours ;
    - `LOSING` ;
    - `FALSE_PLATEAU` : pesées brutes à plat, mais pente de la tendance encore négative de façon nette ;
    - `PLATEAU` : pente au-dessus de −0,1 % du poids par semaine pendant au moins 21 jours, avec un intervalle qui contient 0 ;
    - `REGAINING`.

  `GoalForecast` (dans `Milestones.kt`) prend la tendance en entrée au lieu des pesées brutes. Tests dans `TrendTest.kt`, avec des séries synthétiques bruitées à graine fixe.
- **Interface.**
  - `HomeScreen.WeightCard` et `ProgressScreen.ProgressHero` affichent « Tendance : 84,2 kg, −0,4 kg/sem », et la pesée du jour en petit avec « dans ta variation habituelle ».
  - `ProgressScreen.WeightChartCard` : `LineChart` reçoit une deuxième série. Les pesées deviennent des points pâles et la tendance une ligne pleine.
  - Nouvelle `PlateauCard` dans `ProgressScreen`. Sa liste de vérification se construit à partir des 21 jours précédents comparés aux 21 jours d'avant : protéines (`daily_logs` et `food_entries`), eau, fibres, changement de tour de taille (`measurements`), semaines au même palier (`Titration.status`), effets sévères. Elle suggère aussi D4 (victoires).
- **Réglages.** `Settings.kt` : `trendAsMainNumber`, activé par défaut. Le widget peut garder le poids brut.
- **Base de données :** aucun changement.

### 3. E1 : vérificateur de signaux d'alarme et de déshydratation
- **Domaine.** Créer `domain/safety/RedFlags.kt` :
  - `RedFlagRule(id, trigger: (SafetyInput) -> Boolean, questions: List<Question>, level: EMERGENCY | URGENT | CALL_SOON, messageRes, actions)` ;
  - `SafetyInput` : effets des 72 dernières heures, eau des 2 derniers jours, médicament, présence d'un diabète traité ;
  - `evaluate()` renvoie les alertes actives ;
  - `answer(ruleId, answers)` renvoie la conclusion.
- **Contacts.** `domain/safety/EmergencyContacts.kt` donne les numéros selon le pays (`Locale.getDefault().country`, modifiables dans Paramètres).
- **Interface.**
  - Nouveau `Editor.Safety(ruleId)` dans le `sealed interface Editor` d'`Editors.kt`, et une feuille `ui/screens/SafetySheet.kt` : questions oui/non une à la fois, puis carte de conclusion colorée selon le niveau. Les boutons ouvrent le composeur avec `Intent.ACTION_DIAL` (aucune permission).
  - L'enregistrement d'un effet dans l'éditeur appelle `evaluate()` et ouvre la feuille si une règle s'applique.
  - `UiState.safetyAlerts` (sentinelle : vomissements ou diarrhée 2 jours de suite et moins de 1 L d'eau) affiche une carte en haut de `HomeScreen`, au-dessus de `NextDoseCard`. On peut la fermer, mais elle revient si les signes persistent.
  - Une entrée permanente « J'ai un symptôme inquiétant » dans `JournalScreen`.
- **Données.** Table `safety_checks(id, timestamp, ruleId, answers, outcome)` dans la migration v2 → v3, avec `Backup.kt` (JSON et CSV) et une section « Signaux d'alarme rapportés » dans le PDF.
- **Contenu et tests.** Le contenu est tiré des monographies, relu par un pharmacien, et porte toujours « Ceci n'est pas un diagnostic ». Tests unitaires pour chaque règle.

### 4. C3 : calculatrice visuelle U-100 et détection des erreurs ×10
- **Domaine.** Créer `domain/Dosing.kt`, sans dépendance Android :
  - `concentration(powderMg, diluentMl)` ;
  - `volumeMl(doseMg, mgPerMl)` ;
  - `units(volumeMl) = volumeMl × 100` ;
  - `pickSyringe(units)` parmi 30, 50 et 100 unités ;
  - `check(doseMg, units, previousDoseMg, syringe)`, qui renvoie des avertissements : `DECIMAL_SHIFT` (la valeur saisie vaut 10 fois ou 1/10 de la valeur calculée), `OVER_SYRINGE`, `JUMP_OVER_2X`, `HALF_UNIT_UNSUPPORTED`, `ABOVE_LABEL_MAX` ;
  - `clicksToMg(clicks, clicksPerMg)`, seulement avec un ratio saisi par la personne.

  `DosingTest.kt` couvre chaque cas, y compris les exemples de la FDA (5 unités lues comme 50).
- **Données.** Migration v2 → v3 : table `vials(id, medication, concentrationMgPerMl, volumeMl, openedAt, budDays, notes)` et colonnes nullables `injections.units` et `injections.vialId`. `Backup.kt` et le CSV ajoutent ces champs ; la restauration reste compatible avec les anciennes sauvegardes.
- **Interface.**
  - `ui/components/Syringe.kt` : seringue dessinée sur un Canvas avec ses graduations réelles (petites et grandes, demi-unités sur 30 unités) et le piston animé jusqu'à la bonne graduation.
  - `ui/screens/DoseCalculator.kt` : feuille ouverte depuis l'éditeur de dose quand le médicament est `*_COMPOUNDED`. Saisie en mg **ou** en unités, conversion affichée dans les deux sens, avertissements en rouge qui demandent une confirmation explicite, et mode « Montrer à mon pharmacien » en plein écran.
  - `Treatment.SupplyCard` montre le volume restant dans la fiole et la date limite d'utilisation (on reprend `Supply.discardDate`).
  - L'action « C'est fait » de la notification et du widget note aussi les unités.
  - Le PDF ajoute une colonne « unités ».

### 5. A5 : coach protéines selon la masse maigre
- **Réglages.** `data/Settings.kt` : `sex` (F, M ou non précisé), `birthYear`, `proteinPerKg` (1,2 à 1,6, réglable) et `proteinBasis` (masse maigre ou poids visé).
- **Domaine.** Créer `domain/Protein.kt` :
  - `leanMassKg(sex, heightCm, weightKg, bodyFatPct?)` : formule de Boer (H : 0,407 × poids + 0,267 × taille − 19,2 ; F : 0,252 × poids + 0,473 × taille − 48,3), ou poids × (1 − % de gras) si on le connaît. Si le sexe n'est pas précisé, calcul sur le poids visé.
  - `targetRange()` ;
  - `gap(today)`, qui additionne `daily_logs.proteinG` et `food_entries` ;
  - `suggest(gap, kcalLeft, favorites, catalog)` : choix gourmand des aliments au meilleur rapport protéines/calories ;
  - `leanLossShare(bodyComps)`, quand au moins 2 mesures de composition sont espacées de 4 semaines ou plus.

  `domain/ProteinFoods.kt` (ou `res/raw/protein_foods.csv`) contient une cinquantaine d'aliments courants au Canada, avec leurs noms FR/EN, la portion, les protéines et les calories. Tests dans `ProteinTest.kt`.
- **Données.** Colonne `measurements.bodyFatPct` et colonne `daily_logs.strengthTraining` (migration). `health/HealthConnect.kt` importe `BodyFatRecord` et `LeanBodyMassRecord`, avec les permissions `READ_BODY_FAT` et `READ_LEAN_BODY_MASS` dans le manifeste et l'écran de justification existant.
- **Interface.**
  - Une étape facultative « Pour calculer tes protéines » dans `OnboardingScreen`.
  - `JournalScreen.HabitsCard` : le `GoalRow` des protéines prend l'objectif calculé, avec une puce « Il te manque 28 g » qui ouvre les suggestions. Un toucher sur une suggestion l'ajoute comme `FoodEntity`.
  - Case « Musculation aujourd'hui » dans la saisie éclair (B1).
  - Carte « Composition » dans `ProgressScreen`, avec la part de masse maigre dans le poids perdu et un seuil d'attention à 35 % (« Parles-en, la musculation aide »).
  - Le PDF reprend l'objectif, le taux d'atteinte et la composition.

---

## Sources consultées (recherche rapide, octobre 2026)

*La plupart sont des sources secondaires (presse, blogues de cliniques). Il faut revérifier chaque chiffre dans les monographies canadiennes et les publications d'origine avant de l'afficher dans l'app ou la fiche Play.*

- Nausée selon le jour et la dose : [Frontiers, figure (PMC8294388)](https://pmc.ncbi.nlm.nih.gov/articles/PMC8294388/figure/f2) · [Doctronic, effets du tirzépatide semaine par semaine](https://www.doctronic.ai/blog/tirzepatide-side-effects/)
- Protéines et masse maigre : [Second Nature, planificateur protéines GLP-1](https://www.secondnature.io/guides/weight-loss-medications/glp1-muscle-protein-planner) · [Clinical Nutrition Center](https://www.clinicalnutritioncenter.com/research-updates/glp-1-protein-strategy-preserve-muscle-while-losing-weight)
- Arrêt, reprise de poids, diminution progressive : [Fox News, étude JAMA sur l'arrêt](https://www.foxnews.com/health/study-reveals-how-rapidly-weight-comes-back-after-quitting-obesity-medications.amp) · [AJMC, cohorte de la Cleveland Clinic](https://www.ajmc.com/view/weight-regain-after-glp-1-discontinuation-is-less-rapid-in-real-world-hamlet-gasoyan-phd) · [APhA, arrêt progressif](https://www.pharmacist.com/Publications/Transitions/coming-off-glp-1s-slowly-could-be-key-to-preventing-weight-regain)
- Alcool : [GoodRx, tirzépatide et envie d'alcool](https://www.goodrx.com/mounjaro/tirzepatide-alcohol-cravings)
- Mises en garde (pancréatite, vésicule, reins) : [Lilly Medical, mises en garde de Mounjaro](https://medical.lilly.com/us/products/answers/are-there-any-special-warnings-or-precautions-with-the-use-of-mounjaro-tirzepatide-311029)
- Voyage et conservation : [Lilly Canada, voyager avec Mounjaro](https://www.lilly.com/en-CA/resources/faq/mounjaro-kwikpen/travelling-with-mounjaro) · [Healthline, voyager avec Ozempic](https://www.healthline.com/health/drugs/how-to-travel-with-ozempic)
- Erreurs de dose en magistral : [HealthDay, avertissement de la FDA](https://www.healthday.com/health-news/weight-loss/fda-warns-of-accidental-overdoses-from-compounded-versions-of-ozempic) · [Physician's Weekly](https://www.physiciansweekly.com/post/fda-warns-of-accidental-overdoses-from-compounded-versions-of-ozempic)
- Chute de cheveux : [Sondage sur la chute de cheveux (PMC12965749)](https://pmc.ncbi.nlm.nih.gov/articles/PMC12965749/) · [Healio, Penn Medicine 2026](https://www.healio.com/news/dermatology/20260731/glp1-therapy-associated-with-hair-loss-in-people-with-type-2-diabetes)
- Santé mentale : [eMarketer, sondage de 83 000 utilisateurs](https://www.emarketer.com/content/many-glp-1-patients-may-unprepared-physical-emotional-toll-of-treatment) · [APA Monitor, 2025](https://www.apa.org/monitor/2025/07-08/weight-loss-drugs-mental-health)
- Coûts 2026 : [NBC News, prix 2026 et TrumpRx](https://nbcnews.com/health/health-news/weight-loss-drug-prices-2026-glp-1-pills-trumprx-what-expect-rcna249520)
