# Domaine Session

> Ce fichier est un **template** pour documenter le domaine backend.
> Copie/complète les sections ci-dessous (voir `docs/DOMAIN_TEMPLATE.md`).
> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/sales.md` (context session)

---

## 1. Rôle du domaine

**Responsabilité principale**

> Gérer le cycle de vie des sessions POS (ouverture/fermeture), l’association avec un terminal et un outlet, et exposer des vues de session pour le contexte de vente.
> **Ce que le domaine fait**

- Ouvrir une session pour un utilisateur, un terminal et un outlet.
- Fermer une session (validation de clôture, timestamps, statut).
- Exposer des requêtes de sessions (courante, historiques, par outlet/terminal).
  **Ce que le domaine ne fait pas**
- Ne vend pas de tickets (sales).
- Ne gère pas les payouts.

---

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- `PosSession` — id, tenantId, outletId, terminalId, userId, openedAt, closedAt?, status.

### Invariants métier

- Une session ouverte ne peut pas être ré-ouverte (pas de double session active pour un même terminal).
- `closedAt` défini uniquement lors de `close`.
  > Valeur métier clé :
  > Offrir un contexte opérationnel fiable (tenant/outlet/terminal/user) pour les ventes et rapports.

---

## 3. Cas d’utilisation (ports d’entrée)

- `OpenSessionCommandHandler(tenantId, outletId, terminalId, userId)`.
- `CloseSessionCommandHandler(sessionId)`.
- `GetCurrentSessionQueryHandler(tenantId, terminalId)`.
- `ListSessionsByOutletQueryHandler(tenantId, outletId, page, size)`.

---

## 4. Ports de sortie (dépendances externes)

- `PosSessionRepoPort` — persistance/lecture des sessions.

---

## 5. Mapping & DTOs (convention)

- MapStruct pour mapper infra.web.model ↔ application.command/query.model.
- Records immuables pour DTO simples.

---

## 6. Règles métier importantes

- Interdire l’ouverture d’une nouvelle session si une session active existe pour le même terminal.
- Valider la cohérence tenant/outlet/terminal/user à l’ouverture.

---

## 7. Intégration avec les autres domaines

Dépend de : pos (terminal), outlet, user (identité), tenant.
Utilisé par : sales (contexte de vente), reporting.

---

## 8. Notes techniques

- Multi-tenant; RLS; wrappers ID; UUID en JPA.
- Transactions `@TchTx` pour commands.

---

## 9. Incohérences / TODO

- Préciser les statuts de session (OPEN/CLOSED/ABORTED?).
- Définir les endpoints exacts et la pagination par défaut.
