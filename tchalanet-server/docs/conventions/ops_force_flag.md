# Convention : Ops Force Flag

> **Scope** : Operations / Backend  
> **Status** : MANDATORY

## 1. Principes généraux

Le flag `force=true` présent sur certaines commandes d'administration (OPS) permet de bypasser des gardes-fous applicatifs (cooldowns, validations de statut souples, unicité applicative). Son usage doit être strictement encadré pour éviter des dérives opérationnelles.

## 2. Règles MUST

1. **Raison obligatoire** : Toute commande exécutée avec `force=true` MUST inclure une `reason` non vide expliquant pourquoi le bypass est nécessaire.
2. **Audit systématique** : L'usage du flag `force=true` est automatiquement audité via l'aspect `@AuditedForceCommand` et persiste en base dans la table `audit_log`.
3. **Périmètre restreint** : Ces commandes ne doivent être exposées que sur des endpoints `/platform/ops/**` ou `/admin/**` sécurisés par des rôles de type `SUPER_ADMIN` ou `OPERATIONS`.

## 3. Ce que le flag `force` NE bypass PAS

Le flag `force` ne doit **JAMAIS** permettre de contourner les contrôles suivants :

- **Auth & Permissions** : L'utilisateur doit toujours avoir la permission requise.
- **RLS (Row Level Security)** : L'isolation des données par tenant reste absolue.
- **Règles de finalité financière** : Un override de résultat est interdit si un tirage lié est déjà `SETTLED` (payouts émis).
- **Contraintes SQL strictes** : Les contraintes de clés primaires et d'intégrité référentielle en base.

## 4. Règles de bypass autorisées

Le flag `force` peut bypasser :

- **Cooldowns temporels** : Par exemple, autoriser un nouveau fetch avant la fin du délai d'attente habituel.
- **Statuts transitoires** : Autoriser le re-apply d'un résultat sur un tirage déjà `RESULTED` (mais non `SETTLED`).
- **Validations de doublons applicatifs** : Forcer la génération de tirages déjà présents en base (écrasement).

## 5. Checklist PR

Lors de l'ajout du flag `force` sur une nouvelle commande :

- [ ] La commande est annotée avec `@AuditedForceCommand`.
- [ ] Le record de la commande inclut un champ `reason`.
- [ ] Une méthode `@AssertTrue public boolean isReasonValidForForce()` valide la présence de la raison si force est vrai.
- [ ] Le controller reporte bien les erreurs de validation (400).
- [ ] Le code métier vérifie explicitement les "règles non bypassables" avant de prendre en compte le flag `force`.
