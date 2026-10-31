# Barça Home — Android TV

**[Télécharger le paquet Windows (ZIP)](https://github.com/midox25/mehdi/raw/refs/heads/main/downloads/BarcaHome-Windows.zip)** · [APK seul](https://github.com/midox25/mehdi/raw/refs/heads/main/downloads/BarcaHome.apk)

Première version compilée et signée ; essai physique sur TV encore nécessaire.

## Version Barcelona 1.1

**[Télécharger la mise à jour Barcelona](https://github.com/midox25/mehdi/raw/refs/heads/main/downloads/BarcaHome-Barcelona-v1.1.zip)** · [Guide de mise à jour et annulation](BARCELONA.md)

Courbes inspirées de Gaudí, mosaïque discrète, tons terre cuite et photo personnelle choisie depuis Windows. Pour une TV où Barça Home est déjà installé, utiliser `Mettre-a-jour-Barcelona.ps1`, qui sauvegarde la version précédente avant remplacement.

Lanceur local conçu pour MiTV-MOSR4 / Android 11. Deux tuiles : YouTube et Live TV, un bouton Personnaliser, une horloge, navigation au pavé directionnel, bordure de focus visible et transitions de 120 ms. Aucun accès réseau, compte, publicité, télémétrie ou modification des autres applications.

Le fond par défaut est une illustration abstraite. Une photo personnelle de Barcelone peut être importée ; aucune photo ni marque du FC Barcelone n'est fournie. L'historique privé reste dans YouTube → Vous / Bibliothèque → Historique. Le lanceur ne peut pas extraire cet historique ni assurer une lecture automatique ou contourner l'écran de sélection du profil.

## Installation Windows, après essai

Décompresser le paquet téléchargé dans un dossier. Garder `BarcaHome.apk`, `BarcaHome.apk.sha256` et `Installer-Windows.ps1` ensemble. Ouvrir PowerShell dans ce dossier. ADB doit déjà être connecté et autorisé sur la TV.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Installer-Windows.ps1
```

Cette option de politique ne concerne que ce processus PowerShell. Le script vérifie l'empreinte de l'APK, sauvegarde l'inventaire et l'accueil actuel, prépare l'annulation puis installe et ouvre le lanceur pour essai. Il ne définit pas l'accueil par défaut et ne désactive pas Xiaomi. Si un accueil initial unique ne peut pas être identifié, il s'arrête. Il refuse de remplacer une installation existante. Les journaux logcat peuvent contenir des informations privées : les garder localement.

Photo personnelle (JPG/PNG de préférence, paysage 1920×1080) :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Installer-Windows.ps1 -Photo "$env:USERPROFILE\Pictures\barcelone.jpg"
```

Ou Personnaliser → Choisir une photo si la TV possède un sélecteur de documents. Le décodage s'effectue hors du fil d'interface, à résolution limitée. La photo est copiée dans les données de l'application, l'original reste intact. Le sélecteur n'applique pas les métadonnées de rotation EXIF : utiliser une image déjà orientée en paysage.

## Essai à effectuer sur la TV

1. Vérifier que l'écran est entièrement visible et que YouTube reçoit le focus initial.
2. Naviguer gauche/droite/bas/haut et vérifier la bordure de focus sans saccade.
3. Ouvrir YouTube puis gérer soi-même profil/connexion ; revenir avec Accueil (qui ouvre encore l'accueil Xiaomi), puis rouvrir Barça Home depuis les applications.
4. Tester Live TV : il ouvre uniquement `com.mitv.livetv`, jamais Xiaomi TV+. Si indisponible, le lanceur affiche un message et ne réactive rien.
5. Tester Personnaliser et Réglages. Annuler le sélecteur de photo ne doit rien modifier.
6. Vérifier le fond personnel et la lisibilité à distance.

Après validation seulement : Personnaliser → Choisir l'accueil par défaut, si la TV offre cette page. Autre possibilité ADB, à exécuter volontairement après l'essai :

```powershell
& "$env:USERPROFILE\TV-tools\platform-tools\adb.exe" -s 10.114.7.230:5555 shell cmd package set-home-activity --user 0 com.mehdi.barcahome/.HomeActivity
```

Sur certains firmwares Xiaomi, le choix du lanceur est limité ou la touche Home peut rouvrir l'accueil constructeur. Ne pas désactiver les lanceurs d'origine pour contourner cette restriction.

## Annulation

Le script crée un dossier horodaté `sauvegarde-...` contenant `ANNULER.ps1`. Exécuter ce fichier depuis PowerShell : il restaure d'abord le composant d'accueil capturé puis désinstalle uniquement notre application (et sa copie de photo). Les sauvegardes et fichiers photo d'origine restent intacts. Les désactivations et réglages DNS précédemment effectués ont leur propre fichier d'annulation dans `TV-tools\nettoyage-...` ; ils ne sont pas modifiés par ce projet.

## Compilation

Python 3, JDK 21 et SDK Android officiels : plateforme android-35 et build-tools 35.0.0. Depuis ce dossier :

```sh
ANDROID_HOME=/workspace/android-sdk PATH=/chemin/vers/jdk/bin:$PATH python3 build.py
```

Pas de Gradle ni bibliothèque tierce. La clé de développement est créée hors du projet dans `../.barca-home-signing/development.p12`. Elle sert aux essais, pas à la publication Play Store. Garder la même clé pour les futures mises à jour et ne pas la publier. Le build vérifie la signature et l'alignement de l'APK puis écrit SHA256.

## Validation et limites

Consulter VALIDATION.md. La compilation ne garantit pas le comportement du firmware : test physique à réaliser avant de changer l'accueil par défaut. Aucun test sur votre TV n'est effectué depuis le cloud et aucune installation distante n'est prétendue.
