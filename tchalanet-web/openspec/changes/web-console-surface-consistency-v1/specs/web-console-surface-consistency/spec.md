# Web console surface consistency

## ADDED Requirements

### Requirement: Detail pages use a shared main and summary layout

Les pages de détail de la console MUST utiliser `tch-admin-page-shell` et
`tch-admin-detail-layout`. Le contenu principal et le résumé latéral MUST utiliser les primitives
de console et les tokens de thème partagés.

#### Scenario: Desktop detail page

- **GIVEN** une entité chargée avec un statut et des actions disponibles
- **WHEN** l’utilisateur ouvre sa page de détail sur desktop
- **THEN** l’identité, le statut et les actions sont visibles dans le shell
- **AND** les sections métier sont dans la zone principale
- **AND** le résumé et les métriques sont dans un panneau latéral cohérent avec les autres détails

#### Scenario: Mobile detail page

- **GIVEN** la même entité sur une largeur mobile
- **WHEN** la page est affichée
- **THEN** le panneau latéral passe sous le contenu principal
- **AND** aucune action ou valeur métier n’est masquée par débordement

### Requirement: Console tables use readable business identities and standard states

Les tableaux de la console MUST afficher une identité métier lisible en priorité, utiliser les
badges de statut partagés et distinguer les états loading, error, empty-data et empty-filter. Les
UUID internes MUST NOT être la seule identité visible dans une ligne métier.

#### Scenario: Draw report row

- **GIVEN** une ligne de rapport associée à un tirage connu
- **WHEN** le tableau est rendu
- **THEN** le code ou libellé du canal, la date et les valeurs financières sont visibles
- **AND** l’UUID reste masqué de la présentation principale ou réservé à une information technique

#### Scenario: Filter returns no rows

- **GIVEN** un filtre valide qui ne correspond à aucune ligne
- **WHEN** la réponse est chargée
- **THEN** le tableau reste stable
- **AND** un état « aucun résultat pour ces filtres » avec une action de réinitialisation est affiché

### Requirement: Console forms use shared validation and mutation feedback

Les formulaires de la console MUST utiliser une structure et des actions cohérentes. Les erreurs de
champ adressables MUST être attachées au champ, les erreurs restantes MUST apparaître dans un résumé
ou une erreur de section, et une mutation MUST exposer son état pending et son résultat.

#### Scenario: Valid form submission

- **GIVEN** un formulaire valide
- **WHEN** l’utilisateur enregistre
- **THEN** le bouton d’enregistrement est désactivé pendant la mutation
- **AND** un feedback de succès traduit est affiché
- **AND** les données visibles sont rechargées sans perdre le contexte de la page

#### Scenario: Server field validation error

- **GIVEN** le backend renvoie une erreur de validation liée à un champ
- **WHEN** la mutation échoue
- **THEN** le champ concerné reçoit l’erreur et le focus/état touché approprié
- **AND** l’utilisateur peut corriger la valeur sans perdre les autres champs
