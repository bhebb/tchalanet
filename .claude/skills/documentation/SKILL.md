---
name: documentation
description: >
  Use when deciding where to write or find documentation in tchalanet — maps each documentation type to its authoritative location (DOMAIN_*.md, FEATURE_*.md, tchalanet-docs, openspec) and enforces the single-source-of-truth rule.
---

# Documentation — Sources de vérité

> Règle fondamentale : **une seule source de vérité par type d'information**.

## Hiérarchie des sources

```
1. Near-code documentation    ← DOMAIN_*.md, FEATURE_*.md, code lui-même
2. Central documentation      ← tchalanet-docs/
3. OpenSpec context packs     ← openspec/context/
4. OpenSpec feature specs     ← openspec/specs/
```

En cas de conflit : la near-code documentation l'emporte.

---

## Index par type d'information

| Type                                               | Emplacement canonique                            |
| -------------------------------------------------- | ------------------------------------------------ |
| Invariants et règles métier d'un domaine           | `tchalanet-server/src/**/<domain>/DOMAIN_*.md`   |
| Documentation d'une feature backend                | `tchalanet-server/src/**/<feature>/FEATURE_*.md` |
| Conventions backend (nommage, tests, persistence…) | `tchalanet-server/docs/conventions/`             |
| Architecture backend                               | `tchalanet-server/docs/ARCHITECTURE.md`          |
| Nommage backend                                    | `tchalanet-server/docs/NAMING.md`                |
| Règles non-négociables (archi, RLS, multi-tenant)  | `openspec/context/10-non-negotiables.md`         |
| Règles backend Java/Spring                         | `openspec/context/20-backend-rules.md`           |
| Règles frontend Angular                            | `openspec/context/30-frontend-rules.md`          |
| Règles mobile                                      | `openspec/context/40-mobile-rules.md`            |
| Règles edge service                                | `openspec/context/50-edge-service-rules.md`      |
| Règles infra                                       | `openspec/context/60-infra-rules.md`             |
| Règles catalog                                     | `openspec/context/75-catalog-rules.md`           |
| Règles core                                        | `openspec/context/80-core-rules.md`              |
| Règles features                                    | `openspec/context/81-feature-rules.md`           |
| Versions de toutes les dépendances                 | `VERSIONS.md`                                    |
| Hub documentation général                          | `DOCUMENTATION.md`                               |
| Specs capabilities actuelles                       | `openspec/specs/<capability>/spec.md`            |
| Proposals / changes en cours                       | `openspec/changes/<change-id>/`                  |
| ADRs                                               | `tchalanet-docs/docs/03-adr/`                    |
| Documentation opérationnelle infra                 | `tchalanet-infra/docs/`                          |
| Démarrage rapide                                   | `QUICK-START.md`                                 |
| Edge service                                       | `tchalanet-edge-service/README.md`               |

---

## Règles d'écriture

- **Règles stables** → `tchalanet-docs/docs/`
- **Détails techniques proches du code** → docs near-code (`DOMAIN_*.md`, `FEATURE_*.md`)
- **Specs et proposals en cours** → `openspec/changes/` et `openspec/specs/`
- **Contexte et règles transversales** → `openspec/context/`

---

## Ce que la near-code documentation DOIT couvrir

### `DOMAIN_*.md`

- Sens métier du domaine
- Invariants et règles métier
- Exemples concrets
- Sémantique des données
- Lifecycle (si applicable)

### `FEATURE_*.md`

- Objectif UI de la feature
- Sub-slices si feature umbrella
- Domaines core touchés
- Flows d'orchestration

---

## Ce que la near-code documentation NE DOIT PAS couvrir

- `DOMAIN_*.md` ne contient pas de règles de structure/layering (→ openspec context)
- `FEATURE_*.md` ne décrit pas les invariants métier (→ core domain)

---

## Synchronisation avec tchalanet-docs

La documentation centrale est synchronisée via :

```bash
# Sync des near-code docs vers tchalanet-docs
scripts/sync-ref-docs.sh
```

Les copies dans `tchalanet-docs/docs/99-links/_ref/` sont auto-générées — ne pas éditer directement.
