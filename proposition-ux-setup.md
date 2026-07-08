# Proposition UX/UI — Configuration du tenant (`/app/admin/setup`)

Ce document accompagne la maquette `proposition-ux-setup.html`. Il explique le problème central identifié et les cinq changements proposés pour le corriger, sans toucher au contenu ni à la structure existante de la page — uniquement au traitement visuel du statut.

## Le problème central

Sur la page actuelle, toutes les cartes d'étape partagent le même fond jaune pâle, qu'elles soient déjà complétées (coche verte) ou non. Le jaune est une couleur qui signale habituellement « attention » ou « à faire », donc son usage uniforme entre en conflit avec le message de la coche verte et oblige l'utilisateur à lire chaque carte individuellement pour savoir ce qu'il reste à faire, alors que l'objectif d'une checklist d'onboarding est justement de rendre ce statut visible d'un coup d'œil.

## Changements proposés

1. **Le fond de carte suit le statut, pas la marque.** Une carte terminée passe en fond blanc avec une bordure verte discrète ; le jaune/or reste réservé aux cartes réellement incomplètes. Le jaune retrouve ainsi sa fonction de signal au lieu d'être un simple habillage décoratif.
2. **Un seul badge de statut, pas deux icônes.** La coche de statut devient un petit badge en overlay sur l'icône de catégorie (coin bas-droit), plutôt que deux icônes juxtaposées sans lien visuel évident. Sur une carte incomplète, ce badge affiche un point neutre ou un « ! » au lieu d'une coche, pour bien distinguer les deux états.
3. **Le bloc « Vendeurs » rejoint le design des cartes.** Même rayon de bordure, même style d'icône catégorie, même logique de couleur que le reste de la grille — au lieu d'un bandeau au style différent qui rompt la cohérence en bas de page.
4. **Le badge « Requis » gagne en contraste tant que l'étape n'est pas terminée.** Fond marine / texte or sur une carte encore incomplète pour bien marquer l'obligation ; une fois l'étape terminée, le badge passe en vert clair pour confirmer que l'obligation est remplie.
5. **Le bandeau « Support: famille »** est remplacé par un vrai composant interrupteur (toggle visuel) au lieu d'une pilule « ON » sans affordance de clic claire.

## Ce qui ne change pas

La structure de l'information (titre, description, bouton par carte), la disposition en grille, la navigation latérale et le contenu texte restent identiques. La proposition est volontairement limitée au système de couleur/statut, qui est le point qui nuit le plus à la lisibilité actuelle.

## Suite possible

Si ce sens de correction est validé, l'implémentation réelle se ferait dans `tchalanet-web` en s'appuyant sur les composants existants du design system (`@tch/ui/console`, `TchStatusBadge`) plutôt qu'en CSS ad hoc, en suivant le workflow OpenSpec du projet pour une page de cette nature (écran console / admin).
