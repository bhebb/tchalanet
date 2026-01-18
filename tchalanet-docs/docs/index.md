# Documentation Tchalanet

**Version**: 1.0.0 | **Date**: 2026-01-17

Bienvenue dans la documentation technique et fonctionnelle du projet Tchalanet.

---

## 🎯 Principe d'organisation

Cette documentation centrale (MkDocs) contient :

- ✅ **Guidelines stables** (constitution, politiques, règles IA)
- ✅ **Architecture "maps"** (où est quoi dans le code)
- ✅ **Documentation métier** (domaines + workflows)
- ✅ **ADR** (décisions d'architecture)

Cette documentation **ne contient pas** :

- ❌ How-to locaux / notes de debug quotidiennes
- ❌ Détails d'implémentation spécifiques à un module (voir docs proches du code)
- ❌ Work-in-progress specs/plans (voir `.specify/work/`)

---

## 📚 Navigation rapide

### [00 - Guidelines](00-guidelines/index.md)

Constitution, politique documentaire, règles IA, glossaire métier

### [01 - Architecture](01-architecture/index.md)

Vue d'ensemble système, maps backend/frontend/infra, modèle de sécurité

### [02 - Fonctionnel](02-functional/index.md)

Domaines métier (draw, sales, payout, ledger) et workflows transverses

### [03 - ADR](03-adr/index.md)

Architecture Decision Records (décisions techniques importantes)

### [99 - Liens](99-links/index.md)

Pointeurs vers les docs détaillées (backend, web, infra, specs)

---

## 🗺️ Où trouver quoi ?

| Sujet                                | Où chercher                                                              |
| ------------------------------------ | ------------------------------------------------------------------------ |
| **Constitution projet**              | [00-guidelines/constitution.md](00-guidelines/constitution.md)           |
| **Politique doc (ce guide)**         | [00-guidelines/doc-policy.md](00-guidelines/doc-policy.md)               |
| **Architecture globale**             | [01-architecture/system-overview.md](01-architecture/system-overview.md) |
| **Domaines métier**                  | [02-functional/domains/](02-functional/domains/index.md)                 |
| **Workflows (vente ticket, payout)** | [02-functional/flows/](02-functional/flows/index.md)                     |
| **Décisions techniques**             | [03-adr/](03-adr/index.md)                                               |
| **Détails backend (JPA, API, etc.)** | [99-links/backend.md](99-links/backend.md) → `tchalanet-server/docs/`    |
| **Détails web (Angular, routing)**   | [99-links/web.md](99-links/web.md) → `apps/tchalanet-web/docs/`          |
| **Détails infra (Docker, Keycloak)** | [99-links/infra.md](99-links/infra.md) → `tchalanet-infra/docs/`         |
| **Specs features en cours**          | `.specify/work/features/` (workflow SDD)                                 |

---

## 🚀 Pour commencer

### Développeur backend (Spring Boot)

1. Lire [00-guidelines/constitution.md](00-guidelines/constitution.md)
2. Lire [01-architecture/backend-map.md](01-architecture/backend-map.md)
3. Suivre les liens vers `tchalanet-server/docs/` pour détails techniques
4. Consulter [02-functional/domains/](02-functional/domains/index.md) pour règles métier

### Développeur frontend (Angular/Ionic)

1. Lire [00-guidelines/constitution.md](00-guidelines/constitution.md)
2. Lire [01-architecture/frontend-map.md](01-architecture/frontend-map.md)
3. Suivre les liens vers `apps/*/docs/` pour détails techniques
4. Consulter [02-functional/flows/](02-functional/flows/index.md) pour workflows UI/UX

### Agent IA (Copilot/ChatGPT)

1. **MUST read** [00-guidelines/ai-policy.md](00-guidelines/ai-policy.md)
2. **MUST read** `AGENTS.md` (racine du repo)
3. **MUST read** `.specify/constitution/constitution.md`
4. Identifier le module technique cible → suivre liens vers docs détaillées
5. Identifier le domaine fonctionnel → lire [02-functional/domains/](02-functional/domains/index.md)

### Business / Product

1. Lire [00-guidelines/glossary.md](00-guidelines/glossary.md) (glossaire métier)
2. Lire [02-functional/domains/](02-functional/domains/index.md) (domaines métier)
3. Lire [02-functional/flows/](02-functional/flows/index.md) (workflows utilisateur)

---

## 📝 Contribuer à la documentation

### Documentation centrale (MkDocs)

```bash
cd tchalanet-docs
pip install -r requirements.txt
mkdocs serve
# Ouvrir http://127.0.0.1:8000
```

Modifier les fichiers dans `docs/` et commiter.

### Documentation proche du code

Modifier directement les README.md ou fichiers `docs/*.md` dans le module concerné :

- Backend : `tchalanet-server/docs/*.md`
- Web : `apps/tchalanet-web/docs/*.md`
- Infra : `tchalanet-infra/docs/*.md`
- Libs : `libs/*/README.md`

### Specs features (SDD workflow)

Utiliser `.specify/work/features/FEAT-XXX/` pour les specs en cours.  
Ne **pas** polluer la doc centrale avec des specs non finalisées.

---

## 🔗 Liens rapides

- [GitHub](https://github.com/votre-org/tchalanet) (repo principal)
- [CI/CD](https://github.com/votre-org/tchalanet/actions) (GitHub Actions)
- [Swagger API](http://localhost:8080/swagger-ui.html) (dev local)
- [Keycloak Admin](http://localhost:8180) (dev local)

---

**Maintenu par** : équipe Tchalanet  
**Dernière mise à jour** : 2026-01-17
