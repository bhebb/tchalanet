# Domaine Catalog Game

> Référentiel des jeux, leurs codes, et options configurables. Lookup utilisé par ventes (sales) et tirages (draw). Peu de logique; admin CRUD pour gérer le catalogue.

---

## 1. Rôle du domaine

**Responsabilité principale**

> Maintenir le registre des jeux (gameCode, labels, options) et fournir un lookup cohérent pour les domaines `sales` et `draw`.

**Ce que le domaine fait**

- Liste et décrit les jeux disponibles.
- Gère les options/configurations de jeu (référentiel).
- (Optionnel) tenant-game mapping si certaines options sont spécifiques à un tenant.

**Ce que le domaine ne fait pas**

- Ne calcule pas les résultats ni les gains.
- Ne gère pas la vente — `core.sales`.

---

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- `Game` — (id, gameCode, name, optionsJson, status).
- (Optionnel) `TenantGame` — mapping tenant ↔ game (si scoping tenant).

### Invariants métier

- `gameCode` unique (global).

> Valeur métier clé :
> Servir de base référentielle stable pour les opérations de vente et tirage.

---

## 3. Cas d’utilisation (ports d’entrée)

- `ListGamesQuery` — lister les jeux.
- `GetGameQuery` — obtenir un jeu par code.
- (Admin) `CreateOrUpdateGameCommand` — maintenir le catalogue de jeux.

---

## 4. Ports de sortie (dépendances externes)

- `GameReaderPort` — lecture référentiel.
- `GameWriterPort` — écriture référentiel (admin).

---

## 5. Mapping & DTOs (convention)

- MapStruct pour mapper entity ↔ projection `GameResponse`.
- DTO d’entrée admin: `GameRequest`.
- IDs wrappers côté web; UUID en JPA.

---

## 6. Règles métier importantes

- Les jeux actifs doivent être visibles aux domaines `sales` et `draw`.
- Les options JSON doivent être validées (schéma) pour éviter incohérences.

---

## 7. Intégration avec les autres domaines

Dépend de : aucun.

Utilisé par : `core.sales` (vente), `core.draw` (tirages), features (pagemodel/widgets).

---

## 8. Notes techniques

- Scoping: global (BaseEntity) ou tenant-scoped (`TenantGame`, BaseTenantEntity) si nécessaire.
- SDR possible (`/_sdr/games`) pour admin.
