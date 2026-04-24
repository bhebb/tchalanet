<!-- OPENSPEC:START -->

# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:

- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:

- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

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

## OpenSpec integration (MANDATORY)

For planning, architectural changes, refactors, or new domains:

- Agents MUST follow the OpenSpec workflow
- See: `openspec/AGENTS.md`
- Changes MUST be proposed under `openspec/changes/`
- Direct implementation without proposal is forbidden

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
   - Backend naming : `tchalanet-server/docs/NAMING.md`
   - Web : `apps/tchalanet-web/README.md` + `libs/**/README.md`
   - Infra : `tchalanet-infra/docs/**`
10. Ne PAS inventer de pattern s'il en existe déjà un

Si une règle doit être cassée → **ADR obligatoire** (`tchalanet-docs/docs/03-adr/`).

---

## 1) Architecture backend (STRICTE)

> ⚠️ Ce fichier est un **navigateur**, pas une source de contenu.
> Le contenu détaillé vit dans les sources canoniques listées ci-dessous.

**4 couches immuables** : `common/` · `catalog/` · `core/` · `features/`

- Règles de dépendances et structures internes :
  👉 `openspec/context/10-non-negotiables.md` (résumé non-négociable)
  👉 `tchalanet-server/docs/ARCHITECTURE.md` (source canonique complète)
  👉 `.claude/skills/backend-architecture/SKILL.md` (fiche IA actionable)

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
- `.claude/skills/backend-*/SKILL.md` (checklists IA)

Règles clés (résumé) :

- Constructor injection uniquement
- Controllers thin (validation + mapping + appel handler)
- Side-effects publiés **after-commit**
- Flyway obligatoire (`ddl-auto=validate`)
- Typed IDs partout sauf persistence

---

## 4) Web / Mobile — règles (MUST)

**Sources canoniques** :

- `apps/tchalanet-web/README.md`
- `libs/**/README.md`
- `.claude/skills/frontend-web/SKILL.md`

Règles clés : mobile-first · i18n fr/en/ht · CSS variables uniquement · ❌ couleurs hardcodées.

---

## 5) Infra / Edge (MUST)

**Sources canoniques** :

- `tchalanet-infra/docs/`
- `tchalanet-edge-service/README.md`
- `.claude/skills/infrastructure/SKILL.md`

Règles clés : pas de `:latest`, images pinées, toute version dans `VERSIONS.md`.

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
- OpenSpec : `openspec/project.md`, `openspec/context/*`, `openspec/specs/*`
- Domain truth : `tchalanet-server/src/**/DOMAIN_*.md`
- Feature truth : `tchalanet-server/src/**/FEATURE_*.md`
- Backend conventions : `tchalanet-server/docs/conventions/*`
- Central docs : `tchalanet-docs/docs/*`
