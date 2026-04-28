# AGENTS.md — Tchalanet (Monorepo Source of Truth)

Ce fichier est la source de vérité pour toute IA (Copilot inclus) et tout contributeur.
Toute implémentation qui contredit ce document est considérée incorrecte
(sauf ADR explicite).

Ce fichier couvre TOUS les scopes :

- backend (tchalanet-server)
- frontend web (Nx / Angular)
- mobile (Flutter)
- infra / DevOps
- edge-service

Tchalanet est multi-frontend :

- web : Angular / Nx
- mobile : Flutter (cible future, a preparer sans refactor de logique metier)

Les specs OpenSpec sont la source de verite partagee entre backend, web et mobile.
Toute feature doit donc etre specifiee avant implementation, de facon agnostique a l'UI.

---

## OpenSpec integration (MANDATORY)

For planning, architectural changes, refactors, or new domains:

- Agents MUST follow the OpenSpec workflow
- See: `openspec/project.md` + `openspec/context/00-index.md`
- Changes MUST be proposed under `openspec/changes/`
- Direct implementation without proposal is forbidden
- Agent skills (openspec workflow): `.github/skills/` and `.claude/skills/`

Operational guardrails:

- Never code a feature without an OpenSpec change
- Never push directly to `main`
- Always work from a dedicated branch, ideally `feature/*` or `chore/*`
- Always validate with `pnpm ops:check`

---

## 0) Before ANY task (MUST — non négociable)

Avant de coder ou générer quoi que ce soit :

1. Lire **ce fichier** (`AGENTS.md`)
2. Lire **`VERSIONS.md`** (versions runtime/build/images)
3. Lire **`.github/copilot.md`** (checklist IA + pointeurs conventions)
4. Load `openspec/context/05-version-guard.md`
5. Verify:
   - language level
   - framework major version
   - available features
6. Reject deprecated or legacy APIs
7. Prefer modern constructs supported by the declared versions
8. Lire **`openspec/project.md`**
9. Charger **2–4 context packs max** depuis `openspec/context/`
10. Lire la doc proche du code concerné :
    - Backend domain : `tchalanet-server/src/**/DOMAIN_*.md`
    - Backend feature : `tchalanet-server/src/**/FEATURE_*.md`
    - Backend conventions : `tchalanet-server/docs/conventions/*`
    - Backend naming : `tchalanet-server/docs/NAMING.md`
    - Web : `apps/tchalanet-web/README.md` + `libs/**/README.md`
    - Infra : `tchalanet-infra/docs/**`
11. Ne PAS inventer de pattern s'il en existe déjà un

Si une règle doit être cassée → **ADR obligatoire** (`tchalanet-docs/docs/03-adr/`).

---

## 1) Architecture backend (STRICTE)

> ⚠️ Ce fichier est un **navigateur**, pas une source de contenu.
> Le contenu détaillé vit dans les sources canoniques listées ci-dessous.

**4 couches immuables** : `common/` · `catalog/` · `core/` · `features/`

Dépendances autorisées (enforced) :

- `features/` → `core/` et `catalog/`
- `core/` MUST NOT dépendre de `features/` ni de `catalog/`
- `catalog/` MUST NOT émettre d'events ni contenir d'invariants métier
- `common/` MUST NOT dépendre de `core/`, `features/` ou `catalog/`

- Règles de dépendances et structures internes :
  👉 `openspec/context/10-non-negotiables.md` (résumé non-négociable)
  👉 `tchalanet-server/docs/ARCHITECTURE.md` (source canonique complète)
  👉 `tchalanet-server/docs/PLAYBOOK.md` (comment livrer une feature, Definition of Done)
  👉 `tchalanet-server/CLAUDE.md` (checklist IA + commandes)

---

## 2) Versions Gate + Deprecated Guard (MUST)

**Source unique des versions** : 👉 **`VERSIONS.md` (root)**

Aucune version ne change sans mise à jour de `VERSIONS.md`.
Deprecated guard : 👉 `openspec/context/05-version-guard.md`

---

## 3) Backend — règles techniques (MUST)

**Sources canoniques** :

