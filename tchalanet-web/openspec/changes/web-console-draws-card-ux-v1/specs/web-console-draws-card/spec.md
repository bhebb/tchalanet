# Spec: Web Console Draws Card

Portée : rendu carte de `ConsoleDrawsTableComponent` sous `down(expanded)` (< 840px). Le rendu
tableau desktop (`≥840px`) n'est pas couvert par ce document.

## ADDED Requirements

### Requirement: La carte répond d'abord à « quel tirage est-ce »

L'en-tête de carte SHALL afficher le code fonctionnel du tirage (ex. `NY-SOIR`) comme identifiant
principal, avec le libellé humain (ex. « New York · Soir ») juste en dessous.

L'en-tête SHALL NOT afficher les informations techniques du provider (nom brut du provider, heure
provider, fuseau horaire) — ces informations restent disponibles sur la page de détail du tirage.

#### Scenario: Le code fonctionnel prime sur le nom du provider

- **WHEN** la carte d'un tirage est affichée sous 840px
- **THEN** le code fonctionnel du tirage est le texte le plus proéminent de l'en-tête, et aucune
  ligne « Provider: » n'apparaît sur la carte.

### Requirement: L'heure locale reste toujours visible, jamais l'heure provider

La carte SHALL afficher l'heure locale (tenant) du tirage sous le badge d'état, quel que soit
l'état de vente — y compris à côté d'un compte à rebours sur un tirage ouvert. La carte SHALL NOT
afficher l'heure provider, quel que soit l'état du tirage.

> Révisé après retour utilisateur : la première version de cette règle réservait l'heure locale
> aux tirages fermés (masquée dès qu'un compte à rebours était affiché). Retour explicite : elle
> doit rester visible en permanence, le compte à rebours ne la remplace pas.

#### Scenario: Un tirage fermé reste situable dans le temps sans ouvrir le détail

- **WHEN** un tirage a les ventes fermées
- **THEN** la carte affiche l'heure locale du tirage sous le badge « Fermé », sans heure provider
  à proximité.

#### Scenario: Un tirage ouvert affiche le compte à rebours ET l'heure locale

- **WHEN** un tirage a les ventes ouvertes et un compte à rebours calculé
- **THEN** la carte affiche le badge « Ouvert », le compte à rebours, et l'heure locale — les deux
  informations coexistent, aucune ne remplace l'autre.

### Requirement: L'état de vente n'affiche que ce qui est actionnable

Un tirage aux ventes ouvertes SHALL afficher le badge d'état, le temps restant avant fermeture s'il
existe, et l'heure locale (couverte par la règle dédiée à l'heure locale ci-dessus).

Un tirage aux ventes fermées SHALL afficher le badge d'état et l'heure locale, sans bloc résultat
vide ni tiret de remplissage lorsqu'aucun résultat n'est encore disponible.

#### Scenario: Un tirage fermé sans résultat n'affiche pas de tiret

- **WHEN** un tirage a les ventes fermées et n'a ni résultat, ni numéros, ni indice de résultat
- **THEN** la carte n'affiche pas de bloc résultat vide (pas de `—` de remplissage).

### Requirement: Le résultat s'affiche sans titre de section

Quand un résultat existe (numéros ou statut CONFIRMED/PROVISIONAL/OVERRIDDEN/APPLIED), la carte
SHALL afficher le badge de statut de résultat et les numéros directement, sans libellé de section
("RÉSULTAT" ou équivalent) au-dessus.

#### Scenario: Un résultat confirmé s'affiche sans en-tête de section

- **WHEN** un tirage a un résultat confirmé avec des numéros
- **THEN** la carte affiche le badge « Confirmé » suivi des numéros, sans texte « RÉSULTAT »
  au-dessus.

### Requirement: L'absence de résultat se lit comme une action, pas un statut brut

Quand un tirage n'a pas encore de résultat, la carte SHALL afficher un message orienté action
plutôt que le libellé de statut technique brut (`Attendu`, `Manquant`) :

- « Résultat à saisir » si la saisie manuelle est actuellement permise pour ce tirage
  (équivalent de `canEnterManualResult`) ;
- « Résultat manquant » sinon.

Cette reformulation SHALL être propre au rendu carte : le tableau desktop et la page de détail du
tirage continuent d'afficher le libellé de statut existant (`consoleDrawResultStatusLabel`) sans
changement.

#### Scenario: Une saisie manuelle possible se lit comme une invite à agir

- **WHEN** un tirage n'a pas de résultat et que la saisie manuelle lui est actuellement permise
- **THEN** la carte affiche « Résultat à saisir » au lieu de « Attendu ».

#### Scenario: Une saisie non encore permise reste un statut d'attente

- **WHEN** un tirage n'a pas de résultat et que la saisie manuelle ne lui est pas encore permise
  (délai non atteint)
- **THEN** la carte affiche « Résultat manquant » au lieu de « Attendu » ou « Manquant » brut.

### Requirement: Une seule action principale reste visible, le reste va dans un menu

La carte SHALL afficher exactement une action principale en bouton plein, déterminée par l'état du
tirage (déjà calculée par la règle existante de `primaryAction()` — cette règle métier n'est pas
modifiée par cette exigence).

Les actions de cycle de vie (ouvrir, fermer, verrouiller, déverrouiller, annuler, archiver) SHALL
être regroupées dans un menu contextuel (`⋮`), jamais rendues comme boutons à plat à côté de
l'action principale.

Chaque item de ce menu SHALL porter à la fois une icône et un libellé texte — jamais l'icône seule.

#### Scenario: L'action principale reste seule visible

- **WHEN** un tirage a une action principale et une ou plusieurs actions de cycle de vie disponibles
- **THEN** seule l'action principale est visible en bouton plein ; les autres n'apparaissent qu'après
  ouverture du menu `⋮`.

#### Scenario: Un tirage sans action de cycle de vie n'affiche pas de menu vide

- **WHEN** un tirage n'a aucune action de cycle de vie disponible (ex. utilisateur sans droit de
  gestion du cycle de vie)
- **THEN** le bouton `⋮` n'est pas rendu.

#### Scenario: Une action de menu ne s'appuie jamais sur l'icône seule

- **WHEN** le menu `⋮` est ouvert sur une carte
- **THEN** chaque action listée affiche son libellé texte (ex. « Verrouiller ») à côté de son icône.

### Requirement: Toutes les cartes suivent le même ordre de blocs

Chaque carte SHALL présenter ses blocs dans l'ordre : identité, état de vente, résultat (si
disponible), action principale — sans variation d'ordre d'une carte à l'autre selon son état.

#### Scenario: L'ordre des blocs ne dépend pas de l'état du tirage

- **WHEN** on compare une carte de tirage ouvert sans résultat et une carte de tirage fermé avec
  résultat confirmé
- **THEN** les deux suivent le même ordre de blocs — seul le contenu de chaque bloc diffère, jamais
  sa position relative.
