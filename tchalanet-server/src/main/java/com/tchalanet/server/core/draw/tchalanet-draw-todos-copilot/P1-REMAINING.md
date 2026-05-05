# P1 Remaining Tasks - core.draw

Items P1 identifiés mais non critiques pour la fonctionnalité de base.

---

## 🔧 Optimization (Performance)

### DrawSummaryJdbcRepository - Éviter N+1

**Status** : Pas créé  
**Impact** : Performance si gros volumes de draws  
**Effort** : Medium  
**Priorité** : Medium

**Action** :

```java
@Repository
public class DrawSummaryJdbcRepository {
    // Une seule query join pour summaries enrichies
    // draw + draw_channel + result_slot + draw_result
    public DrawSummaryView getById(UUID drawId) { ... }
    public List<DrawSummaryView> findByCriteria(...) { ... }
}
```

---

## 🏗️ Architecture (Clean)

### DrawSummary location refactor

**Status** : Dans `domain.model` (devrait être `application.query.projection`)  
**Impact** : Architecture suboptimale mais non bloquant  
**Effort** : Large (nombreuses références)  
**Priorité** : Low

**Action** :

1. Créer `DrawSummaryView` dans `application.query.projection`
2. Migrer toutes les références
3. Supprimer `DrawSummary.java` de `domain.model`

---

## 📊 Monitoring (Observability)

### Cache eviction ciblée

**Status** : MVP = `evictAll()` (acceptable)  
**Impact** : Performance cache suboptimale  
**Effort** : Small  
**Priorité** : Low

**Action** :

```java
// Au lieu de evictAll()
drawCacheEvictor.evictSummaries(event.tenantId());
drawCacheEvictor.evictLatest(event.tenantId());
drawCacheEvictor.evictNext(event.tenantId());
```

---

## 🕐 Time Management (Consistency)

### bulkOpen/bulkClose Clock injection

**Status** : Utilise DB `now()` actuellement  
**Impact** : Timestamps pas contrôlables en tests  
**Effort** : Small  
**Priorité** : Low

**Action** :

```java
// Passer Instant now depuis handler
int bulkOpen(List<DrawId> ids, Instant now);
int bulkClose(List<DrawId> ids, Instant now);
```

### findOpenable/findDueToClose epoch removal

**Status** : Utilise `to_timestamp(:nowEpoch)`  
**Impact** : Code moins lisible  
**Effort** : Small  
**Priorité** : Low

**Action** :

```java
// Passer Instant depuis adapter
List<OpenableDrawRow> findOpenable(Instant from, Instant to, Instant now, int limit);
```

---

## 🔔 Notifications (Future)

### Batch notifications

**Status** : Pattern existant mais non branché  
**Impact** : Pas de notifications ops auto  
**Effort** : Small  
**Priorité** : Low

**Action** :

- Ajouter `@BatchScheduledJob` sur schedulers critiques
- Configurer cooldown/fingerprint

---

## 🔍 Monitoring (Watchdog)

### DrawProvisionalWatchdogScheduler

**Status** : Non isolé/clarifié  
**Impact** : Logique watchdog mélangée  
**Effort** : Medium  
**Priorité** : Low

**Action** :

- Isoler watchdog logic
- Clarifier RLS context
- Notifications ops via edge service

---

## 💰 Settlement (Critical Future)

### Settlement alignment core.sales

**Status** : Marqué deferred  
**Impact** : Settlement pas production-ready  
**Effort** : Large (cross-domain)  
**Priorité** : **HIGH** (but deferred to sales refactor)

**Action** :

1. Définir contrat settlement avec core.sales
2. Guard tickets pending/payout incompatible
3. Transaction boundary cross-domain
4. Events settlement coordonnés
5. Retirer marker deferred de /settle endpoint

---

## 📝 Estimation globale

| Item                      | Effort | Priorité   | Timing                     |
| ------------------------- | ------ | ---------- | -------------------------- |
| DrawSummaryJdbcRepository | M      | Medium     | Sprint+1                   |
| DrawSummary refactor      | L      | Low        | Backlog                    |
| Cache eviction            | S      | Low        | Backlog                    |
| Clock injection           | S      | Low        | Backlog                    |
| Epoch removal             | S      | Low        | Backlog                    |
| Batch notifications       | S      | Low        | Sprint+2                   |
| Watchdog isolation        | M      | Low        | Backlog                    |
| **Settlement alignment**  | **L**  | **HIGH\*** | **Coordinated with sales** |

\* HIGH priority mais bloqué par refactor sales

---

**Recommandation** :

- Sprint actuel : Valider P0, tests, production-ready
- Sprint+1 : DrawSummaryJdbcRepository (si perf issue)
- Coordonné : Settlement alignment après sales refactor
- Backlog : Autres items P1 selon capacité
