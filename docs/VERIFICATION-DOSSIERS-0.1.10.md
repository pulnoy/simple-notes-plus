# Vérification de Simple Notes+ 0.1.10

Date : 30 septembre 2026. Version de test : 0.1.10-debug, code 71.

## Utilisation

- Quarante icônes dans « Personnaliser », avec les couleurs existantes. Le sélecteur défile.
- Le tiroir est verrouillé par défaut. Le cadenas active ou désactive la réorganisation.
- Déverrouiller, maintenir une poignée puis glisser vers un autre dossier du même niveau. Les sous-dossiers restent rattachés à leur parent.
- Verrouiller après le déplacement. Cet état est mémorisé sur l'appareil ; la navigation et la personnalisation restent disponibles.
- Le déplacement change l'ordre des dossiers, sans changer leur parent.

## Contrôles réalisés

- Compilation de l'APK et analyse Detekt : réussies.
- 1 091 tests unitaires : aucun échec.
- 14 tests Android sur émulateur Android 16 : réussis, en 99 secondes.
- Déplacement dans les deux sens, annulation sans changement d'ordre, nouveau déplacement après annulation, verrouillage et réouverture du tiroir.
- Création, renommage, personnalisation, sous-dossiers et suppression avec conservation des notes.
- Aperçus des photos et mémos vocaux, cohérence des thèmes et options des paramètres.
- Sauvegarde/restauration des notes, médias et métadonnées des dossiers.
- Captures du tiroir verrouillé et déverrouillé inspectées visuellement.

Deux corrections découvertes pendant ces contrôles : recréer un dossier supprimé utilise le nouveau parent choisi ; une sauvegarde qui remplace un fichier plus long le tronque correctement.

## APK

Fichier : `versions-test/Simple-Notes-Plus-0.1.10-test.apk` dans le dossier de travail.

Package : `fr.mswgillian.simplenoteskeep.debug`. Signature de test conservée pour le client OAuth existant.

SHA-256 du fichier : `2C7A45FC0C88A1E3911E9FAE1BB332B306F141EAE829D91553FD262A5A9D660A`.

Cette version est livrée comme APK de test. La connexion réelle à Drive sur téléphone et la synchronisation entre deux appareils n'ont pas été rejouées pour cette modification. Aucune publication Google Play effectuée.
