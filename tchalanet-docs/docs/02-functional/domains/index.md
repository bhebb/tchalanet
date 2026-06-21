# Domaines métier (Bounded Contexts)

**Bounded contexts critiques du système Tchalanet**

Chaque domaine est responsable d'un ensemble cohérent de règles métier et de données.

# Domaines

Chaque domaine décrit :

- responsabilité
- invariants métiers
- objets principaux (agrégats / VO)
- interactions avec autres domaines
- pointeurs vers la doc near-code

Règle :

- ici = _quoi et pourquoi_
- near-code = _comment_

---

## 📋 Domaines

### [Draw (Tirages)](draw.md)

### [Sales (Ventes)](sales.md)

### [Payout (Paiements gagnants)](payout.md)

### [Ledger (Journal comptable)](ledger.md)

### [Limits (Limites métier)](limits.md)

### [AccessControl (Contrôle d'accès)](accesscontrol.md)

### [Catalog (Référentiels)](catalog.md)

---

## Catalogue complet des domaines

- [AccessControl](accesscontrol.md) — Rôles, permissions, évaluation fine-grained par tenant.
- [Address](address.md) — Adresses (tenant/outlet/user), validations basiques.
- [Audit](audit.md) — Audit applicatif (audit_event) + historique Envers.
- [Autonomy](autonomy.md) — Niveaux d’autonomie (NONE|PARTIAL|FULL) influençant validations.
- [Catalog](catalog.md) — Référentiels (read-mostly) : game, pricing, drawresult, resultslot.
- [Draw](draw.md) — Tirages (planification/états) + trigger/ingestion settlement.
- [DrawResult (Catalog)](drawresult.md) — Référentiel des résultats publiés.
- [External](external.md) — Intégrations externes (Firebase, PSP, providers data).
- [FeatureFlags](featureflags.md) — Flags de fonctionnalités (lecture contrôlée).
- [Game (Catalog)](game.md) — Référentiel des jeux et bet types.
- [Haiti](haiti.md) — Projection lots haïtiens + catalog Tchala.
- [Ledger](ledger.md) — Journal comptable append-only, idempotence.
- [Limits](limits.md) — Limites de vente/payout (period/scope/dimension).
- [Notification](notification.md) — Notifications tenant, ack.
- [OfflineSync](offlinesync.md) — Synchronisation offline des ventes.
- [Outlet](outlet.md) — Points de vente (PDV), groupement optionnel par tenant.
- [SellerTerminal](terminal.md) — Acteur de vente unique (remplace Terminal + Session + Seller).
- [Pricing (Catalog)](pricing.md) — Référentiel de pricing (multipliers, odds).
- [Payout](payout.md) — Claims & payments (split, reversal) avec idempotence.
- [ResultSlot (Catalog)](resultslot.md) — Slots globaux (provider/timezone/drawTime/daysOfWeek).
- [Sales](sales.md) — Tickets: émission, annulation, statut public.
- [Session](session.md) — **RETIRÉ** — sessions POS supprimées (voir SellerTerminal).
- [Settings](settings.md) — App settings (registry + values) par tenant/outlet.
- [Tenant](tenant.md) — Identité/état du tenant, configuration globale.
- [Theme](theme.md) — Thèmes visuels (publication, audit, versioning) par tenant.
- [US Lottery](uslottery.md) — Providers US (pick3/pick4), normalisation.
- [User](user.md) — Profil applicatif + préférences.

> Chaque page de domaine renvoie vers sa source near-code côté backend (`tchalanet-server/src/**/DOMAIN_*.md`).

---

## 🎯 Principe

Chaque doc domaine contient :

- Responsabilités (owns / does not own)
- Invariants métier (règles inviolables)
- États et transitions (state machines)
- Événements publiés (domain events)
- Contrats (input/output)

**Pas de code** ici (Java/TS/SQL) → voir implémentations via [99-links](../../99-links/index.md).

---

## Rappel — Contrat de non-duplication

- Cette section (MkDocs) décrit le quoi/pourquoi, invariants, glossaire et relations cross-apps.
- Les détails d’implémentation (handlers, ports, transactions, tables, events, API paths) vivent dans `tchalanet-server/src/**/DOMAIN_*.md`.
- Les détails UI (routes/pages/widgets/i18n/contrats web/mobile) vivent près du code (apps/libs READMEs).

**Dernière mise à jour** : 2026-06-20
