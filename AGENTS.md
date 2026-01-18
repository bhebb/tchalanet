# AGENTS.md — Tchalanet (Monorepo Source of Truth)

Ce fichier est la source de vérité pour toute IA (Copilot inclus) et tout contributeur.
Toute implémentation qui contredit ce document est considérée incorrecte
(sauf ADR explicite).

Ce fichier couvre TOUS les scopes :

- backend (tchalanet-server)
- frontend web (Nx / Angular)
- mobile (Ionic)
- infra / DevOps
- edge-service

---

## 0) Before ANY task (MUST — non négociable)

Avant de coder ou générer quoi que ce soit :

1. Lire **ce fichier** (`AGENTS.md`)
2. Lire **`VERSIONS.md`** (versions runtime/build/images)
3. Load `openspec/context/05-version-guard.md`
4. Verify:
   - language level
   - framework major version
   - available features
5. Reject deprecated or legacy APIs
6. Prefer modern constructs supported by the declared versions
7. Lire **`openspec/project.md`**
8. Charger **2–4 context packs max** depuis `openspec/context/`
9. Lire la doc proche du code concerné :
   - Backend domain : `tchalanet-server/src/**/DOMAIN_*.md`
   - Backend feature : `tchalanet-server/src/**/FEATURE_*.md`
   - Backend conventions : `tchalanet-server/docs/conventions/*`
   - Web : `apps/tchalanet-web/README.md` + `libs/**/README.md`
   - Infra : `tchalanet-infra/docs/**`
10. Ne PAS inventer de pattern s’il en existe déjà un

Si une règle doit être cassée → **ADR obligatoire** (`tchalanet-docs/docs/03-adr/`).

---

## 1) Architecture backend (STRICTE)

Le backend respecte **4 couches** immuables :

- `common/`  
  → technique transversal (context, errors, bus, tx, cache infra)  
  ❌ aucune logique métier

- `catalog/`  
  → référentiels / lookup / read-mostly  
  ❌ pas d’events métier, ❌ pas de calculs d’argent

- `core/`  
  → domaines critiques (hexagonal + CQRS)  
  ✔ ventes, tirages, résultats, argent, limites, audit, sécurité

- `features/`  
  → orchestration / BFF / pages / agrégation multi-domaines  
  ❌ pas de duplication de règles métier critiques

**Règles de dépendances** :

- `core/` ❌ dépend de `features/`
- `catalog/` ❌ side-effects
- `features/` ✔ orchestre `core/`, lit `catalog/`
- `common/` ❌ dépend du métier

---

## 2) Versions Gate + Deprecated Guard (MUST)

### Source de vérité

👉 **`VERSIONS.md` (root)**

Aucune version (framework, lib, runtime, image Docker) ne change sans :

1. mise à jour de `VERSIONS.md`
2. mise à jour du wrapper/pin correspondant
3. mise à jour des images Docker si concerné
4. note d’impact si prod

### Deprecated Guard

- Interdiction d’introduire des APIs **deprecated**
- Toujours utiliser les **features de la version pin**
- Si un deprecated est inévitable :
  - commenter WHY
  - ouvrir une ADR ou un ticket

---

## 3) Backend — règles techniques (MUST)

### Stack

- Java **25**
- Spring Boot **4.x**
- Maven (`./mvnw`)
- MapStruct (mappers)
- Lombok (boilerplate uniquement, éviter `@Data`)
- JPA/Hibernate + Flyway
- Redis + Caffeine
- Spring Batch

### Règles clés

- Constructor injection uniquement
- Controllers thin (validation + mapping + appel handler)
- Side-effects publiés **after-commit**
- Flyway obligatoire (`ddl-auto=validate`)
- Wrappers d’ID partout (UUID brut uniquement dans entities/repos)

### Tests

- AssertJ uniquement
- `@Nested` pour scénarios
- Préférer ports in-memory aux mocks lourds

📌 Source de vérité :
`tchalanet-server/docs/ARCHITECTURE.md`  
`tchalanet-server/docs/conventions/*`

---

## 4) Web / Mobile — règles (MUST)

### Stack web

- Node 20.19.x
- pnpm 10.19.0 (pin via `packageManager`)
- Nx 21.x
- Angular 20.x + Material

### Règles UI

- Mobile-first (480 / 768 / 1024)
- i18n fr / en / ht (snake_case, namespaces)
- Theming :
  - base SCSS unique
  - overrides via CSS variables
  - ❌ aucune couleur hardcodée
- Widgets/pages :
  - rendu via renderer
  - ❌ logique widget dans pages

📌 Source de vérité :
`apps/tchalanet-web/README.md`  
`libs/**/README.md`

---

## 5) Infra / Edge (MUST)

- Pas de `:latest`
- Images Docker pinées
- Toute image = référencée dans `VERSIONS.md`
- Config documentée dans `tchalanet-infra/docs/`
- Edge-service : `tchalanet-edge-service/README.md`

---

## 6) Documentation rules (MUST)

- **Une seule source de vérité** par type d’info
- Règles stables → `tchalanet-docs/docs/`
- Détails techniques → docs proches du code
- SDD / specs en cours → `openspec/`

Doc hub canonique : **`DOCUMENTATION.md`**

---

## 7) Index rapide

- Versions : `VERSIONS.md`
- Doc hub : `DOCUMENTATION.md`
- OpenSpec : `openspec/project.md`, `openspec/context/*`, `openspec/specs/*`
- Domain truth : `tchalanet-server/src/**/DOMAIN_*.md`
- Feature truth : `tchalanet-server/src/**/FEATURE_*.md`
- Backend conventions : `tchalanet-server/docs/conventions/*`
- Central docs : `tchalanet-docs/docs/*`
