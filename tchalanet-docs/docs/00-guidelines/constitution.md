# Constitution Tchalanet

**Version**: 1.0.0 | **Date**: 2026-01-17

---

## 1. Sources de vérité

- **`.specify/`** est la source canonique pour workflows SDD, templates, scripts
- **`tchalanet-docs/docs/`** est la documentation centrale publiée (MkDocs)
- **Docs proches du code** (`tchalanet-server/docs/`, `apps/*/docs/`, etc.) sont la vérité détaillée technique
- Les docs publiques pointent vers les docs détaillées

---

## 2. Architecture

### Backend

- **Hexagonal** (Ports & Adapters) + **CQRS** (CommandBus/QueryBus)
- **Modules** : `common/`, `core/<domain>`, `features/<slice>`, `catalog/<name>`
- **Argent / tirages / conformité** = `core/` (MUST)

### Frontend

- **BFF PageModel** + **widgets dynamiques**
- **Web** : Angular + Nx + Material + Tailwind
- **Mobile** : Ionic + Angular + Capacitor

### Infra

- **PostgreSQL** (RLS multi-tenant)
- **Keycloak** (auth centralisée)
- **Docker Compose** (dev local)
- **Traefik** (reverse proxy)

---

## 3. Règles IA (non négociables)

1. **MUST read** `AGENTS.md` (racine) avant toute action
2. **MUST read** `.specify/constitution/constitution.md` (workflow SDD)
3. **MUST read** cette constitution (centrale MkDocs)
4. **MUST** respecter placement des modules (`common`, `core`, `features`, `catalog`)
5. **MUST NOT** inventer de patterns (suivre `ARCHITECTURE.md` et `.specify/`)
6. **Si ambigu** → proposer max 2 options, puis appliquer la solution la plus safe
7. **MUST** documenter toute décision (pas de règle implicite)

---

## 4. Évolution

### Principe

- **Une règle cassée** = doc mise à jour **immédiatement**
- **Pas de règle implicite** (tout doit être écrit)
- **Exceptions** justifiées dans ADR ou spec feature

### Workflow changement

1. Identifier la règle violée
2. Proposer alternatives (max 2)
3. Choisir la plus safe
4. Documenter dans ADR ou spec
5. Mettre à jour constitution/ARCHITECTURE.md si nécessaire

---

## 5. Organisation documentation

### Documentation centrale (MkDocs) — `tchalanet-docs/docs/`

- **Rôle** : guidelines stables, architecture maps, docs métier, ADR
- **Public** : dev, business, IA
- **Mise à jour** : peu fréquente (règles stables)

### Docs proches du code — `**/docs/*.md`, `**/README.md`

- **Rôle** : détails techniques, how-to, notes de debug
- **Public** : dev travaillant sur ce module
- **Mise à jour** : fréquente (évolution code)

### Workflow SDD — `.specify/work/features/`

- **Rôle** : specs, plans, tasks en cours
- **Public** : dev feature, IA
- **Mise à jour** : quotidienne (work-in-progress)
- **Archivage** : une fois feature livrée, archiver ou supprimer

---

## 6. Références

- **AGENTS.md** (racine) : règles backend Spring Boot / Java
- **ARCHITECTURE.md** (racine) : architecture hexagonale, modules, patterns
- **PLAYBOOK.md** (racine) : guide opérationnel humains + IA
- **.specify/index.md** : organisation `.specify/`
- **tchalanet-docs/docs/** : cette documentation centrale

---

**Maintenu par** : équipe Tchalanet  
**Dernière ratification** : 2026-01-17
