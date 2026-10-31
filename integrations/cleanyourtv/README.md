# CleanYourTV sur MiTV-MOSR4 — essai et nettoyage limité

[Projet d'origine](https://github.com/Gathin23/cleanyourtv) · source examinée : `57c00a40dafe040b0be1f32086e96c05a1a37054`.

Le dépôt contient une fiche pour le MiTV-MOSR4 et une note concernant le build `RTM5.220609.003.1493`. Sa liste principale a été essayée sur un autre firmware (`1585`) et reste provisoire. Elle n'est pas appliquée ici : elle inclut des fonctions à conserver et signale une perte de Wi-Fi/risque de récupération après désactivation de launcherx/setupwraith.

## Télécharger

[CleanYourTV-MiTV-MOSR4.zip](https://github.com/midox25/mehdi/raw/refs/heads/main/downloads/CleanYourTV-MiTV-MOSR4.zip)

Ce paquet contient un APK compilé depuis les sources du projet, pas une release officielle de Gathin. Les sources utilisées, leurs licences, le config.xml adapté et la provenance sont inclus. Aucune clé de signature, sauvegarde de TV ou donnée personnelle n'est incluse.

## 1. Essayer le nouvel accueil

Extraire le ZIP dans un nouveau dossier, ouvrir PowerShell dans ce dossier :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Essayer-CleanYourTV.ps1
```

ADB doit déjà être connecté et autorisé. Le script vérifie le modèle et le SDK, sauvegarde l'inventaire, les réglages et le composant Home, capture l'écran et prépare l'annulation avant d'installer. Il ouvre `com.gathin.tvhome/.MainActivity` pour essai. Aucun autre paquet n'est désactivé et le choix de Home reste intact.

Le dock donne la priorité à YouTube, Live TV et Barça Home. La configuration Live TV pointe vers `com.mitv.livetv` (HDMI/sources conservés). Aucune permission de localisation ou WRITE_SECURE_SETTINGS n'est accordée, aucun service d'accessibilité/notification n'est activé.

Tester avec la vraie télécommande : directions, OK, Retour, YouTube, Live TV, HDMI, Réglages, volume maintenu et touche Home. Home revient encore à notre interface habituelle. Envoyer `apres.png` ou une photo ; garder les autres sauvegardes privées.

**Deux accueils distincts :** notre application `com.mehdi.barcahome` conserve la photo, l'horloge décalée et les suggestions vidéo. Ces ajouts ne sont pas intégrés au lanceur CleanYourTV. La tuile Barça Home permet de rouvrir notre application.

## 2. Nettoyage avec les outils officiels

Python 3 est nécessaire (déjà confirmé installé par l'utilisateur). Le script crée un environnement virtuel local et installe les dépendances du fichier officiel. Ensuite :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Nettoyer-CleanYourTV.ps1
```

Périmètre strict, déjà autorisé :

- `com.google.android.feedback` — rapports d'erreurs.
- `com.miui.tv.analytics` — analyse Xiaomi.
- `com.xiaomi.statistic` — statistiques Xiaomi.
- `fusion.android.tv.demo` — démonstration magasin, identifiée dans la fiche du modèle.

Les trois premiers étaient déjà désactivés au dernier contrôle : on ne prétend pas à un nouveau gain de performance pour eux. Le script utilise **sans modification** `upstream/tools/disable.ps1`, avec le backend ADB natif autorisé. Les garde-fous et l'enregistrement d'état de CleanYourTV restent actifs. Une liste modifiée est refusée avant accès à la TV. Le résultat est relu et une capture est prise. Aucun réglage de DNS ou d'animation n'est changé.

Préservés : les deux anciens lanceurs, setupwraith, Live TV/HDMI, tous les fournisseurs, réseau, Bluetooth, télécommande, clavier, Play Store/Services, Assistant, Chromecast/Miracast et mises à jour. Ne pas exécuter la liste complète `examples/xiaomi-mitv-mosr4/debloat.txt`.

Après vérification visuelle, tester une fois un redémarrage normal et comparer les états. Ne pas redémarrer si Home affiche un écran de récupération ou si la TV a perdu le Wi-Fi. Aucune désactivation automatique répétée à chaque démarrage.

## Choisir l'accueil et revenir en arrière

Le dossier `sauvegarde-essai-cyt-...` contient :

- `CHOISIR-CET-ACCUEIL.ps1` : choisir CleanYourTV comme Home, seulement après validation de l'essai ; aucun ancien lanceur n'est désactivé.
- `ANNULER-ESSAI.ps1` : remettre le composant Home capturé et désactiver uniquement l'application d'essai. Aucun paquet n'est désinstallé. CleanYourTV reste installé mais inactif et peut être réactivé par `pm enable --user 0 com.gathin.tvhome`.

Le dossier `sauvegarde-nettoyage-cyt-...` contient `ANNULER-NETTOYAGE.ps1`. Il appelle `upstream/tools/restore.ps1` avec la même liste puis rétablit le composant Home capturé. Les applications déjà désactivées restent désactivées selon l'enregistrement officiel. Conserver le dossier du ZIP et les enregistrements dans `%USERPROFILE%\.cleanyourtv` : ils servent à retrouver l'état exact.

Exécuter un script d'annulation depuis son dossier avec :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ANNULER-ESSAI.ps1
```

Utiliser `ANNULER-NETTOYAGE.ps1` pour annuler le nettoyage. Les logs et captures restent locaux. Pendant les essais ADB reste disponible ; une fois toutes les vérifications terminées, désactiver le débogage réseau/sans fil et USB depuis la TV et vérifier sa fermeture selon le guide officiel.

## Validation réalisée

- Build officiel Gradle : `testReleaseUnitTest lintRelease assembleRelease` réussi.
- 307 tests Java exécutés : zéro échec, erreur ou test ignoré.
- Lint : zéro erreur ; 97 avertissements et 96 informations existants sont conservés.
- APK signé v2, aligné, 586 113 octets, sous la limite du projet (589 824 octets).
- 7 tests du backend ADB natif, 12 tests de protection des paquets cœur et 26 tests de garde Home passent (45 tests Python, sans tests ignorés dans le passage final).
- Scripts d'essai/annulation exécutés avec ADB simulé : Home et paquets existants préservés pendant l'essai, capture PNG contrôlée, retour à Home capturé et refus d'une liste modifiée avant tout accès TV.
- Pas d'installation ni nettoyage exécutés sur la vraie TV depuis le cloud. Le script de nettoyage complet n'a pas été exécuté sur une vraie TV ni dans un environnement Windows réel.

## Reproduire l'APK

Le ZIP fournit `upstream/launcher`, les outils, les licences et le config.xml utilisé. Source exacte et dépendances : voir `UPSTREAM.txt`. JDK 21 (17–22 accepté), SDK platform 34 et build-tools 34.0.0 ; Gradle 8.9 est vérifié par le wrapper :

```powershell
Set-Location .\upstream\launcher
.\gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

Compiler seulement : ne pas lancer `build-and-install.ps1` par défaut, car il peut désactiver un lanceur. Notre procédure d'essai ne l'utilise pas et conserve tous les garde-fous des outils de nettoyage. Une compilation locale crée une autre clé de développement : ne pas remplacer une installation existante par un APK signé avec une autre clé. La clé du build fourni reste privée hors du dépôt.
