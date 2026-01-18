# Guide Documentation Tchalanet

Organisation de la documentation technique et fonctionnelle  
**Audience** : IA / dev / produit / business

**Version**: 1.2.0  
**Date**: 2026-01-17

---

## 🎯 Principes

1. Trouver vite la bonne doc
2. Une seule source de vérité par type d’info
3. Doc proche du code quand c’est technique
4. SDD rapide via OpenSpec sans polluer la doc centrale
5. Versions contrôlées et explicites

---

## 🔒 Versions & compatibilité (OBLIGATOIRE)

Toutes les versions runtime/build/images sont canonisées dans :

👉 **`VERSIONS.md` (root)**

Toute PR qui modifie :

- une dépendance
- un framework
- une image Docker
- un runtime (Java, Node…)

DOIT mettre à jour `VERSIONS.md` et utiliser les APIs compatibles
(éviter tout deprecated).

---

## 📚 Les 3 espaces de documentation

### 1) Documentation centrale (MkDocs)

📍 `tchalanet-docs/docs/`

**Contient** :

- Constitution & règles IA
- Architecture maps
- Domaines métier (business-readable)
- Workflows bout-en-bout
- ADR

👉 Stable, publiée, lisible business.

---

### 2) Docs proches du code

📍 `**/docs/*.md`, `**/README.md`

**Contient** :

- Détails techniques
- API, persistence, cache, batch
- Setup, debug, troubleshooting
- Exemples concrets

**Emplacements** :

- Backend : `tchalanet-server/docs/` + `src/**/DOMAIN_*.md`
- Web : `apps/tchalanet-web/*.md` + `libs/**/README.md`
- Infra : `tchalanet-infra/docs/`
- Edge : `tchalanet-edge-service/README.md`

👉 Source de vérité technique.

---

### 3) SDD — OpenSpec

📍 `openspec/`

**Contient** :

- Contexte projet (`openspec/project.md`)
- Context packs (`openspec/context/*`)
- Specs features en cours (`openspec/specs/*`)

👉 Atelier de conception + IA, pas doc officielle.

---

## 🗺️ Où mettre quoi ?

| Question                  | Réponse            |
| ------------------------- | ------------------ |
| Règle stable partagée ?   | MkDocs             |
| Métier / domaine ?        | MkDocs             |
| Décision structurante ?   | ADR                |
| Détail technique module ? | Doc proche du code |
| Feature en cours ?        | OpenSpec           |

---

## 📖 Navigation rapide

| Sujet               | Lien                                                |
| ------------------- | --------------------------------------------------- |
| Règles globales IA  | `AGENTS.md`                                         |
| Versions            | `VERSIONS.md`                                       |
| Doc hub             | `DOCUMENTATION.md`                                  |
| Constitution        | `tchalanet-docs/docs/00-guidelines/constitution.md` |
| Architecture maps   | `tchalanet-docs/docs/01-architecture/`              |
| Domaines métier     | `tchalanet-docs/docs/02-functional/domains/`        |
| Workflows métier    | `tchalanet-docs/docs/02-functional/flows/`          |
| ADR                 | `tchalanet-docs/docs/03-adr/`                       |
| Backend archi       | `tchalanet-server/docs/ARCHITECTURE.md`             |
| Backend conventions | `tchalanet-server/docs/conventions/`                |
| Web docs            | `apps/tchalanet-web/README.md`                      |
| Infra docs          | `tchalanet-infra/docs/`                             |
| OpenSpec            | `openspec/project.md`                               |
| Specs               | `openspec/specs/`                                   |

---

## 🚀 Quick Start (dev)

```bash
cat AGENTS.md
cat VERSIONS.md
cat openspec/project.md
Identifier scope & domaine

Lire la doc métier (MkDocs)

Lire la doc near-code

Créer/mettre à jour la spec :

openspec init FEAT-123 "Description"
openspec plan FEAT-123
openspec tasks FEAT-123


Implémenter en respectant AGENTS + versions

Mettre à jour la doc si nécessaire
```
