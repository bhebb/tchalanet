# Sensitive JPA Updates — Norme P0

## Status

**NORMATIVE**

Cette norme complète `persistence.md`, `jpa_entities.md`, `audit.md`, et `rls.md`.

---

## 1) Problème couvert

Une entité JPA sensible ne doit jamais être traitée comme un DTO détaché lors d'un
update. Le pattern suivant est interdit pour toute ligne existante sensible :

```java
var entity = mapper.toEntity(domainAggregate); // fresh entity with existing id
repository.save(entity);                       // Hibernate merge
```

Ce pattern perd l'état managé par Hibernate et contourne les protections que la
persistence doit garantir :

- `@Version` / optimistic locking ;
- `created_at`, `created_by`, `updated_at`, `updated_by` ;
- `tenant_id` (`updatable=false`) et listener tenant ;
- soft delete ;
- graphes enfants avec cascade/orphan-removal ;
- transitions de lifecycle et états irréversibles.

---

## 2) Définition : entité sensible

Une entité est sensible si elle porte au moins un des éléments suivants :

- tenant ownership (`BaseTenantEntity`, RLS, `tenant_id`) ;
- lifecycle métier (`status`, `opened_at`, `closed_at`, `settled_at`, `paid_at`, etc.) ;
- argent, payout, ledger, exposure, limite ou settlement ;
- session opérationnelle ou blocage opérationnel ;
- audit Envers ou audit Spring ;
- versioning optimistic (`@Version`) ;
- lignes enfants dans le même agrégat ;
- état final ou irréversible (`CONFIRMED`, `OVERRIDDEN`, `SETTLED`, `PAID`, etc.).

Exemples P0 dans Tchalanet :

- `draw`
- `draw_result`
- `ticket`, `ticket_line`, `ticket_charge`
- `payout`
- `terminal`
- `outlet`
- `sales_session`
- `limit_assignment`, exposure/controls
- `ledger_entry`
- offline sync records

---

## 3) Patterns autorisés

### 3.1 Create-only mapping

Autorisé uniquement quand la ligne ne peut pas déjà exister.

```java
var entity = mapper.toEntity(domain);
repository.save(entity);
```

Conditions obligatoires :

- le code sait que c'est une création ;
- si l'id est fourni par le domaine, l'existence préalable est vérifiée ou empêchée par
  une contrainte unique ;
- un duplicate id/natural key échoue, il ne devient pas un update silencieux.

### 3.2 Load-managed-and-mutate

Pattern par défaut pour les updates JPA sensibles.

```java
var entity = repository.findById(domain.id().value())
    .orElseThrow(...);

mutator.applyTo(entity, domain);
return mapper.toDomain(entity);
```

Règles :

- charger l'entité managée dans la même transaction ;
- charger les enfants nécessaires avant de les différencier ;
- assert les champs immutables au lieu de les écraser ;
- muter uniquement les champs explicitement autorisés ;
- ne jamais transplanter `tenantId`, `version`, `createdAt`, `createdBy` ;
- laisser Hibernate gérer dirty checking, audit et version bump.

### 3.3 Guarded SQL

Autorisé pour bulk updates, idempotency/replay, append-only, ou transitions où la garde
DB est plus claire que JPA.

Le SQL doit porter la règle de concurrence :

- scope tenant ou contexte équivalent ;
- garde de status/lifecycle ;
- garde version si applicable ;
- natural key ou idempotency key ;
- `version = version + 1` lorsque la table a une colonne `version`.

Exemples acceptables :

- draw bulk open/close avec status compatible ;
- draw result upsert qui préserve `CONFIRMED` / `OVERRIDDEN` ;
- ledger append-only avec contrainte unique ;
- insert idempotent `ON CONFLICT DO NOTHING` pour replay.

---

## 4) Patterns interdits

### 4.1 Rebuild + save sur update

Interdit pour toute entité sensible existante.

```java
repository.save(mapper.toEntity(existingDomain));
```

Même si le code "répare" ensuite `version`, `tenantId`, ou les champs audit, c'est un
bug de conception. Ces réparations masquent le problème et doivent être supprimées.

### 4.2 Missing row devient create

Interdit pour un update.

```java
var entity = repository.findById(id).orElseGet(Entity::new);
mapper.updateEntity(domain, entity);
repository.save(entity);
```

Ce pattern est acceptable seulement si la méthode est explicitement un upsert métier et
que la création est voulue, documentée, testée, et protégée par une natural key.

Pour les entités sensibles, si `domain.id()` représente une ligne existante attendue et
que la ligne manque, le writer doit échouer (`not found`, `conflict`, ou équivalent).

### 4.3 Mapper qui écrase les champs système

Un mapper d'update ne doit pas modifier :

- `tenantId`
- `version`
- `createdAt`
- `createdBy`
- colonnes `updatable=false`
- champs d'identité ou natural key immutables

---

## 5) Mutator d'agrégat

Pour les agrégats avec beaucoup de champs, créer un mutator dédié proche de la
persistence :

```text
core.<domain>.internal.infra.persistence.mapper.<Aggregate>JpaMutator
```

Responsabilités :

- `assertImmutableFields(managed, domain)` ;
- copier les champs scalaires mutables ;
- différencier les collections enfants ;
- ajouter les nouveaux enfants via helper dédié ;
- retirer les orphelins uniquement quand c'est une règle métier attendue ;
- ne pas contenir d'invariant métier, seulement la traduction persistence.

Le domaine reste la source des décisions. Le mutator applique une décision déjà prise.

### 5.1) Attention : mutator = risque de bug silencieux