- `tchalanet-server/docs/ARCHITECTURE.md`
- `tchalanet-server/docs/conventions/*`
- `tchalanet-server/CLAUDE.md` (Do ✅ / Don't ❌ + commandes Maven)

Règles clés (résumé) :

- Constructor injection uniquement
- Controllers thin (validation + mapping + appel handler)
- `ApiResponse<T>` sur tous les controllers JSON ; `TchPage<T>` pour les collections (jamais Spring `Page`)
- Side-effects publiés **after-commit**
- Flyway obligatoire (`ddl-auto=validate`)
- Typed IDs partout sauf persistence
- ❌ `@Autowired` champ · `@Data` Lombok · `XxxDto` dans features · `/api/v1` dans `@RequestMapping`

- Catalogs (modules sous `catalog/`) MUST NOT expose or depend on application "ports" or use a global `QueryBus` for their read API.
  - Les catalogues exposent un contrat `catalog/<name>/api` (interfaces + `api.model`) et ont une implémentation interne (`catalog/<name>/internal/read`).
  - Toute utilisation de `QueryBus`, `CommandBus` ou d'`application.port` depuis d'autres modules vers un catalogue est interdite — les autres modules doivent appeler le `XCatalog` (interface) uniquement.

---

## 4) Web / Mobile — règles (MUST)

**Sources canoniques** :

- `apps/tchalanet-web/README.md`
- `libs/**/README.md`
- `apps/tchalanet-web/CLAUDE.md` · `tchalanet-mobile/CLAUDE.md`

Règles clés : mobile-first · i18n fr/en/ht · CSS variables uniquement · ❌ couleurs hardcodées.

Web (Angular 20) : composants `standalone: true` + `OnPush` · control flow moderne (`@if`/`@for`) · signaux (`signal()`, `computed()`, `effect()`) · widget renderer (`libs/ui/widget-renderer/`) pour page models.

Mobile : **Flutter** (Dart) + Riverpod + GoRouter · Material 3 · Capacitor (POS) · Android-first (terminal POS Motorola) · offline-first.

---

## 5) Infra / Edge (MUST)

**Sources canoniques** :

- `tchalanet-infra/docs/`
- `tchalanet-edge-service/README.md`
- `tchalanet-infra/CLAUDE.md` · `tchalanet-edge-service/CLAUDE.md`

Règles clés : pas de `:latest`, images pinées, toute version dans `VERSIONS.md`.

Services infra : PostgreSQL · Redis · Keycloak (auth) · Traefik (reverse proxy) · **Unleash 7.4** (feature flags) · **Meilisearch v1.11** (search).

Edge service : templates Liquid via **LiquidJS** dans `templates/` (convention `{channel}_{eventType}_{locale}.liquid`) · règles dans `rules/{eventType}.json` · canaux : WEB · EMAIL (Mailgun) · SMS (Bird).

---

## 6) Documentation rules (MUST)

- **Une seule source de vérité** par type d'info
- Règles stables → `tchalanet-docs/docs/`
- Détails techniques → docs proches du code
- SDD / specs en cours → `openspec/`

Doc hub canonique : **`DOCUMENTATION.md`**

---

## 7) Index rapide

- Versions : `VERSIONS.md`
- Doc hub : `DOCUMENTATION.md`
- Politique documentaire : `tchalanet-docs/docs/00-guidelines/doc-policy.md`
- Copilot context + checklist IA : `.github/copilot.md`
- OpenSpec : `openspec/project.md`, `openspec/context/*`, `openspec/specs/*`, `openspec/changes/`
- Agent skills (openspec workflow) : `.github/skills/`, `.claude/skills/`
- Domain truth : `tchalanet-server/src/**/DOMAIN_*.md`
- Feature truth : `tchalanet-server/src/**/FEATURE_*.md`
- Backend conventions : `tchalanet-server/docs/conventions/*`
- Backend playbook : `tchalanet-server/docs/PLAYBOOK.md`
- Central docs : `tchalanet-docs/docs/*`
- Scope CLAUDE.md : `tchalanet-server/CLAUDE.md` · `apps/tchalanet-web/CLAUDE.md` · `tchalanet-mobile/CLAUDE.md` · `tchalanet-infra/CLAUDE.md` · `tchalanet-edge-service/CLAUDE.md` · `tchalanet-docs/CLAUDE.md`
