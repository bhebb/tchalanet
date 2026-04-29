## Why

`LimitPolicyRuntimeService.evaluate()` passe `Collections.emptyList()` comme liste des cibles d'évaluation — l'évaluation des limites ne prend donc jamais en compte les assignments spécifiques et retourne systématiquement des résultats factices. Ce placeholder non finalisé a été identifié lors de l'audit du 2026-04-27 comme anomalie HAUTE.

## What Changes

- Implémentation réelle de `LimitPolicyRuntimeService.evaluate()` : charger les `LimitAssignment` correspondant aux cibles du contexte et les passer au moteur d'évaluation.
- Suppression du `Collections.emptyList()` placeholder.
- Couverture de test unitaire et d'intégration sur le service runtime.

## Capabilities

### New Capabilities

<!-- aucune nouvelle capability fonctionnelle, finalisation d'une implémentation placeholder -->

### Modified Capabilities

<!-- aucun changement de contrat d'interface, implémentation interne -->

## Impact

- `core.limitpolicy/application/service/LimitPolicyRuntimeService` — méthode `evaluate()` à implémenter réellement.
- `core.limitpolicy/application/port/out/` — identifier le port de lecture des assignments (`LimitAssignmentReaderPort` ou équivalent) à injecter dans le service.
- Tests : `LimitPolicyRuntimeServiceTest` + test d'intégration end-to-end du moteur d'évaluation.
- **Aucun breaking change sur l'interface publique** — le contrat reste inchangé.
