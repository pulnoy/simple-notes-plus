# Test de Simple Notes+ 0.1.5

Installer l’APK de test 0.1.5 sur les deux appareils. Il met à jour l’application de test existante (`fr.mswgillian.simplenoteskeep.debug`) et s’affiche à côté de la version Play Store. Vérifier la version dans À propos.

## État de synchronisation

1. Sur les deux appareils, ouvrir Synchronisation et notifications et connecter le même compte Google déjà autorisé pour les tests.
2. Sur A, créer une note « Essai école » et appuyer sur Synchroniser. Attendre la dernière réussite avec sa date et son heure.
3. Sur B, synchroniser : la note doit apparaître. Fermer puis rouvrir l’application : la dernière réussite doit rester affichée.
4. Sur A, couper Internet et modifier la note. L’état doit indiquer une attente de connexion, sans remplacer la date de la dernière réussite.
5. Rétablir Internet et synchroniser. Sur B, synchroniser et vérifier la modification.
6. Si l’accès Google expire, utiliser Vérifier le compte puis Reconnecter Google Drive dans les paramètres.

## Corbeille et restauration

1. Sur A, supprimer la note d’essai, laisser passer le délai Annuler, puis synchroniser.
2. Ouvrir Corbeille depuis l’accueil : la note doit y figurer et la durée par défaut doit être 30 jours.
3. Sur B, synchroniser : la note doit disparaître de la liste principale et apparaître dans Corbeille.
4. Sur B, restaurer la note puis synchroniser. Sur A, synchroniser : la note doit réapparaître avec son texte et ses pièces jointes.
5. Pour tester la suppression définitive, utiliser uniquement cette note d’essai et confirmer sa suppression dans Corbeille. Après synchronisation sur les deux appareils, elle doit disparaître des deux corbeilles.

La limite de 30 jours est couverte par les tests automatisés existants ; il n’est pas nécessaire de changer l’horloge du téléphone.

## Widgets

1. Épingler la note d’essai dans l’application.
2. Appuyer sur Widgets à l’accueil de l’application. La note épinglée doit apparaître en premier.
3. Choisir cette note et confirmer l’ajout proposé par Android. Le widget doit afficher la note choisie.
4. Modifier la note et vérifier la mise à jour du widget. Modifier sur l’autre appareil et synchroniser : le widget doit se mettre à jour aussi.
5. Ajouter le widget Note / liste rapide. Tester les boutons de création, puis redimensionner le widget.
6. Si le lanceur du téléphone ne propose pas l’ajout direct : appui long sur un espace vide de l’accueil du téléphone → Widgets → Simple Notes+ Test.

## Recherche

1. Créer des notes de texte, une liste avec « Réserver le train », un dessin et un audio. Ranger certaines notes dans un dossier « Vacances ».
2. Appuyer sur Rechercher. Chercher « ecole » doit retrouver « école » ; chercher « vacances reserver » doit retrouver la liste.
3. Tester Texte, Listes, Dessins / images et Audio. Une note avec dessin et audio doit apparaître dans les deux filtres correspondants.
4. Choisir un dossier, puis Sans dossier et Tous les dossiers. Les résultats doivent suivre le choix.
5. Effacer le texte et remettre Tous et Tous les dossiers pour retrouver l’affichage habituel. La corbeille ne doit pas apparaître dans les résultats.

## Limite de validation sur ce poste

Aucun appareil Android n’était connecté pendant le développement. Les contrôles de compilation, de qualité et les tests automatiques ne remplacent pas ce parcours sur téléphone, notamment pour l’ajout de widgets propre à chaque lanceur.
