# Guide Documentation Tchalanet

> **Ce fichier est un index de navigation.**
> La **politique documentaire complète** (règles de placement, arborescence cible, workflow, règles de référencement croisé)
> vit dans la source canonique :
> 👉 `tchalanet-docs/docs/00-guidelines/doc-policy.md`

**Version** : 2.0.0 | **Date** : 2026-04-23

---

## 🎯 Principes (rappel)

1. Une seule source de vérité par type d'info
2. Doc proche du code quand c'est technique
3. SDD rapide via OpenSpec, pas dans la doc centrale
4. Versions contrôlées dans `VERSIONS.md` uniquement

---

## 📚 Les 3 espaces documentation

| Espace                | Emplacement                | Contient                                       | Public                        |
| --------------------- | -------------------------- | ---------------------------------------------- | ----------------------------- |
| **Centrale** (MkDocs) | `tchalanet-docs/docs/`     | Constitution, archi maps, domaines métier, ADR | dev · business · IA · externe |
| **Near-code**         | `**/docs/`, `**/README.md` | Détails techniques, how-to, exemples           | dev du module                 |
| **SDD / OpenSpec**    | `openspec/`                | Specs features en cours, context packs IA      | dev feature · IA              |

---

## 🗺️ Où mettre quoi ?

| Question                  | Réponse                                  |
| ------------------------- | ---------------------------------------- |
| Règle stable partagée ?   | `tchalanet-docs/docs/00-guidelines/`     |
| Architecture / carte ?    | `tchalanet-docs/docs/01-architecture/`   |
| Métier / domaine ?        | `tchalanet-docs/docs/02-functional/`     |
| Décision structurante ?   | `tchalanet-docs/docs/03-adr/`            |
| Détail technique module ? | Doc proche du code                       |
| Feature en cours ?        | `openspec/specs/` ou `openspec/changes/` |

---

## 📖 Navigation rapide

| Sujet                           | Lien                                                |
| ------------------------------- | --------------------------------------------------- |
| Règles globales IA              | `AGENTS.md`                                         |
| Versions                        | `VERSIONS.md`                                       |
| Politique documentaire complète | `tchalanet-docs/docs/00-guidelines/doc-policy.md`   |
| Constitution                    | `tchalanet-docs/docs/00-guidelines/constitution.md` |
| Architecture maps               | `tchalanet-docs/docs/01-architecture/`              |
| Domaines métier                 | `tchalanet-docs/docs/02-functional/domains/`        |
| Workflows métier                | `tchalanet-docs/docs/02-functional/flows/`          |
| ADR                             | `tchalanet-docs/docs/03-adr/`                       |
| Backend archi (canonique)       | `tchalanet-server/docs/ARCHITECTURE.md`             |
| Backend conventions             | `tchalanet-server/docs/conventions/`                |
| Web docs                        | `apps/tchalanet-web/README.md`                      |
| Infra docs                      | `tchalanet-infra/docs/`                             |
| OpenSpec project                | `openspec/project.md`                               |
| Specs en cours                  | `openspec/specs/`                                   |
| Skills IA                       | `.claude/skills/`                                   |
