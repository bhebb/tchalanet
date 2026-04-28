# Domaine core.pos — Points de vente & Terminaux (sessions + devices)

> Gère les sessions POS (ouverture/fermeture) et les terminaux (enregistrement, heartbeat, lock/unlock) associés aux outlets.
> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/sales.md` (pos context)

---

## 1. Rôle du domaine

- Ouvrir/fermer des sessions POS.
- Associer un terminal à une session et un outlet.
- Enregistrer/désenregistrer des terminaux; heartbeat; lock/unlock.

**Ne fait pas**

- Vente (sales).

---

## 2. Modèle & invariants

- `PosSession`: id, tenant, outletId, terminalId, userId, openedAt, closedAt, status.
- `Terminal`: id, tenantId, outletId, state, lastSeen, metadata.
- Invariants:
  - Pas de double session ouverte pour le même terminal.
  - Un terminal appartient à un tenant et un outlet.

---

## 3. Use Cases

- `OpenPosSessionCommandHandler`
- `ClosePosSessionCommandHandler`
- `ListPosSessionsQueryHandler`
- `RegisterTerminalCommandHandler`
- `SendHeartbeatCommandHandler`
- `LockTerminalCommandHandler`
- `UnlockTerminalCommandHandler`
- `UpdateTerminalMetadataCommandHandler`

---

## 4. Ports

- `PosSessionRepoPort`
- `TerminalRepoPort`

---

## 5. Intégrations

- sales pour le contexte de vente.
- outlet association.

---

## 6. Notes techniques

- Multi-tenant; RLS; wrappers ID.
- MapStruct pour mappers; UUID en JPA.

---

## 7. Incohérences / TODO

- Aligner les endpoints et la modélisation `pos` vs `terminal` vs `outlet`.
- Clarifier le cycle heartbeat et la persistance des métadonnées.
