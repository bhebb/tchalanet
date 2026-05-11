# Conventions — Appels inter-domaines (CANONIQUE)

Ce document définit la règle pragmatique pour les interactions entre domaines : core ↔ catalog, core ↔ core, et l’orchestration via `features/` ou `batch/`.

---

## 1) Règle officielle (table de décision)

| Cas                          | Solution / pattern recommandé                                                                        |
| ---------------------------- | ---------------------------------------------------------------------------------------------------- |
| Core → Catalog (lecture)     | Import d’un reader / port depuis le catalog (read-only)                                              |
| Core → Core (lecture simple) | Import d’un reader / port de l’autre domaine (à minimiser)                                           |
| Core → Core (effet métier)   | Émettre un event + publication après commit + listeners (consommation dans transactions séparées)    |
| Feature / BFF                | Orchestration via `CommandBus` / `QueryBus` (séquences multi-domaines)                               |
| Batch                        | Utiliser le bus (commands/queries → handlers); le batch orchestre, ne contient pas la logique métier |

> ❌ Règle générale : pas de ports « pour tout », pas de cycles, et surtout pas d’écritures cross-domain dans une même transaction critique.

---

## 2) Définitions rapides

- **Lecture simple** : lecture _read-only_, pas d’écriture, pas de side-effects, pas de transition d’état.
- **Effet métier** : écriture, transitions d’état, side-effects (ledger, notifications, statistiques, payouts…).

---

## 3) Interdits (non négociables)

- ❌ Écriture cross-domain dans la même transaction critique.
- ❌ Un domaine qui pilote la logique interne d’un autre domaine (pas de commande déguisée « appeler les implémentations internes »).
- ❌ Dépendances circulaires (A dépend de B et B dépend de A).
- ❌ Création de ports « pour tout » juste pour éviter un import.
- ❌ `core/` n’écrit jamais dans `catalog/` (catalog = référentiel déclaratif).

---

## 4) Principes structurants

### 4.1 Shared lookup → Catalog d’abord

Si une donnée est lue par 2+ domaines et qu’elle est majoritairement en lecture (read-mostly), elle doit être exposée par un **catalog** (registry / lookup / mapping / config déclarative). Objectif : réduire les imports inter-core et limiter les risques de cycles.

### 4.2 Effet cross-domain → Event

Dès qu’un effet métier touche un autre domaine (écriture, transition, ledger, payout, stats) :

1. Publier un _event_ après commit (`after-commit`).
2. Les domaines cibles réagissent via des listeners qui effectuent leurs écritures dans leurs propres transactions.

---

## 5) Patterns autorisés (avec exemples)

### 5.1 Core → Catalog (lecture)

- Le domaine core importe un port/reader exposé par le catalog.
- Le catalog est _side-effect free_ : pas d’events, pas d’invariants métier, pas d’orchestration.

**Exemples**

- `draw` lit `result_slot` (catalog global).
- `sales` lit des registries (jeux / options / mappings) si déclaratifs.

---

### 5.2 Core → Core (lecture simple)

- Le domaine consommateur importe un port de lecture minimal exposé par l’autre domaine.
- À utiliser quand ce n’est pas un shared lookup (sinon : créer/consommer un catalog).

**Exemple**

- `sales` lit une projection « autonomie/limites » si elle appartient clairement à un domaine core.

---

### 5.3 Core → Core (effet métier)

- Le domaine source **publie un event** (publication after-commit).
- Les domaines cibles **listening** réagissent et écrivent dans leurs propres transactions.

**Exemples**

- `DrawResultAppliedEvent` → rapprochement/settlement de tickets, payouts, statistiques.
- `TenantConfigUpdatedEvent` → invalidation cache, refresh de vues dérivées.

---

### 5.4 Feature / BFF (orchestration via bus)

- Les _features_ orchestrent plusieurs domaines en envoyant des commands/queries via le bus :
  - `QueryBus.ask(...)` — pour les lectures
  - `CommandBus.execute(...)` — pour les écritures
- La feature séquence A → B → C, compose des DTO, gère erreurs/timeouts.
- Aucune invariant métier critique ne doit vivre dans une feature.

**Exemple**

- Feature « Sell ticket » : access-control → limits → pricing → sales (create) → notification (eventual).

---

### 5.5 Batch (via le bus)

- Un job batch envoie des commands/queries via le bus.
- Le batch orchestre des traitements, mais **ne contient pas** la logique métier elle‑même.

---

## 6) Checklist de review (PR)

Avant de merger une PR impliquant des appels inter-domaines, vérifiez :

- L’appel est‑il **lecture** ou **effet** ?
- Si lecture partagée : faudrait‑il créer/consommer un **catalog** ?
- Si effet : est‑ce bien publié via **event after‑commit** ?
- Est‑ce qu’on évite une écriture cross‑domain dans la même transaction ?
- La PR introduit‑elle une dépendance circulaire ?
- Le port importé est‑il **minimal** et **read‑only** ?

---
