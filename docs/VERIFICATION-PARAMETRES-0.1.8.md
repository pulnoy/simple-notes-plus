# Vérification des paramètres — Simple Notes+ 0.1.8

## Corrections
- 163 textes français et 6 groupes de pluriels ajoutés : aucune chaîne de base ne manque désormais dans la traduction française.
- Formats numériques et paramètres des messages contrôlés, sans doublons.
- Noms des langues affichés selon la langue de l’application.
- Journal des modifications disponible en français pour Simple Notes+.
- Régression corrigée : le réglage des icônes contrôle à nouveau le type de note et l’épinglage.
- Régression corrigée : en vue liste, la longueur d’aperçu utilise le réglage prévu pour cette vue.
- Le choix tiroir/vue dossiers est inclus dans les sauvegardes des paramètres et relu après restauration.

## Contrôles effectués
| Partie | Vérification |
|---|---|
| Apparence | Palettes jaune, bleue et dynamique, ouverture du tiroir par bouton et glissement, choix de la vue dossiers et conservation du réglage |
| Cartes des notes | Masquage et affichage des dates et des icônes ; titre uniquement ; longueur d’aperçu distincte entre grille et liste ; titre personnalisé |
| Éditeur | Conservation de l’enregistrement automatique, de la position de saisie, du retour en haut des listes et du compteur de mots ; lecture des réglages au démarrage de l’éditeur |
| Images | Traitement de fichiers réels dans les modes compressé, sans perte et original ; conservation de l’original et redimensionnement des fichiers traités |
| Sauvegarde | Création puis restauration en fusion sur les données de test ; conservation du choix de navigation des dossiers |
| Paramètres | Ouverture et retour depuis 14 sous-écrans, dont affichage, sécurité, synchronisation, sauvegarde, importation, corbeille, journal d’activité, langues et informations |
| Synchronisation et notifications | Conservation des déclencheurs, de l’intervalle, du Wi-Fi uniquement et des options de notification après réouverture |

Résultats : 1 082 tests unitaires réussis, 11 tests Android réussis et contrôle Detekt réussi.

## À confirmer sur téléphone
L’émulateur ne dispose pas de ton compte Google connecté. La connexion Drive, les notifications réellement reçues, le verrouillage biométrique et le fonctionnement en arrière-plan sur ton modèle de téléphone nécessitent un essai sur celui-ci. Les tests de cette version ne valident pas une connexion à un serveur WebDAV réel.

Installer Simple-Notes-Plus-0.1.8-test.apk par-dessus l’APK de test existant. Les données de l’émulateur ne sont pas incluses dans cet APK.