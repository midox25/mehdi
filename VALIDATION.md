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
