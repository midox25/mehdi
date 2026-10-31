# Mi TV Home 1.4 — horloge et vidéos

## Installation Windows

Télécharger et extraire [BarcaHome-Videos-v1.4.zip](https://github.com/midox25/mehdi/raw/refs/heads/main/downloads/BarcaHome-Videos-v1.4.zip), ouvrir PowerShell dans ce nouveau dossier et exécuter :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Mettre-a-jour-Videos.ps1
```

La version actuellement installée est sauvegardée avant remplacement. Le script conserve les données, la photo, le lanceur par défaut et tous les réglages système. Une procédure `sauvegarde-videos-...\ANNULER-MISE-A-JOUR.ps1` est créée avant l'installation.

## Heure moins une heure

L'horloge de l'accueil affiche par défaut l'heure de la TV moins 60 minutes. Exemple : 14:45 devient 13:45 et 00:15 devient 23:15. Aucun changement de l'heure Android, du fuseau ou de la synchronisation automatique.

Pour revenir à l'heure sans décalage : Menu → Décalage de l'horloge → Heure TV sans décalage. Le choix reste local à cette application et peut être inversé à tout moment.

## Suggestions de vidéos

La rangée « Vidéos à découvrir · sélection locale » remplace les anciennes cartes de raccourcis d'applications. Les icônes des applications disponibles restent dans la rangée suivante.

Deux vidéos sont proposées au départ : Big Buck Bunny et Sintel, projets Blender. Ce sont des liens locaux sélectionnés, pas des recommandations personnalisées ni une lecture de l'historique. Les cartes utilisent un visuel de lecture dessiné ; aucune miniature n'est téléchargée.

- Flèches : sélectionner une carte et afficher son titre dans la zone principale.
- OK ou Regarder : transmettre son lien à l'application YouTube TV.
- Ajouter une vidéo : saisir un titre et un lien YouTube HTTPS (youtube.com ou youtu.be).
- Menu → Vidéos suggérées : modifier, retirer, annuler la dernière modification ou rétablir les deux suggestions de départ.

Jusqu'à 12 vidéos. Aucun compte ou mot de passe n'est lu. Les titres, liens et une copie de la sélection précédente sont conservés uniquement dans les préférences de l'application. Les liens sont normalisés en URL YouTube et ne sont jamais lancés dans un navigateur tiers. La rangée est masquée si YouTube n'est pas disponible.

YouTube peut afficher « Qui regarde ? », demander une connexion ou signaler une vidéo indisponible. L'utilisateur gère ces écrans lui-même ; aucune lecture automatique ou disponibilité des vidéos n'est garantie.

## Annulation

Ouvrir PowerShell dans le dossier `sauvegarde-videos-...` et exécuter :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ANNULER-MISE-A-JOUR.ps1
```

L'ancienne interface et son affichage de l'heure sont rétablis en réinstallant l'APK sauvegardé. Les nouveaux paramètres locaux restent dans les données mais sont ignorés par les versions précédentes. Pour annuler uniquement le décalage, utiliser le menu de l'horloge ; pour annuler une modification des vidéos, utiliser l'option dédiée.

## Vérification

26 contrôles JVM passent : catalogue des applications (8), liens vidéo et décalage de l'horloge (18). Compilation, signature, alignement et compatibilité du certificat vérifiés. Le script de mise à jour, son annulation et son arrêt sur échec ont été exécutés avec un ADB simulé. Les dialogues Android, le rendu et l'ouverture des liens YouTube sur la TV restent à tester physiquement. Aucune permission réseau n'est ajoutée au lanceur.
