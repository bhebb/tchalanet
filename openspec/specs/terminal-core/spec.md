# Spec — Package `core.terminal` & class rename (`SalesSession*`)

> Capability introduite par la change `rename-pos-to-terminal` (2026-04-28).
> Remplace l'ancien package `core.pos` et les classes `PosSession*`.

---

## Requirements

### Requirement: Package `core.terminal` remplace `core.pos`

Le codebase NE DOIT PAS contenir de classe sous le package `com.tchalanet.server.core.pos`.
Toutes les classes auparavant sous `core.pos.**` DOIVENT être déplacées sous `core.terminal.**`,
en conservant la même structure de sous-packages.

#### Scenario: Package core.pos absent

- **WHEN** l'arborescence source complète est scannée pour `com.tchalanet.server.core.pos`
- **THEN** zéro fichier Java est trouvé sous ce package

#### Scenario: Sous-packages préservés

- **WHEN** une classe était à `core.pos.domain.model.Terminal`
- **THEN** elle est maintenant à `core.terminal.domain.model.Terminal` avec un contenu identique sauf la déclaration `package`

#### Scenario: Compilation réussie

- **WHEN** `./mvnw compile` est exécuté après le renommage
- **THEN** exit code est 0 sans erreur de symbole non résolu

---

### Requirement: Classes dans `core.session` renommées de `PosSession*` vers `SalesSession*`

Les renommages suivants DOIVENT être appliqués dans `core.session` :

| Ancien nom                | Nouveau nom                 | Localisation                           |
| ------------------------- | --------------------------- | -------------------------------------- |
| `PosSession`              | `SalesSession`              | `core.session.domain.model`            |
| `PosSessionView`          | `SalesSessionView`          | `core.session.application.query.model` |
| `PosSessionRepoPort`      | `SalesSessionRepoPort`      | `core.session.application.port.out`    |
| `PosSessionResponse`      | `SalesSessionResponse`      | `core.session.infra.web.model`         |
| `PosSessionController`    | `SalesSessionController`    | `core.session.infra.web`               |
| `PosSessionMapper`        | `SalesSessionMapper`        | `core.session.infra.web`               |
| `PosSessionJpaEntity`     | `SalesSessionJpaEntity`     | `core.session.infra.persistence`       |
| `JpaPosSessionRepository` | `JpaSalesSessionRepository` | `core.session.infra.persistence`       |

Aucune classe nommée `PosSession*` NE DOIT rester en source de production.

#### Scenario: Aucune classe PosSession ne subsiste

- **WHEN** l'arborescence de production est recherchée pour les classes nommées `PosSession*`
- **THEN** zéro correspondance est trouvée

#### Scenario: SalesSessionController est le contrôleur actif

- **WHEN** l'application démarre et `/tenant/sessions` est requêté
- **THEN** `SalesSessionController` traite la requête

---

### Requirement: Tous les imports mis à jour pour refléter les nouveaux noms

Chaque fichier source Java du projet DOIT importer depuis `core.terminal.*` (pas `core.pos.*`)
et DOIT référencer les nouveaux noms de classe `SalesSession*`.

#### Scenario: Aucun import stale ne subsiste

- **WHEN** l'arborescence source complète est grepped pour `import com.tchalanet.server.core.pos.`
- **THEN** zéro occurrence est trouvée

#### Scenario: Aucun import PosSession stale ne subsiste

- **WHEN** l'arborescence source est grepped pour `import .*PosSession`
- **THEN** zéro occurrence est trouvée

---

### Requirement: Les identifiants stables NE SONT PAS renommés

Les éléments suivants DOIVENT rester inchangés :

- Classe typed ID `TerminalId`
- Classe typed ID `SessionId`
- Chemin HTTP `/tenant/sessions/*`
- Noms de champs `terminal_id`, `outlet_id` dans toutes les tables
- Noms d'actions d'audit (ex. `SESSION_OPEN`)

#### Scenario: TerminalId inchangé

- **WHEN** le fichier source de `TerminalId` est lu
- **THEN** le nom de classe et le package correspondent à l'état pré-change

---

### Requirement: Documentation mise à jour pour refléter les nouveaux noms

Tous les fichiers `.md` du dépôt qui référencent `core.pos`, `PosSession*` ou `pos_session` DOIVENT
être mis à jour dans la même change. Aucune référence de documentation stale NE DOIT subsister.

#### Scenario: Aucune référence doc stale ne subsiste

- **WHEN** tous les fichiers `.md` du dépôt sont grepped pour `core\.pos` et `PosSession`
- **THEN** zéro occurrence en dehors de `openspec/changes/` et des documents d'audit archivés

---

### Requirement: Classes de test renommées selon la même carte que les classes de production

Les classes de test DOIVENT suivre la même carte de renommage que les classes de production :

| Ancien nom                 | Nouveau nom                  |
| -------------------------- | ---------------------------- |
| `PosSessionControllerTest` | `SalesSessionControllerTest` |
| `PosSessionRepoPortTest`   | `SalesSessionRepoPortTest`   |
| `PosSessionMapperTest`     | `SalesSessionMapperTest`     |

Les fixtures référencées par ces tests DOIVENT également être renommées.

#### Scenario: Aucune classe de test PosSession ne subsiste

- **WHEN** l'arborescence de test est recherchée pour des fichiers nommés `PosSession*Test*`
- **THEN** zéro correspondance est trouvée

#### Scenario: Les tests passent après le renommage

- **WHEN** `./mvnw test` est exécuté
- **THEN** exit code est 0 ; aucun test n'est sauté ou cassé en raison du renommage
