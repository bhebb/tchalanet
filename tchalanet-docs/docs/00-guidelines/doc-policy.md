# Politique documentaire Tchalanet

**Version**: 1.0.0 | **Date**: 2026-01-17

---

## Objectif

1. **Trouver vite** la bonne doc (IA / dev / business)
2. **Éviter la duplication** (une source de vérité par type d'info)
3. **Garder la doc à jour** en la mettant là où on travaille
4. **Avoir un workflow SDD rapide** (OpenSpec) sans polluer la doc centrale

---

## Les 3 espaces documentation

### A) Documentation centrale (MkDocs) — `tchalanet-docs/docs/`

**Contient** :

- ✅ Guidelines stables (constitution, règles IA)
- ✅ Architecture "maps" (où est quoi dans le code)
- ✅ Docs métier (domaines + workflows) lisibles business
- ✅ ADR (Architecture Decision Records)

**Ne contient pas** :

- ❌ How-to locaux, notes de debug quotidiennes
- ❌ Détails d'implémentation d'un module spécifique
- ❌ Docs qui changent chaque jour
- ❌ Specs features work-in-progress

**Mise à jour** : peu fréquente (règles stables)  
**Public** : dev, business, IA, externe

---

### B) Docs proches du code — `**/docs/*.md`, `**/README.md`

**Exemples** :

- `tchalanet-server/docs/*.md` (backend)
- `tchalanet-server/src/**/DOMAIN*.md`, `CACHE.md` (détails implémentation)
- `libs/**/README.md` (libs Nx)
- `tchalanet-infra/docs/*.md` (infra)
- `apps/tchalanet-web/docs/*.md` (web)

**Règle** :

> Si c'est utile **pendant que tu modifies ce composant** → doc ici.

**Contient** :

- ✅ Détails techniques (JPA, API, routing, state management)
- ✅ How-to spécifiques (setup, debug, troubleshooting)
- ✅ Notes d'implémentation (cache strategy, performance)
- ✅ Exemples de code

**Mise à jour** : fréquente (suit le code)  
**Public** : dev travaillant sur ce module

---

### C) Workflow SDD — `.specify/work/features/`

**Contient** :

- ✅ Specs features en cours (`FEAT-XXX/specify.md`)
- ✅ Plans d'implémentation (`plan.md`)
- ✅ Tasks (`tasks.md`)
- ✅ Notes de conception (`notes.md`)

**Règle** :

> `.specify/` = "atelier de construction", pas "livre officiel".

**Mise à jour** : quotidienne (work-in-progress)  
**Public** : dev feature, IA  
**Archivage** : une fois feature livrée → archiver ou supprimer

---

## Arborescence cible

### Documentation centrale (MkDocs)

```
tchalanet-docs/docs/
├── index.md
├── 00-guidelines/
│   ├── index.md
│   ├── constitution.md
│   ├── doc-policy.md         # Ce fichier
│   ├── ai-policy.md
│   └── glossary.md
├── 01-architecture/
│   ├── index.md
│   ├── system-overview.md
│   ├── backend-map.md
│   ├── frontend-map.md
│   ├── infra-map.md
│   └── security-model.md
├── 02-functional/
│   ├── index.md
│   ├── domains/
│   │   ├── index.md
│   │   ├── draw.md
│   │   ├── sales.md
│   │   ├── payout.md
│   │   ├── ledger.md
│   │   └── pagemodel.md
│   └── flows/
│       ├── index.md
│       ├── ticket-verify-public.md
│       └── results-pipeline.md
├── 03-adr/
│   ├── index.md
│   ├── ADR-0001-stack.md
│   └── ADR-0002-slot-first-results.md
└── 99-links/
    ├── index.md
    ├── backend.md
    ├── web.md
    ├── infra.md
    └── specs.md
```

### Workflow SDD (.specify/)

```
.specify/
├── constitution/
│   └── constitution.md       # Version courte (référence la centrale)
├── templates/
│   ├── openspec-feature.md
│   ├── plan.md
│   ├── tasks.md
│   └── prompts/
│       └── *.prompt.md
├── scripts/
│   ├── new-feature.sh
│   └── archive-feature.sh
└── work/
    └── features/
        └── FEAT-XXX/
            ├── specify.md
            ├── plan.md
            ├── tasks.md
            └── notes.md
```

---

## Règles de placement (décision rapide)

### Question 1 : C'est stable et partagé entre modules ?

➡️ **Oui** : `tchalanet-docs/docs/00-guidelines/` ou `01-architecture/`

### Question 2 : C'est du métier (domaine ou workflow) ?

➡️ **Oui** : `tchalanet-docs/docs/02-functional/domains/` ou `flows/`

### Question 3 : C'est une décision technique importante ?

➡️ **Oui** : `tchalanet-docs/docs/03-adr/`

### Question 4 : C'est technique spécifique à un module (backend/web/infra) ?

➡️ **Oui** : docs proches du code (`tchalanet-server/docs/`, `apps/*/docs/`, etc.)

### Question 5 : C'est une spec/plan en cours ?

➡️ **Oui** : `.specify/work/features/FEAT-XXX/`

---

## Règles de référencement croisé

### Depuis doc centrale → doc détaillée

✅ Utiliser des liens relatifs ou absolus vers `tchalanet-server/docs/`, `apps/*/docs/`

Exemple dans `tchalanet-docs/docs/99-links/backend.md` :

```markdown
## JPA & Persistence

Voir [tchalanet-server/docs/persistence.md](../../tchalanet-server/docs/persistence.md)
```

### Depuis doc détaillée → doc centrale

✅ Référencer la doc centrale pour contexte

Exemple dans `tchalanet-server/docs/api-controllers.md` :

```markdown
Pour les règles métier sales, voir [Documentation centrale - Domain Sales](../../tchalanet-docs/docs/02-functional/domains/sales.md)
```

### Docs fonctionnelles (agnostiques implémentation)

❌ **Ne jamais** inclure de code Java/TS dans `02-functional/`  
✅ Décrire **contrats**, **états**, **workflows** (diagrammes, tableaux)  
✅ Référencer les implémentations techniques via liens

---

## Workflow contributions

### Modifier doc centrale (MkDocs)

```bash
cd tchalanet-docs
pip install -r requirements.txt
mkdocs serve
# Modifier docs/*.md
# Commiter
```

### Modifier doc proche du code

```bash
# Naviguer vers le module
cd tchalanet-server  # ou apps/tchalanet-web, tchalanet-infra
# Modifier docs/*.md ou README.md
# Commiter avec le code
```

### Créer une spec feature (SDD)

```bash
# Utiliser script (à créer)
.specify/scripts/new-feature.sh FEAT-123 "Description courte"
# Éditer .specify/work/features/FEAT-123/specify.md
# Générer plan/tasks via agents SpecKit
```

---

## Migration ancien contenu

| Ancien emplacement                             | Nouveau                                                     |
| ---------------------------------------------- | ----------------------------------------------------------- |
| `tchalanet-docs/docs/architecture/overview.md` | `01-architecture/system-overview.md`                        |
| `tchalanet-docs/docs/backend/security.md`      | `99-links/backend.md` → `tchalanet-server/docs/security.md` |
| `tchalanet-docs/docs/web/dev-rules.md`         | `99-links/web.md` → `apps/tchalanet-web/docs/dev-rules.md`  |
| `.specify/conventions/*.md`                    | Migré vers docs centrales ou proches du code                |

---

## Checklist avant de créer un doc

- [ ] C'est stable et partagé ? → Doc centrale
- [ ] C'est technique spécifique à un module ? → Doc proche du code
- [ ] C'est work-in-progress ? → `.specify/work/`
- [ ] Pas de duplication avec existant
- [ ] Liens croisés clairs
- [ ] Mention "Dernière mise à jour" + version si pertinent

---

**Maintenu par** : équipe Tchalanet  
**Dernière mise à jour** : 2026-01-17
