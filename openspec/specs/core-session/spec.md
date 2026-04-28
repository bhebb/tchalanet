# Spec — Tables `sales_session` & `sales_session_totals`

> Capability introduite par la change `rename-pos-to-terminal` (2026-04-28).
> Renommage de tables uniquement, pas de migration de données.
> DB recréée from scratch : les fichiers de migration existants sont édités in-place.
> Aucun nouveau fichier Flyway n'est ajouté pour ce renommage.

---

## Scope

Tables concernées :

| Ancien nom               | Nouveau nom                | Fichier de migration   |
| ------------------------ | -------------------------- | ---------------------- |
| `pos_session`            | `sales_session`            | `V8__core_pos.sql`     |
| `pos_session_totals`     | `sales_session_totals`     | `V8__core_pos.sql`     |
| `pos_session_aud`        | `sales_session_aud`        | `V43__audit_table.sql` |
| `pos_session_totals_aud` | `sales_session_totals_aud` | `V43__audit_table.sql` |

Autres fichiers édités in-place : `V40__rls_policies.sql`, `V35__seed_outlet_terminal_pos.sql`.

---

## Requirements

### Requirement: La table `sales_session` est la table de persistance canonique des sessions

Carte de renommage pour `pos_session` :

| Ancien objet                       | Nouvel objet                         |
| ---------------------------------- | ------------------------------------ |
| `pos_session`                      | `sales_session`                      |
| `ux_pos_session_open_per_terminal` | `ux_sales_session_open_per_terminal` |
| `ix_pos_session_tenant_terminal`   | `ix_sales_session_tenant_terminal`   |
| `ix_pos_session_tenant_opened_at`  | `ix_sales_session_tenant_opened_at`  |
| `trg_pos_session_updated_at`       | `trg_sales_session_updated_at`       |

#### Scenario: Table existe sous le nouveau nom

- **WHEN** une DB fraîche est créée et toutes les migrations sont exécutées
- **THEN** `SELECT 1 FROM sales_session LIMIT 1` réussit

#### Scenario: Ancien nom de table n'existe pas

- **WHEN** une DB fraîche est créée et toutes les migrations sont exécutées
- **THEN** `SELECT 1 FROM pos_session LIMIT 1` retourne une erreur (relation n'existe pas)

#### Scenario: Trigger renommé

- **WHEN** les triggers sur `sales_session` sont listés
- **THEN** `trg_sales_session_updated_at` existe et `trg_pos_session_updated_at` n'existe pas

---

### Requirement: La table `sales_session_totals` remplace `pos_session_totals`

La FK `session_id` DOIT pointer vers `sales_session(id)`.

Carte de renommage pour `pos_session_totals` :

| Ancien objet                           | Nouvel objet                             |
| -------------------------------------- | ---------------------------------------- |
| `pos_session_totals`                   | `sales_session_totals`                   |
| `ux_pos_session_totals_session_id`     | `ux_sales_session_totals_session_id`     |
| `ix_pos_session_totals_tenant`         | `ix_sales_session_totals_tenant`         |
| `ix_pos_session_totals_tenant_session` | `ix_sales_session_totals_tenant_session` |
| `trg_pos_session_totals_updated_at`    | `trg_sales_session_totals_updated_at`    |

#### Scenario: Table enfant existe sous le nouveau nom

- **WHEN** une DB fraîche est créée et toutes les migrations sont exécutées
- **THEN** `SELECT 1 FROM sales_session_totals LIMIT 1` réussit

#### Scenario: FK référence le nouveau parent

- **WHEN** le schéma de `sales_session_totals` est inspecté
- **THEN** `session_id` référence `sales_session(id)` ON DELETE CASCADE ; aucune référence à `pos_session` ne subsiste

---

### Requirement: Tables d'audit Envers renommées dans `V43__audit_table.sql`

Carte de renommage pour les tables d'audit :

| Ancienne table           | Nouvelle table             |
| ------------------------ | -------------------------- |
| `pos_session_aud`        | `sales_session_aud`        |
| `pos_session_totals_aud` | `sales_session_totals_aud` |

Renommages de contraintes dans `sales_session_aud` :

| Ancienne contrainte            | Nouvelle contrainte              |
| ------------------------------ | -------------------------------- |
| `pos_session_aud_pkey`         | `sales_session_aud_pkey`         |
| `pos_session_aud_status_check` | `sales_session_aud_status_check` |

Renommages de contraintes dans `sales_session_totals_aud` :

| Ancienne contrainte           | Nouvelle contrainte             |
| ----------------------------- | ------------------------------- |
| `pos_session_totals_aud_pkey` | `sales_session_totals_aud_pkey` |

#### Scenario: Tables d'audit existent sous les nouveaux noms

- **WHEN** une DB fraîche est créée et toutes les migrations sont exécutées
- **THEN** `SELECT 1 FROM sales_session_aud LIMIT 1` et `SELECT 1 FROM sales_session_totals_aud LIMIT 1` réussissent

---

### Requirement: Liste de politiques RLS mise à jour dans `V40__rls_policies.sql`

Le littéral `'pos_session'` DOIT être renommé en `'sales_session'`.

#### Scenario: Politique RLS active sur sales_session

- **WHEN** les politiques RLS sur `sales_session` sont listées après migration
- **THEN** la politique d'isolation tenant existe sur `sales_session`

---

### Requirement: Fichier seed mis à jour dans `V35__seed_outlet_terminal_pos.sql`

Toutes les références à `pos_session` (INSERT, SELECT, RAISE NOTICE, commentaires) DOIVENT être renommées en `sales_session`.

#### Scenario: Seed s'exécute sans erreur

- **WHEN** toutes les migrations s'exécutent sur une DB fraîche avec le tenant `tchalanet` présent
- **THEN** le seed insère une ligne dans `sales_session` sans erreur

---

### Requirement: Aucune référence à `pos_session` ne subsiste dans les fichiers de migration

Après l'édition in-place, aucun fichier SQL dans `src/main/resources/db/migration` NE DOIT contenir
le token `pos_session`.

#### Scenario: Fichiers de migration propres

- **WHEN** tous les fichiers `.sql` sous `src/main/resources/db/migration` sont grepped pour `pos_session`
- **THEN** zéro occurrence est trouvée

---

### Requirement: Aucun alias VIEW de compatibilité descendante

Aucune VIEW SQL nommée `pos_session` NE DOIT être créée comme shim de compatibilité.
Tout consommateur aval référençant `pos_session` DOIT être mis à jour directement.

#### Scenario: Aucune vue alias pour l'ancien nom

- **WHEN** le schéma de la DB est inspecté après migration
- **THEN** aucune view nommée `pos_session` n'existe
