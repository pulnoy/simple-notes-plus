# Vérification des dossiers — Simple Notes+ 0.1.9

## Version livrée

- Version : 0.1.9-debug, code 70.
- Installation de test distincte : fr.mswgillian.simplenoteskeep.debug.
- APK : Simple-Notes-Plus-0.1.9-test.apk dans versions-test.
- Signature de test conservée, compatible avec le client OAuth Google Drive déjà configuré.
- SHA-256 APK : DC74EBFDD5E69B62123EEE62257000DA8B28E881B8524943288E6B5DDBB2DB90.

## Fonctionnement

Dans le menu ⋮ de chaque dossier :
- Monter / Descendre réorganise les dossiers du même niveau.
- Icône et couleur de fond propose douze icônes, onze couleurs et une couleur automatique.
- Nouveau sous-dossier crée un dossier à l’intérieur du dossier choisi.
- Renommer conserve les enfants et leurs notes.
- Supprimer retire aussi les sous-dossiers et conserve leurs notes dans Toutes, avec annulation possible.

Quand un dossier est ouvert :
- Ses sous-dossiers immédiats apparaissent en tuiles, avec ses notes.
- Le bouton + propose Nouveau sous-dossier.
- La flèche revient au parent ; le chemin affiche les niveaux successifs.
- Le bouton Retour Android remonte également d’un niveau.

Les liens entre dossiers, les icônes, les couleurs et les positions sont inclus dans les sauvegardes et les métadonnées Drive / WebDAV. Les anciens dossiers restent à la racine.

## Contrôles réalisés

- Analyse Detekt : réussite sans modification du seuil de qualité.
- Compilation APK et APK de tests : réussite.
- 1 090 tests unitaires : aucune erreur.
- 13 tests Android sur émulateur Android 16 : réussite.
- Tests réels de création de deux niveaux de sous-dossiers, réorganisation depuis le menu, choix de l’icône et du jaune, navigation, renommage du parent et suppression avec conservation des notes.
- Sauvegarde locale puis restauration réelle des dossiers : parent, ordre, icône et couleur identiques.
- Transport JSON et fusion simulée entre deux appareils Drive : nouvelles métadonnées conservées dans les deux sens.
- Affichage visuellement vérifié à partir des captures subfolders-home.png et customized-drawer.png.
- Tests existants des paramètres, modes de navigation, thèmes, photos et mémos vocaux : réussite.

## Limites et essai sur téléphone

- Les noms de dossiers doivent encore être uniques dans toute l’application. Le dialogue signale un nom déjà utilisé.
- La réorganisation utilise Monter / Descendre ; il n’y a pas de glisser-déposer dans cette version.
- La hiérarchie est une organisation dans l’application. Les répertoires WebDAV restent compatibles avec le stockage existant.
- Installer 0.1.9 sur les deux appareils avant l’essai Drive : une ancienne version ne connaît pas ces nouvelles métadonnées.
- L’autorisation OAuth et un échange réel avec votre Drive n’ont pas été relancés sur téléphone pendant cette vérification. L’accès reste soumis à la configuration OAuth de test existante.
- L’APK est destiné au test ; aucune publication Google Play n’a été effectuée.

Pour l’essai Drive : créer un parent et un enfant sur le premier téléphone, choisir une icône et une couleur, synchroniser les deux téléphones, puis vérifier l’arborescence sur le second. Modifier l’ordre ou la couleur sur le second et synchroniser dans l’autre sens. Créer enfin une note dans l’enfant et vérifier qu’elle apparaît au même endroit sur les deux appareils.
