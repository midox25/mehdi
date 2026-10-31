# Mise à jour Barcelona 1.1

Interface inspirée de l'architecture de Barcelone : courbes asymétriques, touches de mosaïque trencadís, couleurs terre cuite, pierre et vert méditerranéen, typographie à empattements et photo personnelle en plein écran. YouTube et Live TV restent les seules applications principales. Les transitions restent de 120 ms, sans flou ni animation permanente.

## Sur votre PC Windows

1. Enregistrez la photo que vous avez choisie dans vos Images (clic droit sur l'image → Enregistrer l'image sous, ou télécharger la pièce jointe). Choisir un JPG/PNG paysage, idéalement 1920×1080. Une photo verticale ou 4:3 est recadrée au centre ; ses filigranes ne sont pas effacés.
2. Téléchargez et extrayez **BarcaHome-Barcelona-v1.1.zip**, dans un nouveau dossier.
3. Ouvrez PowerShell dans ce dossier et lancez :

```powershell
powershell -NoProfile -STA -ExecutionPolicy Bypass -File .\Mettre-a-jour-Barcelona.ps1
```

4. Une fenêtre s'ouvre : sélectionnez votre photo. Annuler ferme le script sans modifier la TV.
5. Le script sauvegarde l'APK actuellement installé et le fond externe s'il existe, crée le script d'annulation, puis installe la nouvelle version et transfère votre photo. Il ouvre l'application sans modifier le choix d'accueil par défaut.
6. Vérifiez la lisibilité, le focus à la télécommande et l'ouverture des deux applications. Envoyez une photo de la TV pour vérifier le rendu.

L'image que vous choisissez reste sur votre PC et votre TV : elle n'est ni envoyée au développeur ni publiée sur GitHub. Le ZIP ne contient pas de photo de Barcelone ; il contient un fond architectural dessiné de secours.

## Annuler la mise à jour

Dans le dossier `sauvegarde-barcelona-...` créé à côté du script, ouvrez PowerShell et lancez :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ANNULER-MISE-A-JOUR.ps1
```

Cela réinstalle l'APK sauvegardé et restaure l'ancien fond externe ou retire la nouvelle copie si aucun fond externe n'existait. Les autres applications, les réglages système et le choix de l'accueil par défaut ne sont pas modifiés. L'original de votre photo reste intact. Conservez le dossier complet de sauvegarde.

Cette version de développement garde volontairement le versionCode 1 et la même clé de signature que la 1.0 pour permettre la réinstallation réversible sans désinstaller ni effacer les données de l'application. Le versionName affiché dans les informations est 1.1. Ce mécanisme est prévu pour cet essai local et pas pour une publication Play Store.

Compilation, signature, compatibilité du certificat, absence de permissions et syntaxe PowerShell vérifiées. La mise à jour et l'annulation ont été exécutées avec un ADB simulé ; l'exécution réelle sur votre TV et la sélection de fichiers Windows restent à vérifier.
