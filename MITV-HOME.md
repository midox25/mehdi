# Mi TV Home 1.3

Présentation inspirée de la référence fournie : navigation en pilule, grande zone principale à gauche, rangée de cartes puis icônes circulaires des applications. Les couleurs de fond sont violet/bleu, avec la photo personnelle existante conservée. Les affiches de films, les comptes et les recommandations de la capture ne sont pas reproduits : les cartes sont des accès vers les applications réellement disponibles.

## Mettre à jour sur Windows

1. Télécharger et extraire [BarcaHome-MiTV-v1.3.zip](https://github.com/midox25/mehdi/raw/refs/heads/main/downloads/BarcaHome-MiTV-v1.3.zip) dans un nouveau dossier.
2. Ouvrir PowerShell dans ce dossier, puis :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Mettre-a-jour-MiTV.ps1
```

3. Le script vérifie l'empreinte, sauvegarde l'APK installé et écrit `sauvegarde-mitv-...\ANNULER-MISE-A-JOUR.ps1`, puis installe et ouvre la version 1.3. Il conserve la photo et le choix d'accueil par défaut.
4. Envoyer une photo de la TV après lancement pour vérifier la présentation.

## Fonctionnement

- Les noms et icônes sont lus depuis Android, sans téléchargement ni compte.
- Une application apparaît si elle propose une activité de lancement activée, exportée et non suspendue. Les activités TV sont préférées aux activités téléphone et chaque paquet n'apparaît qu'une fois.
- YouTube et Live TV sont placés en premier s'ils sont disponibles. L'onglet Direct disparaît si l'application `com.mitv.livetv` n'est pas disponible.
- Les applications désactivées, les lanceurs d'accueil et les outils internes d'usine/configuration identifiés ne sont pas affichés. Aucun paquet n'est activé, désactivé ou désinstallé par cette mise à jour.
- Revenir sur l'accueil actualise la liste. Les cartes et les icônes présentent les mêmes applications : le focus met à jour la grande zone principale, OK ouvre l'application.
- La loupe recherche localement dans les noms des applications disponibles. La roue dentée et la touche Menu ouvrent Personnaliser.
- L'historique personnel se consulte dans YouTube. Ce lanceur ne lit pas les comptes et ne fabrique pas de recommandations vidéo.

## Vérification sur la TV

Tester les flèches entre la barre supérieure, le bouton Ouvrir, les cartes et la rangée d'icônes. Les rangées défilent horizontalement ; le contenu défile verticalement si nécessaire. Vérifier que YouTube et Live TV s'ouvrent, que les applications désactivées n'apparaissent pas et que la recherche trouve une application présente.

Si un nom ou une icône inattendue apparaît, envoyer la photo de la rangée : ne pas désactiver un service système pour corriger un simple affichage.

## Retour arrière

Conserver tout le dossier de sauvegarde. Ouvrir PowerShell dans `sauvegarde-mitv-...`, puis :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ANNULER-MISE-A-JOUR.ps1
```

L'APK précédemment installé sera réinstallé avec les données conservées. La photo et le lanceur système ne sont pas modifiés. Le versionCode 1 et la même clé sont conservés pour permettre ces retours pendant le développement ; le versionName est 1.3. Ne pas supprimer la sauvegarde tant que les essais ne sont pas terminés.
