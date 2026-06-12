# Tchalanet — Server Documentation

Ce dossier contient la **documentation canonique** du backend **tchalanet-server**.

Son objectif est de :

- stabiliser l'architecture,
- éviter le _vibe coding_,
- guider les développeurs **et** les agents IA,
- permettre d'écrire des specs (OpenSpec) **sans rediscuter les bases**.

---

## 🚦 Ordre de priorité (source de vérité)

En cas de conflit entre documents :

1. **ARCHITECTURE.md**  
   → Frontières, responsabilités, règles structurantes (constitution)

2. **PLAYBOOK.md**  
   → Comment travailler, livrer une feature, Definition of Done

3. **conventions/**  
   → Règles techniques **normatives** (API, pagination, cache, RLS, etc.)

4. **OpenSpec**  
   → Contrat dérivé (jamais source de vérité)

> ⚠️ OpenSpec ne redéfinit jamais l'architecture ni les conventions.

---

## 🧭 Start here (nouvel arrivant / IA)

Si tu découvres le projet :

1. Lire **ARCHITECTURE.md**
2. Lire **PLAYBOOK.md**
3. Parcourir **conventions/** selon ton besoin (API, pagination, cache…)

C'est suffisant pour contribuer sans poser de questions d'architecture.

---

## 🧱 Documents racine

### ARCHITECTURE.md

- Structure globale (`common / catalog / core / features`)
- Frontières et dépendances
- Règles **non négociables**
- API versioning (`/api/v1`)
- Contexte, sécurité, RLS
- Cache, batch, after-commit

👉 À lire **avant** toute implémentation structurante.

---

### PLAYBOOK.md

- Règles de contribution
- Do / Don't
- Definition of Done backend
- Guide opératoire (ajouter une API, un handler, un batch, etc.)
- Contrat IA (ce que Copilot/IA doit et ne doit pas faire)

👉 À suivre **à chaque feature**.

---

## 📐 conventions/ — Normes techniques (NORMATIF)

Le dossier `conventions/` contient des règles **obligatoires**.
Si un sujet est défini ici, il ne doit **pas** être redéfini ailleurs.

### Organisation logique

#### 🌐 HTTP / API

- `web_api.md` — règles controllers, scopes, erreurs
- `api_response.md` — `ApiResponse<T>` (2xx)
- `pagination.md` — pagination standard
- `routing_and_path.md` — paths, scopes, `/api/v1`

#### ⚙️ Exécution / CQRS

- `command_query_handlers.md` — Command/Query/Bus/Handlers
- `idempotency.md` — idempotency commands & batch
- `batch.md` — jobs & orchestration

#### 🎨 Conventions générales

- `NAMING.md` — conventions de nommage (classes, méthodes, variables)

#### 🔐 Sécurité / Contexte / RLS

- `context.md` — `TchRequestContext`
- `identity-providers.md` — Firebase, Keycloak, local JWT/perf, provisioning et tests
- `security_permissions.md` — permissions & rôles
- `rls.md` — Row Level Security (dernière ligne de défense)

#### 💾 Persistence / Data

- `persistence.md` — règles DB & Flyway
- `jpa_entities.md` — entités JPA
- `typed_ids.md` — wrappers d'ID (règle structurante)

#### ⚡ Platform

- `cache.md` — Caffeine + Redis
- `audit.md` — audit actions & Envers
- `testing.md` — standards de tests

---

## 🗺️ Où chercher quoi ?

| Besoin                         | Document                                |
| ------------------------------ | --------------------------------------- |
| Où placer mon code ?           | `ARCHITECTURE.md`                       |
| Comment ajouter une feature ?  | `PLAYBOOK.md`                           |
| Comment écrire un controller ? | `conventions/web_api.md`                |
| Format des réponses HTTP       | `conventions/api_response.md`           |
| Pagination                     | `conventions/pagination.md`             |
| Erreurs / ProblemDetail        | `conventions/web_api.md`                |
| CQRS / handlers                | `conventions/command_query_handlers.md` |
| Conventions de nommage         | `NAMING.md`                             |
| Cache                          | `conventions/cache.md`                  |
| Batch                          | `conventions/batch.md`                  |
| Sécurité / RLS                 | `conventions/context.md` + `rls.md`     |
| Configurer/tester l'auth       | `conventions/identity-providers.md`     |
| Typed IDs                      | `conventions/typed_ids.md`              |

---

## 📌 Documentation near-code (DOMAIN_*.md / CATALOG_*.md / PLATFORM_*.md / FEATURE_*.md)

Chaque domaine/capability dispose d'un fichier normatif co-localisé avec son code :

| Slice | Préfixe | Lieu |
|---|---|---|
| `core/` | `DOMAIN_*.md` | `core/<domain>/DOMAIN_<NAME>.md` |
| `catalog/` | `CATALOG_*.md` | `catalog/<bc>/CATALOG_<NAME>.md` |
| `platform/` | `PLATFORM_*.md` | `platform/<cap>/PLATFORM_<NAME>.md` |
| `features/` | `FEATURE_*.md` | `features/<feat>/FEATURE_<NAME>.md` |

Ces fichiers documentent enums, modèles, invariants, commandes/queries, et intégrations réels. Ils doivent être mis à jour quand l'API du domaine change.

Templates : `docs/DOMAIN_TEMPLATE.md`, `docs/PLATFORM_TEMPLATE.md`.

---

## 🤖 Contrat IA (résumé)

Tout agent IA contribuant au repo doit :

- Lire **ARCHITECTURE.md** et **PLAYBOOK.md** en premier
- Respecter **toutes** les conventions
- Utiliser les wrappers d'ID
- Passer par CommandBus / QueryBus
- Publier les événements after-commit
- Ne jamais inventer de patterns
- Ne jamais bypasser RLS

Les règles détaillées sont dans `../AGENTS.md` (tchalanet-server router).

---

## 📝 Modifier ou ajouter une convention

Quand une nouvelle règle est nécessaire :

1. Ajouter / modifier un fichier dans `conventions/`
2. Mettre à jour **ARCHITECTURE.md** si la règle est structurante
3. Mettre à jour **PLAYBOOK.md** si l'impact est opérationnel
4. Ajouter une note si cela affecte OpenSpec

Aucune règle implicite n'est acceptée.

---

## ✅ Résumé

- **ARCHITECTURE** = quoi et pourquoi
- **PLAYBOOK** = comment faire
- **conventions/** = règles techniques détaillées
- **OpenSpec** = specs dérivées (jamais source)

Toute contribution suit ces documents, sans exception.
