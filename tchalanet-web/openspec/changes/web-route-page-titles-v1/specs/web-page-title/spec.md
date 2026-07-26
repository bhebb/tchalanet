# Spec: Web Page Title

## ADDED Requirements

### Requirement: Chaque route nomme sa page dans le titre du document

`document.title` SHALL refléter la route active, sous la forme `<page> · <marque>`. La marque SHALL
être le `<title>` déclaré dans `index.html`, capturé au démarrage.

Le titre de page SHALL être résolu dans cet ordre :

1. `data.titleKey` de la route active la plus profonde ;
2. à défaut, le `labelKey` de la destination de navigation la plus spécifique qui préfixe l'URL.

Quand aucune des deux sources ne s'applique, le titre SHALL être la marque seule.

#### Scenario: Une route déclarant sa clé nomme sa page

- **WHEN** l'utilisateur navigue vers une route portant `data: { titleKey }`
- **THEN** l'onglet affiche le libellé traduit de cette clé, suivi de la marque.

#### Scenario: Une page atteinte par le menu hérite du libellé du menu

- **WHEN** l'utilisateur navigue vers une URL couverte par une destination du modèle de navigation
  et que la route ne déclare aucune clé
- **THEN** l'onglet affiche le libellé de cette entrée de menu, en retenant la destination la plus
  spécifique lorsque plusieurs correspondent.

#### Scenario: Deux écrans ne portent pas le même titre

- **WHEN** l'utilisateur ouvre deux écrans distincts du même portail
- **THEN** les deux onglets portent des titres différents.

### Requirement: Le titre suit la langue

Le titre SHALL être réappliqué lorsque la langue change, et lorsque les traductions finissent de
charger après la première navigation.

#### Scenario: Changement de langue

- **WHEN** l'utilisateur change de langue depuis une page nommée
- **THEN** le titre de l'onglet est réécrit dans la nouvelle langue, sans navigation.

### Requirement: Aucune clé i18n brute n'atteint l'utilisateur

Une clé sans traduction SHALL être traitée comme une absence de titre : le titre retombe sur la
marque seule.

#### Scenario: Traduction manquante

- **WHEN** une route déclare une `titleKey` absente des bundles
- **THEN** l'onglet affiche la marque seule, jamais la clé.
