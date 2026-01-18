# Domaine core.draw — Tirages & Settlement trigger

> Gère la planification/états des tirages (schedule, status), et le déclenchement (ou ingestion) du settlement côté tickets.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/draw.md`

---

## 1. Rôle du domaine

- Planifier/mettre à jour les tirages.
- Publier `DrawResultAppliedEvent` / marquer settled (selon design).
- Exposer lecture des tirages.

**Ne fait pas**

- Calcul business (gains, limites).
- AuthZ.

---

## 2. Modèle & invariants

- `Draw`: id, tenant?, game_code, slotKey, occurred_at/scheduled_at, status (SCHEDULED/RESULTED/CANCELLED), payload.
- Invariants:
  - Un tirage resulted est figé.

---

## 3. Use Cases (ports d’entrée)

- `ListDrawsQueryHandler`
- `ApplyDrawResultCommandHandler` (ou ingestion)
- `GetDrawBySlotAndDateQueryHandler` (optionnel)

---

## 4. Ports (out)

- `DrawRepoPort`
- `DrawResultProviderPort` (si externe)

---

## 5. Événements

- `DrawResultAppliedEvent` → `core.sales` settle.

---

## 6. Intégrations

- `catalog.resultslot` (slotKey)
- `catalog.drawresult` (payload publication)
- `core.sales` (settlement)

---

## 7. Notes techniques

- Multi-tenant selon cas (certains draws globaux vs tenant-specific) → choisir `BaseEntity` vs `BaseTenantEntity`.
- RLS s’applique si tenant-scoped.

---

## 8. Incohérences / TODO

- Clarifier si draw est global ou tenant-scoped par cas d’usage.
- Confirmer le flow d’ingestion vs calcul (où vit le settlement exact).