Un mutator qui oublie un champ mutable peut passer ses tests unitaires et perdre une
transition métier en production. Il ne suffit donc pas de tester le mutator ou l'adapter
en isolation.

Chaque mutator d'agrégat sensible doit être couvert par :

- des tests unitaires du mutator pour les assertions immutables et le diff technique ;
- des tests d'intégration par commande métier réelle qui touche le writer ;
- une matrice explicite champs immuables / champs mutables dans le test ou le change.

Pour `Ticket`, les tests par commande réelle doivent couvrir au minimum :

- `RecordTicketPrintCommand` ;
- `ApproveTicketSaleCommand` ;
- `CancelTicketCommand` ;
- `VoidTicketCommand` si le flow est exposé par un command handler ;
- promotion/sync offline (`CreateTicketFromOfflineSubmissionCommand` et retour
  `OfflineSubmissionProcessedEvent`) quand le flow crée ou réutilise un ticket.

Ces tests doivent vérifier à la fois le changement attendu et l'absence de changement
sur les champs immuables.

### 5.2) Matrice Ticket : immuable vs mutable

Pour `Ticket`, la matrice minimale est :

| Famille | Champs | Règle |
| --- | --- | --- |
| Identité | `ticketId`, `tenantId` | Immuables, assertés sur update. |
| Contexte vente | `outletId`, `terminalId`, `sellerId`, `salesSessionId`, `drawId`, `businessDate` | Immuables après création, assertés. |
| Codes | `ticketCode`, `publicCode`, `verificationCode` | Immuables, assertés. |
| Origine | canal/source offline initiale | Immuable sauf champ explicitement documenté. |
| Devise | `currency` | Immuable après création, sauf ADR explicite. |
| Audit création | `createdAt`, `createdBy` | Jamais touchés par le mutator. |
| Audit update | `updatedAt`, `updatedBy` | Mutés uniquement par audit/listeners. |
| Sale lifecycle | sale status, approval trace, rejection trace | Mutable via commandes lifecycle autorisées. |
| Print | print status/count/last printed at | Mutable via commandes print/reprint. |
| Cancel / void | cancel/void status, reason, actor, timestamp | Mutable via commandes cancel/void autorisées. |
| Result | result status, draw result ref, resulted at, line result fields | Mutable via settlement/result commands. |
| Settlement | settlement status, payout refs, paid refs | Mutable via settlement/payout commands. |
| Money | payout amounts, winning amount, potential payout where domain permits | Mutable seulement par result/settlement flows documentés. |
| Lines / charges | result/payout fields, additions/removals attendues | Diff en place ; pas de remplacement aveugle du graphe. |

---

## 6) Règles par cas Tchalanet

| Cas | Norme |
| --- | --- |
| Ticket / line / charge | Update par managed aggregate + diff enfants. Pas de version transplant. |
| Sales session | Update par managed entity. Contexte d'ouverture immuable. |
| Draw | Generic JPA save interdit pour update ; utiliser managed mutation ou SQL gardé. |
| Draw result | SQL gardé accepté ; pas de generic JPA save d'update. |
| Payout | Managed mutation ; missing existing id échoue ; ticket/amount/currency immutables après création. |
| Terminal | Managed mutation tenant-scoped ; blocages opérationnels explicites. |
| Outlet | Managed mutation tenant-scoped ; status/blocages opérationnels explicites. |
| Limit assignment | Scope/rule immutables ; update managed ; soft delete explicite. |
| Ledger entry | Append-only ; duplicate id/natural key échoue ; jamais d'update. |
| Offline sync | Existing entity passée au mapper doit être managée ; missing row explicite. |

---

## 7) Tests obligatoires

Chaque correction d'un writer sensible doit couvrir au moins :

- create path inchangé ;
- update path sur ligne existante ;
- commandes métier réelles qui utilisent le writer, pas seulement adapter/mutator ;
- préservation de `tenantId`, `createdAt`, `createdBy` ;
- bump de `version` par Hibernate ou SQL gardé ;
- missing existing id échoue si ce n'est pas un upsert métier ;
- transitions concurrentes ou replay idempotent pour les flows sensibles ;
- enfants ajoutés/supprimés si l'agrégat a des collections.

Pour `Ticket`, ne pas considérer le fix terminé sans tests d'intégration couvrant les
commandes qui appellent réellement `TicketWriterPort#save`, notamment print, approve,
cancel, void si présent, result/settlement, et offline promotion/sync si applicable.

---

## 8) Enforcement

Un test d'architecture/convention doit signaler les adapters core update-capables qui
font :

```text
mapper.toEntity(...) + repository.save(...)
```

sur des entités sensibles.

Allowlist autorisée seulement pour :

- create-only explicite ;
- append-only explicite ;
- guarded SQL avec garde documentée ;
- upsert métier documenté par une natural key et des tests de replay.

Chaque allowlist doit expliquer pourquoi la corruption par detached merge est impossible.

---

## 9) Checklist PR

- [ ] L'entité est-elle sensible selon la section 2 ?
- [ ] Create et update sont-ils séparés ?
- [ ] L'update charge-t-il une entité managée ou utilise-t-il SQL gardé ?
- [ ] Les champs immutables sont-ils assertés ?
- [ ] Le mapper d'update évite-t-il tenant/version/audit ?
- [ ] Missing row sur update échoue-t-il ?
- [ ] Les enfants sont-ils différenciés en place ?
- [ ] Les tests prouvent-ils tenant/audit/version/lifecycle ?
- [ ] Toute exception à cette norme est-elle documentée dans le change OpenSpec ou une ADR ?
