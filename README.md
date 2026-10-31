# Barça Home — Mi TV Home 1.4

**[Télécharger le ZIP Windows](https://github.com/midox25/mehdi/raw/refs/heads/main/downloads/BarcaHome-Videos-v1.4.zip)** · [Guide horloge, vidéos et annulation](VIDEOS.md) · [APK seul](https://github.com/midox25/mehdi/raw/refs/heads/main/downloads/BarcaHome.apk)

Accueil Android TV conçu pour MiTV-MOSR4 / Android 11. Présentation inspirée de la référence : barre arrondie Accueil / Direct / Apps, grand espace principal, suggestions vidéo locales et rangée d'icônes circulaires. Seules les applications installées avec une activité de lancement disponible apparaissent. Aucun accès réseau, publicité, compte, service d'accessibilité ou permission d'administration.

## Mise à jour Windows

Décompresser le ZIP dans un nouveau dossier, ouvrir PowerShell dans ce dossier puis :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Mettre-a-jour-Videos.ps1
```

ADB doit déjà être connecté et autorisé sur `10.114.7.230:5555`. Le script vérifie SHA256, sauvegarde l'APK existant, prépare l'annulation puis installe et ouvre l'application. La photo et l'accueil par défaut restent conservés. Le journal est horodaté et logcat est capturé uniquement pendant chaque commande. Les journaux peuvent contenir des informations personnelles : les conserver localement.

Pour une première installation de notre application, utiliser `Installer-Windows.ps1` à la place. Il sauvegarde l'accueil initial et ouvre l'application pour essai sans la définir comme accueil par défaut. La politique d'exécution indiquée ne concerne que ce processus PowerShell.

## Applications et navigation

Le catalogue combine les catégories MAIN/LEANBACK_LAUNCHER et MAIN/LAUNCHER. Les résultats sont filtrés : application et activité activées, activité exportée, application non suspendue, exclusion des lanceurs et outils internes identifiés. Les doublons sont regroupés par paquet avec préférence à l'activité TV. YouTube et Live TV sont prioritaires quand ils sont effectivement disponibles ; le reste est classé par nom. Aucune liste fictive d'applications ni d'affiches n'est utilisée.

Les cartes vidéo sont une sélection locale modifiable (Big Buck Bunny et Sintel au départ), pas un accès aux recommandations personnelles YouTube. Leurs visuels sont dessinés sans téléchargement. OK ouvre le lien dans YouTube TV ; disponibilité et lecture dépendent de YouTube. Menu → Vidéos suggérées permet de gérer les liens et d’annuler la dernière modification. Les icônes sont celles des applications installées. La sélection met à jour la zone principale, OK ouvre l'application, la loupe recherche localement et la roue dentée/touche Menu ouvre la personnalisation. Le catalogue est actualisé au retour sur l'accueil. L’horloge de l’accueil affiche par défaut l’heure TV moins une heure ; Menu → Décalage de l’horloge permet de revenir à l’heure TV sans modifier Android. Les transitions de focus durent 120 ms ; le défilement est assuré par les vues Android natives.

## Photo et accueil par défaut

La photo personnelle existante est conservée. Personnaliser → Choisir une photo utilise le sélecteur Android s'il est disponible. Une photo peut aussi être transférée dans `/sdcard/Android/data/com.mehdi.barcahome/files/background.jpg`, puis l'application doit être relancée. Le décodage est effectué hors du fil d'interface avec résolution limitée. Le ZIP ne contient pas la photo de l'utilisateur ; un fond architectural dessiné sert de secours.

L'historique privé reste dans YouTube → Vous / Bibliothèque → Historique. Le lanceur ne peut pas l'extraire ni gérer un compte à la place de l'utilisateur.

Après vérification physique seulement : Personnaliser → Choisir l'accueil par défaut, si cette page est exposée par le firmware. Ne pas désactiver l'accueil Xiaomi pour contourner une restriction de la touche Home.

## Annulation

Le dossier `sauvegarde-videos-...` contient l'APK précédent et `ANNULER-MISE-A-JOUR.ps1`. Exécuter ce script restaure la version précédente sans effacer les données. Conserver le dossier complet. Les anciens réglages DNS et désactivations d'applications ne sont pas modifiés et disposent de leurs propres sauvegardes antérieures.

Pour une première installation, le script d'annulation de `Installer-Windows.ps1` restaure le composant d'accueil capturé puis désinstalle uniquement notre application. La clé et le versionCode 1 sont maintenus pendant ces essais pour permettre une réinstallation réversible ; le versionName courant est 1.4.

## Compiler et vérifier

Python 3, JDK 21, SDK Android officiel : plateforme android-35 et build-tools 35.0.0.

```sh
ANDROID_HOME=/workspace/android-sdk PATH=/chemin/vers/jdk/bin:$PATH python3 build.py
javac --release 8 -d /tmp/barca-feature-tests src/com/mehdi/barcahome/AppCatalog.java src/com/mehdi/barcahome/ClockDisplay.java src/com/mehdi/barcahome/VideoSuggestion.java tests/AppCatalogTest.java tests/VideoAndClockTest.java
java -cp /tmp/barca-feature-tests AppCatalogTest
java -cp /tmp/barca-feature-tests VideoAndClockTest
```

Aucune bibliothèque tierce ni Gradle. La clé de développement reste hors du dépôt dans `../.barca-home-signing/development.p12` et n'est pas publiée. La conserver pour les mises à jour. Le build nettoie les anciennes classes générées, vérifie signature/alignement et écrit SHA256. Cette signature est destinée aux essais locaux, pas à une publication Play Store.

Voir [VALIDATION.md](VALIDATION.md) pour les contrôles exécutés et leurs limites. La version 1.4 reste à tester sur une TV ; aucun émulateur n'a été utilisé.

Versions précédentes : [Mi TV Home 1.3](MITV-HOME.md), [Camp Nou 1.2](CAMP-NOU.md), [Barcelona 1.1](BARCELONA.md).
