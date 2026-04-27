# 03 - ADR (Architecture Decision Records)

**Décisions d'architecture importantes**

Les ADR documentent les choix techniques majeurs avec leur contexte, alternatives évaluées et conséquences.

# ADR — Architecture Decision Records

Les ADRs capturent les **décisions importantes** :

- contexte
- décision
- conséquences
- alternatives considérées

Règles :

- numérotés (ADR-0001, ADR-0002…)
- immuables : si on change d’avis → nouvelle ADR
- pas de procédures “how-to” ici (ça va dans near-code docs)

Index :

- ADR-0001 Stack
- ADR-0002 Slot-first results

---

## 📋 ADR existants

### [ADR-0001 - Stack technique](ADR-0001-stack.md)

Choix Spring Boot 4 + Java 25 + Angular 20 + Ionic + PostgreSQL + Keycloak

### [ADR-0002 - Results Slot-first](ADR-0002-slot-first-results.md)

Architecture résultats tirages (slot-first vs draw-centric)

---

## 🎯 Format ADR

Chaque ADR suit le template :

```markdown
# ADR-XXXX - Titre court

**Status**: Accepted / Proposed / Deprecated / Superseded  
**Date**: YYYY-MM-DD  
**Deciders**: équipe backend / équipe frontend / architecture team  
**Tags**: backend, frontend, infra, security, performance

## Context

Quel problème résolvons-nous ? Contraintes ? Contexte métier/technique ?

## Decision

Quelle solution avons-nous choisie ?

## Alternatives considered

Quelles autres solutions avons-nous évaluées ? Pourquoi rejetées ?

## Consequences

Impacts positifs / négatifs ? Trade-offs ? Risques ?

## References

Liens vers specs, docs externes, benchmarks
```

---

## 🚀 Créer un nouvel ADR

```bash
# Créer fichier
cd tchalanet-docs/docs/03-adr
cp ADR-TEMPLATE.md ADR-XXXX-titre-court.md

# Éditer
# - Remplir contexte, décision, alternatives, conséquences
# - Status: Proposed (draft) → Accepted (validé)

# Ajouter dans mkdocs.yml navigation
# Commiter
```

---

## 📌 Quand créer un ADR ?

Créer un ADR quand :

- ✅ Choix architectural impactant plusieurs modules
- ✅ Choix de stack / framework / lib majeure
- ✅ Changement de pattern (ex: passage CQRS)
- ✅ Décision sécurité / conformité importante
- ✅ Trade-off performance vs maintenabilité

Ne **pas** créer ADR pour :

- ❌ Détails d'implémentation locaux à un module
- ❌ Refactors mineurs
- ❌ Corrections bugs

---

**Dernière mise à jour** : 2026-01-17
