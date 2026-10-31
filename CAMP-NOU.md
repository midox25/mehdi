# Camp Nou 1.2

Un sélecteur central inspiré du Camp Nou : enceinte ovale, gradins bleu/grenat et terrain vert. Deux icônes sont placées au milieu du terrain : le bouton rouge de lecture YouTube et un écran de télévision pour Live TV. Les pictogrammes sont dessinés en vectoriel dans l'application, avec leurs noms visibles.

- Gauche ou droite : passer entre YouTube et Live TV, avec retour circulaire à l'autre application.
- OK : ouvrir l'application sélectionnée.
- Bas : Personnaliser ; Haut revient à la dernière application sélectionnée.
- Menu : ouvrir la personnalisation directement.
- Le focus est signalé par une bordure dorée et un agrandissement de 120 ms.

L'image de fond déjà choisie reste intacte. Aucune publicité ni permission réseau n'est ajoutée. Le stade est une représentation graphique stylisée, pas une photo du Camp Nou.

## Mettre à jour depuis Windows

1. Télécharger et extraire [BarcaHome-CampNou-v1.2.zip](https://github.com/midox25/mehdi/raw/refs/heads/main/downloads/BarcaHome-CampNou-v1.2.zip) dans un nouveau dossier.
2. Ouvrir PowerShell dans ce dossier et exécuter :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Mettre-a-jour-CampNou.ps1
```

Le script vérifie SHA256, sauvegarde l'APK installé et prépare `sauvegarde-campnou-...\ANNULER-MISE-A-JOUR.ps1` avant l'installation. Il conserve les données, la photo et le choix de l'accueil par défaut. Il ne désactive aucune autre application.

3. Sur la TV, vérifier : deux icônes centrées ; focus gauche/droite ; ouverture de YouTube et de Live TV ; accès aux réglages avec Bas ; retour avec Haut sur la dernière sélection. Envoyer une photo pour vérifier le rendu réel.

Pour une première installation, utiliser `Installer-Windows.ps1` à la place : il conserve et sauvegarde l'accueil Xiaomi. Le script Camp Nou exige une installation précédente de notre application.

## Retour à la version précédente

Conserver le dossier de sauvegarde complet. Dans ce dossier, exécuter :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ANNULER-MISE-A-JOUR.ps1
```

L'APK sauvegardé est réinstallé sans effacer les données ni toucher à la photo. Cette version de développement conserve versionCode 1 et le certificat des versions 1.0/1.1 ; le versionName est 1.2, ce qui permet le retour sans désinstallation. Aucune publication Play Store n'est effectuée.

## Vérifications

Compilation, signature, alignement, absence de permissions et compatibilité du certificat vérifiés. Le script de mise à jour et son annulation ont été exécutés avec un ADB simulé, et l'échec d'installation arrête la procédure. L'affichage du stade et la navigation réelle restent à tester sur la TV ; aucun test Android sur émulateur ou appareil physique n'a été exécuté pour la version 1.2.
