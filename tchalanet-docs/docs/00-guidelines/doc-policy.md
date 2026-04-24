# Politique documentaire Tchalanet

**Version**: 1.1.0 | **Date**: 2026-04-23

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

## Workflow de modification d'une règle

> Quand une règle technique ou métier change, suivre cette cascade dans l'ordre.

### Étape 1 — Modifier la source canonique near-code

C'est **toujours** le premier fichier à modifier.

| Type de règle                               | Fichier à modifier en premier                                  |
| ------------------------------------------- | -------------------------------------------------------------- |
| Architecture backend (couches, dépendances) | `tchalanet-server/docs/ARCHITECTURE.md`                        |
| Nommage Java                                | `tchalanet-server/docs/NAMING.md`                              |
| Typed IDs                                   | `tchalanet-server/docs/conventions/typed_ids.md`               |
| RLS / multi-tenant                          | `tchalanet-server/docs/conventions/persistence/rls.md`         |
| Events / effets de bord                     | `tchalanet-server/docs/conventions/event_model.md`             |
| Tests                                       | `tchalanet-server/docs/conventions/testing.md`                 |
| Persistence / Flyway                        | `tchalanet-server/docs/conventions/persistence/persistence.md` |
| Frontend Angular                            | `apps/tchalanet-web/README.md` ou `libs/**/README.md`          |
| Infra / Docker                              | `tchalanet-infra/docs/`                                        |
| Edge service                                | `tchalanet-edge-service/README.md`                             |
| Domaine métier                              | `tchalanet-server/src/**/DOMAIN_*.md`                          |

### Étape 2 — Mettre à jour le skill IA correspondant

Après avoir modifié la convention near-code, mettre à jour le résumé `.claude/skills/*/SKILL.md` correspondant.

> ⚠️ Ne jamais modifier un skill sans avoir d'abord modifié la convention source.

| Convention modifiée | Skill à mettre à jour                          |
| ------------------- | ---------------------------------------------- |
| `ARCHITECTURE.md`   | `.claude/skills/backend-architecture/SKILL.md` |
| `NAMING.md`         | `.claude/skills/backend-naming/SKILL.md`       |
| `typed_ids.md`      | `.claude/skills/backend-typed-ids/SKILL.md`    |
| `rls.md`            | `.claude/skills/backend-rls/SKILL.md`          |
| `event_model.md`    | `.claude/skills/backend-events/SKILL.md`       |
| `testing.md`        | `.claude/skills/backend-testing/SKILL.md`      |
| `persistence.md`    | `.claude/skills/backend-persistence/SKILL.md`  |

### Étape 3 — Mettre à jour la doc fonctionnelle (si impact métier)

Seulement si la règle a un impact visible côté business ou architecture globale :

- Domaine ou workflow → `tchalanet-docs/docs/02-functional/`
- Décision structurante → `tchalanet-docs/docs/03-adr/` (ADR obligatoire)
- Architecture map → `tchalanet-docs/docs/01-architecture/`

> ❌ Ne jamais commencer par la doc fonctionnelle.  
> ❌ Ne jamais commencer par les skills IA.  
> ✅ Toujours commencer par la source near-code.

### Résumé visuel

```
1. near-code (convention source)
        ↓
2. .claude/skills/ (résumé IA)
        ↓
3. tchalanet-docs/docs/ (si impact métier ou archi)
```

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
**Dernière mise à jour** : 2026-04-23
