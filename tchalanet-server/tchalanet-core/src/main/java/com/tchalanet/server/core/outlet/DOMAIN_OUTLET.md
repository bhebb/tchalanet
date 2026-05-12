# Domaine core.outlet — Points de vente (PDV)

> Gère les points de vente, leur association à un tenant, et leurs métadonnées (adresse, statut, horaires).
> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/sales.md` (outlet context)

---

## 1. Rôle du domaine

- CRUD outlets (admin tenant).
- Fournir lookup outlets pour POS/sales.

**Ne fait pas**

- Vente.

---

## 2. Modèle & invariants

- `Outlet`: id, tenantId, code, name, address, status.
- Invariants:
  - `code` unique par tenant.

---

## 3. Use Cases

- `CreateOutletCommandHandler`
- `UpdateOutletCommandHandler`
- `ListTenantOutletsQueryHandler`

---

## 4. Ports

- `OutletRepoPort`

---

## 5. Intégrations

- pos (sessions), sales, user assignment.

---

## 6. Notes techniques

- Multi-tenant; RLS; wrappers ID.

---

## 7. Incohérences / TODO

- Modéliser horaires et fermeture exceptionnelle.
