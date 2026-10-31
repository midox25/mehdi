# Validation — 2026-10-31

Effectué dans le cloud :

- Compilation Java et génération DEX avec JDK 21, plateforme Android 35, build-tools 35.0.0.
- Ressources et manifeste compilés avec aapt2.
- Signature APK v2/v3 vérifiée avec apksigner ; alignement vérifié avec zipalign.
- Métadonnées APK inspectées : minSdk 26, orientation paysage, activité Leanback, activité Home exportée et aucune permission demandée.
- Deux compilations réussies depuis les sources, dont une dans le dépôt /workspace/mehdi.
- SDK et JDK téléchargés depuis leurs distributions officielles avec vérification des sommes de contrôle publiées.

Non effectué :

- Exécution Android sur émulateur ou TV physique.
- Installation PowerShell sur Windows.
- Vérification visuelle du focus, des transitions, des photos et du lancement Live TV sur ce firmware.
- Changement de l'accueil par défaut.

L'application est une première version de développement signée. Le guide décrit l'essai physique et l'annulation. Ne pas désactiver l'accueil Xiaomi pour la tester. La clé de développement est conservée hors du dépôt et du paquet livré.

## Barcelona 1.1

- Compilation, signature v2/v3 et alignement vérifiés ; certificat identique à la version 1.0.
- versionName 1.1, versionCode 1 pour la réinstallation réversible de développement.
- Aucune permission demandée dans le manifeste compilé.
- Scripts Windows analysés avec le parseur officiel PowerShell 7.5.4 ; aucune erreur de syntaxe ni affectation à HOME/PID/Host.
- Mise à jour et script d’annulation exécutés dans PowerShell avec ADB et sélecteur de fichier simulés : sauvegarde et annulation présentes avant installation.
- La photo réelle et l’affichage 1.1 n’ont pas été testés sur la TV. Le sélecteur Windows réel n’a pas été exécuté.

## Camp Nou 1.2

- Build effectué après nettoyage des classes générées ; ressources, Java et DEX compilés avec succès.
- Signature, alignement, certificat compatible et versionName 1.2 vérifiés.
- Aucune permission demandée par le manifeste compilé.
- Script PowerShell Camp Nou analysé et exécuté avec un ADB simulé : sauvegarde et annulation avant installation, exécution de l'annulation, arrêt sur échec d'installation.
- Les commandes simulées ne modifient ni la photo ni le choix d'accueil par défaut.
- Navigation Android et rendu du stade non testés sur appareil/émulateur ; validation physique à effectuer par l'utilisateur.
