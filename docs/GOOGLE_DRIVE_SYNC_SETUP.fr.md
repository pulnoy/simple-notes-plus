# Mise en service de la synchronisation Google Drive

La variante Google Play de Simple Notes+ utilise l'espace privé `appDataFolder` de Google Drive. Elle demande uniquement le droit OAuth `https://www.googleapis.com/auth/drive.appdata` et n'accède pas aux autres fichiers du compte.

## Configuration Google Cloud

1. Projet Google Cloud créé : **Simple Notes Plus Sync**, ID `simple-notes-plus-sync` (numéro `455460919324`). L'API Google Drive y est activée.
2. Branding OAuth créé avec le nom **Simple Notes+**, l'adresse de contact **pulnoy@protonmail.com** et une audience externe. Le droit `drive.appdata` a été enregistré le 28 septembre 2026.
3. Client OAuth **Android** créé : **Simple Notes+ Google Play**, ID `455460919324-jl8cv9i2nmc8fndkfc7tavcf998eehbu.apps.googleusercontent.com`, package `fr.mswgillian.simplenoteskeep` et empreinte **SHA-1 du certificat de signature de l'application Play** : `77:40:6C:F7:DE:B8:60:F5:9F:A4:20:DE:DA:B4:66:57:6A:E5:46:76`. Cette empreinte provient de Play Console → Simple Notes+ → Protégé avec Play → Signature d'application → Clé de signature d'applications. Le certificat de clé d'importation est différent.
4. Client de test APK créé : **Simple Notes+ Test APK**, ID `455460919324-maiqfd0eipoc3mljca77cuvue66jkdsl.apps.googleusercontent.com`, package `fr.mswgillian.simplenoteskeep.debug`, SHA-1 `9E:60:D1:37:19:34:14:09:4A:FF:E0:A3:E6:6A:F8:F5:F2:E5:81:FA`. La version `0.1.3-debug` s'installe séparément sous le nom **Simple Notes+ Test**. Les deux appareils doivent installer le même APK signé avec cette clé.
5. L'écran de consentement reste en mode test ; `pulnoy@gmail.com` est ajouté comme utilisateur test. Ajouter les autres comptes de test si nécessaire. Publier la configuration OAuth avant une distribution plus large.

La [politique de confidentialité](https://github.com/pulnoy/simple-notes-plus/blob/main/docs/PRIVACY_POLICY.fr.md) incluant Google Drive a été publiée sur `main` le 28 septembre 2026 (commit `3e645a8dec1f6316b72a63b4b7581f27ec12806b`).

## Validation avant une version Play

1. Installer la nouvelle version sur deux appareils de test connectés au même compte Google.
2. Activer Google Drive dans **Réglages → Synchronisation et notifications** sur chacun.
3. Créer une note et une pièce jointe sur le premier appareil ; vérifier leur apparition sur le second.
4. Modifier la même note hors connexion sur les deux appareils ; vérifier que les deux versions sont conservées après reconnexion.
5. Supprimer une note sur un appareil ; vérifier sa suppression sur l'autre. Vérifier également qu'un dossier exclu de la synchronisation reste local.
6. Publier la politique de confidentialité mise à jour et revoir la déclaration « Sécurité des données » de Play Console avant la diffusion.

Le code reste sur la branche `feature/google-drive-sync` jusqu'à ce que la configuration OAuth et l'essai réel soient terminés.
