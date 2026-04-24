# Core – LimitPolicy

## Vision

`LimitPolicy` **ne vend rien**, **ne paye rien**, **ne connaît pas l’UI**.

Il fait exactement deux choses :

1. **Évaluer des règles** sur un ticket  
   → `ALLOW | WARN | BLOCK` (+ détails)
2. **Maintenir des facts (exposure)** nécessaires aux règles stateful  
   → hot number, risque payout, caps

---

## 1. Responsabilités

### Sales / Payout

- Appellent `LimitPolicyRuntimeService` pour évaluer
- Publient un event **AFTER_COMMIT** pour appliquer les facts

### LimitPolicy

- Évalue et compte
- Aucune logique UI
- Aucun side-effect business (vente/paiement)

---

## 2. Structure

core.limitpolicy
├─ domain
│ ├─ model
│ └─ engine
├─ application
│ ├─ service
│ ├─ query
│ └─ command
└─ infra
├─ persistence
└─ event

---

## 3. Runtime Evaluation

`LimitPolicyRuntimeService` est la **façade unique**.

Étapes :

1. Charger `LimitDefinition`
2. Charger `LimitAssignment`
3. Résoudre (`LimitResolver`)
4. Charger facts (`LimitFactsSnapshot`)
5. Évaluer (`InProcessLimitEvaluationEngine`)
6. Retourner `LimitEvaluationResult`

---

## 4. Facts & Exposure

### Table `draw_exposure`

Read model incrémental utilisé par les règles stateful.

Dimensions :

- `tenant_id`
- `draw_id`
- `scope_type` / `scope_id`
- `bet_type`
- `selection_key`

Mesures :

- `stake_total`
- `sales_count`
- `potential_payout_total`

---

## 5. Canonicalisation (obligatoire)

Toute écriture de `selection_key` doit passer par :
common.selection.SelectionKeyCanonicalizer

Formats canoniques :

- `MATCH_1_2D` → `05`
- `MATCH_2_2D` → `12-34`
- `MATCH_3_2D` → `12-34-56`
- `MARRIAGE_2D2D` → `12-34` (trié)
- `LOTTO3_3D` → `123`
- `LOTTO4_PATTERN` → `12**`

---

## 6. Apply Exposure (AFTER_COMMIT)

### Flow

1. `Sales` publie `TicketPlacedEvent`
2. Listener LimitPolicy reçoit l’event
3. Envoie `ApplyTicketExposureCommand`
4. Handler applique l’exposition (idempotent)

### Idempotence

Le handler **doit** utiliser :
-- ProcessedEventPort
-- handler_key = "limitpolicy.exposure"

Flow :

1. `alreadyProcessed` ⇒ noop
2. Apply exposure (incréments)
3. `markProcessed`

---

## 7. Règles Stateful (MVP)

### MAX_EXPOSURE_PER_SELECTION_PER_DRAW

- Compare `stake_total + deltaStake` à la limite

### MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW

- Compare `potential_payout_total + deltaPayout` à la limite

Pattern :

- Grouper par `(betType, selectionKey)`
- Charger facts
- Calculer `current + delta`
- Déclencher breach si dépassement

---

## 8. Dashboard vs Tenant Admin

### Dashboard

- Affiche des **alertes**
- Top selections par :
  - stake
  - payout potentiel
  - sales_count

### Tenant Admin

- Configure :
  - `LimitDefinition`
  - `LimitAssignment`
  - autonomie

Aucune écriture de limits depuis le dashboard.

---

## 9. RLS (non négociable)

- Aucun `WHERE tenant_id = ?` en persistence
- `tenant_id` fourni par `TchContext`
- Jobs async doivent binder un contexte

---

## 10. Checklist (DoD)

- [ ] `LimitPolicyRuntimeService` opérationnel
- [ ] `draw_exposure` incrémental et audité
- [ ] Canonicalisation systématique
- [ ] Apply exposure idempotent
- [ ] RLS respecté
