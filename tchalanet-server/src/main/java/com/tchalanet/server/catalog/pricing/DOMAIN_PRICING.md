# Domaine Catalog Pricing

> Référentiel des grilles de prix et paramètres associés (stakes, limits, coefficients) pour les opérations de vente. Lookup utilisé par `core.sales`.

---

## 1. Rôle du domaine

**Responsabilité principale**

> Maintenir des tables de prix/paramètres (read-mostly) pour alimenter la vente et la validation.

**Ce que le domaine fait**

- Catalogue de pricing (règles statiques ou semi-statiques).
- Expose lookup pour la vente (validation stake, affichage UI).
- (Optionnel) Variantes tenantisées si nécessaire.

**Ce que le domaine ne fait pas**

- Calcul de gains (payout/winning) — core.payout.
- Règles de limites dynamiques — core.limitpolicy.

---

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- `PricingTable` — (id, code, name, jsonSpec, status).
- (Optionnel) `TenantPricing` — mapping tenant ↔ pricing.

### Invariants métier

- `code` unique.

> Valeur métier clé :
> Servir de référentiel pour la cohérence des prix et des validations côté vente.

---

## 3. Cas d’utilisation (ports d’entrée)

- `ListPricingTablesQuery` — lister.
- `GetPricingTableQuery` — obtenir par code.
- (Admin) `CreateOrUpdatePricingTableCommand` — maintenir le catalogue.

---

## 4. Ports de sortie (dépendances externes)

- `PricingReaderPort` — lecture.
- `PricingWriterPort` — écriture (admin).

---

## 5. Mapping & DTOs (convention)

- MapStruct pour mapping entity ↔ projection `PricingResponse`.
- DTO d’entrée admin: `PricingRequest`.
- IDs wrappers côté web; UUID en JPA.

---

## 6. Règles métier importantes

- Le JSON spec doit être validé par schéma.
- Les changements doivent être auditables.

---

## 7. Intégration avec les autres domaines

Dépend de : aucun.

Utilisé par : `core.sales`, features UI.

---

## 8. Notes techniques

- Scoping: global (BaseEntity) ou tenant-scoped si mapping tenant.
- SDR possible (`/_sdr/pricing`).
