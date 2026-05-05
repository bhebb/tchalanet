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

---

## 📋 ADR existants

No published ADR pages are currently present beyond this index.

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
cd tchalanet-docs/docs/03-adr
$EDITOR ADR-XXXX-titre-court.md
```

Remplir contexte, décision, alternatives, conséquences, puis ajouter l'ADR à
`tchalanet-docs/mkdocs.yml` quand elle est prête à publier.

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
