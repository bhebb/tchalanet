# DOMAIN — Agent

But: documenter les invariants, les flows critiques et la classification des use cases
pour le sous-domaine `core.agent` afin d'appliquer la RFC « architecture à intensité
variable » (Niveaux 1..4) et les conventions RLS / ports/adapters.

1) Contexte
---
- Ce domaine gère la structure d'agents (affiliés), leurs zones opérationnelles,
  les mandats de zone (permissions) et les affectations d'utilisateurs.
- Opérations typiques : création d'agent, changement de statut, création de zone,
  assignation d'utilisateur, lectures de listes / résumés pour UI/admin.

2) Classification des use cases (NIVEAUX RFC)
---
- Niveau 1 — Query projection simple
  - Liste d'agents (summary), liste de zones, tableaux de bord admin.
  - Flux canonique : QueryHandler -> ReadPort -> JpaAdapter -> ProjectionRepository
  - Exemple implémenté : `ListAgentsQuery` → `AgentSummaryReadPort` → `AgentSummaryReadJpaAdapter`.

- Niveau 2 — Query métier
  - Calculs de permissions composées (ex: readiness pour vente selon zones),
    contexte opérationnel agrégé (permissions + status + ledger data éventuellement).
  - Reste read-only, peut orchestrer plusieurs ReadPorts / QueryBus.ask.

- Niveau 3 — Command simple (autorisé dans `core` sous conditions)
  - CreateAgentCommand, CreateAgentZoneCommand, AssignUserToAgentCommand,
    UpdateAgentStatusCommand, SeedDefaultAgentZonesCommand.
  - Conditions : pas d'impact financier direct, pas d'écriture cross-domain critique,
    transaction `@TchTx`, ports utilisés (Reader/Writer), adapters infra.

- Niveau 4 — Command critique
  - Opérations qui impacteraient directement settlement/payout/limits/fraude.
  - Aucun cas courant dans `core.agent`. Si une mutation affecte la comptabilité
    ou le règlement, reclasser en Niveau 4 et imposer Domain Model / AfterCommit events.

3) Invariants métier (must)
---
- Un `Agent` appartient à un `tenant` → tenant scoping assuré par RLS; ne pas
  filtrer par `tenantId` côté appli.
- `AgentZone` doit avoir un `code` unique par tenant (contrainte DB `uq_agent_zone_tenant_code`).
- `AgentZoneMandate` doit conserver les limites (maxChildAgents, maxSellers, etc.) et
  ne peut être créé que si la zone et l'agent existent et sont tenant-scoped.
- Mise à jour d'entités sensibles : charger l'entité managée, muter champs autorisés,
  éviter `mapper.toEntity(domain)` + save pour update (voir persistence.md §4.1).

4) RLS / deleted visibility
---
- RLS est la source de vérité pour le tenant-scoping (`app.current_tenant`).
- `app.deleted_visibility` contrôle si les requêtes voient les lignes supprimées.
- Les repositories infra doivent, par défaut, s'appuyer sur RLS ; éviter d'implémenter
  des WHERE tenant_id = :tenantId côté appli.
- Pour les projections read-only, on peut inclure `WHERE deletedAt IS NULL` si la vue
  doit explicitement cacher les supprimés indépendamment du flag RLS; préférence :
  laisser RLS gérer la visibilité sauf besoin explicite.

5) Ports / Adapters / Repositories (règles)
---
- Ports application (`internal.application.port.out`) : `public` interfaces,
  ne doivent pas exposer types JPA.
- Adapters infra (`internal.infra.persistence.adapter` / `internal.infra.persistence`) :
  package-private, annotés `@Component` / `@Repository` si besoin.
- Repositories Spring Data : package-private, utilisés uniquement par adapters.
- Mappers persistence : package-private; utilisés uniquement par adapters.

6) Commands existantes — classification & checklist
---
- `CreateAgentCommand` (Niveau 3)
  - Checklist : validation de zones/status, `@TchTx` présent, utilise Reader/Writer.
  - Reco : ajouter Audit AfterCommit si la création doit être tracée.

- `CreateAgentZoneCommand` (Niveau 3)
  - Checklist : utilise writer.zoneCodeExists (DB constraint), `@TchTx` présent.

- `AssignUserToAgentCommand` (Niveau 3)
  - Checklist : idempotence (unique constraint `uq_agent_user_relation`), audit possible.

- `UpdateAgentStatusCommand` (Niveau 3, potentiellement 4)
  - Checklist : vérifier listeners side-effects; si changement provoque actions
    cross-domain (ex: désactivation stoppe paiements), publier event after-commit.

- `SeedDefaultAgentZonesCommand` (Niveau 3)
  - Checklist : seed idempotent, accepte collision (already exists) comme no-op.

7) Tests recommandés
---
- Unit tests pour chaque handler (validations, politiques) — préférer in-memory ports
  pour handlers et tester adapters séparément.
- Integration tests (Testcontainers) pour valider RLS + repos + projections read views.
- ArchUnit tests : vérifier qu' `internal.application` n'importe pas `internal.infra`.

8) Documentation / PR checklist
---
- Lors d'une PR touchant la persistence : synchroniser entité JPA, vues V108, tables `_aud` si @Audited.
- Documenter tout changement d'invariant dans ce fichier `DOMAIN_AGENT.md`.
- Ajouter note dans l'ADR `ADR-00X-core-variable-intensity.md` si pattern réutilisé ailleurs.

9) Points à challenger / décisions ouvertes
---
- Faut‑il que `UpdateAgentStatus` émette un event after-commit ? (reco : oui si side-effects)
- Doit-on retirer `TenantId` des ports write/read pour conformer strictement au Niveau 1 ?
  (operation invasive — nécessite refactor controllers/handlers/tests)

10) Contact / ownership
---
- Domaine maintenu par l'équipe core/agent (owner: @core-agent). Pour changements invasifs
  (ports signatures, migrations DB), demander validation et tests e2e avant merge.

