# CLAUDE.md — Tchalanet (racine)

> Avant toute tâche : lire aussi `AGENTS.md` et `openspec/AGENTS.md`.
> Chaque sous-projet a son propre CLAUDE.md — lire celui du scope concerné.

---

## Stack (versions canoniques → `VERSIONS.md`)

| Scope        | Stack                                                        |
| ------------ | ------------------------------------------------------------ |
| Backend      | Java 25 · Spring Boot 4.0.1 · Maven · PostgreSQL 18.1        |
| Frontend Web | Angular 20.2 · Nx 21 · NgRx · pnpm 10.19 · Node 20.19        |
| Mobile       | Flutter · Riverpod · GoRouter · Material 3 · Capacitor (POS) |
| Edge Service | Node 20.19 · TypeScript · Express 4 · json-rules-engine 7    |
| Infra        | Docker Compose v2 · Traefik v3.6 · Keycloak 26 · Redis 8.4   |

---

## Règles transverses (NON-NÉGOCIABLES)

- **OpenSpec obligatoire** pour tout nouveau capability, changement breaking, refactoring archi → `openspec/AGENTS.md`
- **Images Docker pinées** — jamais `:latest` — versions dans `VERSIONS.md`
- **Secrets via Doppler uniquement** — rien dans le code, les specs, ni la doc
- **Jakarta** (`jakarta.*`) — jamais `javax.*`
- **RLS PostgreSQL** — l'isolation tenant ne passe jamais par du code Java
- Toute exception → **ADR** dans `tchalanet-docs/docs/03-adr/`

---

## Sous-projets

| Sous-projet                    | CLAUDE.md                          |
| ------------------------------ | ---------------------------------- |
| Backend Spring Boot            | `tchalanet-server/CLAUDE.md`       |
| Frontend Angular               | `apps/tchalanet-web/CLAUDE.md`     |
| Mobile Flutter / POS           | `tchalanet-mobile/CLAUDE.md`       |
| Infrastructure                 | `tchalanet-infra/CLAUDE.md`        |
| Edge Service (règles + notifs) | `tchalanet-edge-service/CLAUDE.md` |
| Documentation (MkDocs)         | `tchalanet-docs/CLAUDE.md`         |

---

## Skills globaux (`.claude/skills/`)

| Skill               | Déclenchement                         |
| ------------------- | ------------------------------------- |
| `openspec-proposal` | Créer une proposal de changement      |
| `openspec-apply`    | Implémenter une proposal approuvée    |
| `openspec-archive`  | Archiver une change après déploiement |
| `documentation`     | Trouver ou écrire de la doc           |
