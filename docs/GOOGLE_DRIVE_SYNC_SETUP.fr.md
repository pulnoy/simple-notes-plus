# Mise en service de la synchronisation Google Drive

La variante Google Play de Simple Notes+ utilise l'espace privé `appDataFolder` de Google Drive. Elle demande uniquement le droit OAuth `https://www.googleapis.com/auth/drive.appdata` et n'accède pas aux autres fichiers du compte.

## Configuration Google Cloud

1. Créer un projet Google Cloud pour Simple Notes+ et activer l'API Google Drive.
2. Configurer l'écran de consentement OAuth avec le nom **Simple Notes+**, l'adresse de contact **pulnoy@protonmail.com** et le droit `drive.appdata`.
3. Créer un client OAuth **Android** avec le package `fr.mswgillian.simplenoteskeep` et l'empreinte **SHA-1 du certificat de signature de l'application Play**. Cette empreinte se trouve dans Play Console → Simple Notes+ → Protégé avec Play → Signature d'application → Clé de signature d'applications. Le certificat de clé d'importation est différent.
4. Pour tester une installation locale de la variante debug, créer un second client Android pour `fr.mswgillian.simplenoteskeep.debug` avec la SHA-1 de la clé debug de ce poste.
5. Ajouter les comptes des testeurs OAuth tant que l'écran de consentement est en mode test. Publier la configuration OAuth avant une distribution plus large.

## Validation avant une version Play

1. Installer la nouvelle version sur deux appareils de test connectés au même compte Google.
2. Activer Google Drive dans **Réglages → Sauvegarde et restauration** sur chacun.
3. Créer une note et une pièce jointe sur le premier appareil ; vérifier leur apparition sur le second.
4. Modifier la même note hors connexion sur les deux appareils ; vérifier que les deux versions sont conservées après reconnexion.
5. Supprimer une note sur un appareil ; vérifier sa suppression sur l'autre. Vérifier également qu'un dossier exclu de la synchronisation reste local.
6. Publier la politique de confidentialité mise à jour et revoir la déclaration « Sécurité des données » de Play Console avant la diffusion.

Le code reste sur la branche `feature/google-drive-sync` jusqu'à ce que la configuration OAuth et l'essai réel soient terminés.
