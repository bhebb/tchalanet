# Domaine `core.<name>` — <Titre court>

> **Archetype** : Clean Architecture / Hexagonal / CQRS (core uniquement).  
> **Placer ce fichier** : `tchalanet-core/src/main/java/com/tchalanet/server/core/<name>/DOMAIN_<NAME>.md`

---

## 1. Rôle du domaine

**Responsabilité principale**

> Une phrase. Ex. : « Gérer le cycle de vie des tirages de la création à la clôture. »

**Ce que le domaine fait**

- 3 à 6 puces sur ce que le domaine couvre.

**Ce que le domaine ne fait pas**

- Ce qui est explicitement hors scope (délégué à quel autre domaine/layer).

---

## 2. Modèle métier

### Agrégats / entités principales

- `NomAgregate` — description courte.
- `ValueObject` — description.

### Invariants métier

- Règles que le domaine est seul à garantir.
- Ex. : « Un ticket VOID ne peut plus être approuvé. »

> Valeur métier clé : ce qui serait catastrophique si ce domaine échouait silencieusement.

---

## 3. API publique (`api/`)

Interface Java consommée par les autres modules.

```text
core/<name>/api/
  command/          ← XxxCommand records (immuables)
  query/            ← XxxQuery records + XxxResult / XxxRow
  event/            ← XxxEvent records (publiés après commit)
  model/            ← Read models partagés
```

**Autorisé dans api/** : commands, queries, events, read models, result models.  
**Interdit dans api/** : agrégats internes, JPA entities, repositories, handlers, ports out, controllers.

---

## 4. Structure interne (`internal/`)

```text
core/<name>/internal/
  domain/
    model/          ← Agrégats, value objects (Java pur, pas de Spring)
    service/        ← Domain policies, calculators (pas d'injection, pas d'I/O)
    event/          ← Domain events internes
    exception/      ← Exceptions métier
  application/
    command/handler/  ← @UseCase, @TchTx, délèguent via ports out
    query/handler/    ← @UseCase, lecture seule
    port/out/         ← Interfaces pour adapters infra (persistence, external)
    service/          ← Orchestrateurs applicatifs (optionnel)
  infra/
    persistence/    ← JPA entities, repositories, JpaAdapter
    web/            ← Controllers thin (CommandBus/QueryBus dispatch)
    event/          ← Listeners (idempotents), publishers AfterCommit
    batch/          ← Jobs Spring Batch (optionnel)
    scheduler/      ← Tâches planifiées (optionnel)
    cache/          ← Cache adapters (optionnel)
    config/         ← Spring @Configuration du domaine
```

---

## 5. Cas d'utilisation (writes)

| Command | Handler | @TchTx |
|---|---|---|
| `XxxCommand` | `XxxCommandHandler` | ✅ |

Pattern :
```java
@UseCase
@TchTx
public class XxxCommandHandler implements CommandHandler<XxxCommand, XxxResult> {
  // 1. Charger agrégat via port out
  // 2. Appeler méthode domaine
  // 3. Persister via port out
  // 4. AfterCommit.run(() -> eventPublisher.publish(...))
}
```

---

## 6. Cas d'utilisation (reads)

| Query | Handler |
|---|---|
| `XxxQuery` | `XxxQueryHandler` |

Pattern :
```java
@UseCase
public class XxxQueryHandler implements QueryHandler<XxxQuery, XxxResult> {
  // Lecture directe via port out ou repository
  // Pas de @TchTx sauf si lecture transactionnelle requise
}
```

---

## 7. Ports de sortie (`application/port/out/`)

- `XxxReaderPort` — charge/cherche des agrégats.
- `XxxWriterPort` — persiste des agrégats.
- `XxxExternalPort` — appels externes (si applicable).

> Les adapters infra implémentent ces ports. Les handlers ne connaissent pas JPA.

---

## 8. Événements publiés

| Événement | Publié dans | Consommateurs |
|---|---|---|
| `XxxEvent` | `AfterCommit.run(...)` dans handler | `core.yyy`, `features.zzz` |

---

## 9. Dépendances

**Consomme** :
- `catalog.<x>.api` — référentiels (via injection directe de `XxxCatalog`)
- `platform.<y>.api` — services transversaux (audit, accesscontrol, tenantconfig…)
- `core.<z>.api` — autres domaines core (via `QueryBus` ou injection directe si autorisé)

**Utilisé par** :
- Modules qui consomment les commands/queries via `CommandBus`/`QueryBus`
- Listeners qui écoutent les events publiés

---

## 10. Points d'attention

- Multi-tenant ? RLS actif ?
- Audit Envers actif sur les entités ?
- Intégrations externes (HTTP clients, batch providers) ?
- Idempotence nécessaire (via `platform.idempotence`) ?
