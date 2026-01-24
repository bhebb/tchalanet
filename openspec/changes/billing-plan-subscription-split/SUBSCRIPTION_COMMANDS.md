# Subscription Services — Commands

## Services à implémenter (core/subscription)

Ces services doivent être implémentés comme des **CommandHandlers** dans `core/subscription/application/command/handler/`.

---

## 1. ApplyTenantPlanCommand (déjà spécifié)

**Command** : `ApplyTenantPlanCommand(tenantId, planCode, effectiveAt?, idempotencyKey?)`

**Handler** : `ApplyTenantPlanCommandHandler`

**Responsabilité** :

- Appliquer un plan à un tenant (nouveau ou changement)
- Valider via `PlanCatalog.findByCode()`
- Créer/mettre à jour `tenant_subscription`
- Publier `TenantSubscriptionUpdatedEvent` after-commit

---

## 2. ChangePlanCommand

**Command** : `ChangePlanCommand(tenantId, newPlanCode, effectiveAt?, idempotencyKey?)`

**Handler** : `ChangePlanCommandHandler`

**Responsabilité** :

- Changer le plan d'un tenant existant
- Valider que le nouveau plan existe et est actif
- Valider que la transition est permise (ex: pas de downgrade en trial)
- Mettre à jour `plan_code` + `version++`
- Publier `TenantSubscriptionUpdatedEvent(action=PLAN_CHANGED)` after-commit

**Différence avec Apply** :

- `Apply` : peut créer une nouvelle subscription
- `ChangePlan` : nécessite subscription existante, focus sur le changement de plan

---

## 3. CancelSubscriptionCommand

**Command** : `CancelSubscriptionCommand(tenantId, reason?, canceledAt?, idempotencyKey?)`

**Handler** : `CancelSubscriptionCommandHandler`

**Responsabilité** :

- Annuler la subscription d'un tenant
- Vérifier que status actuel permet la cancellation (ex: pas déjà canceled)
- Mettre à jour `status = CANCELED`, `canceled_at = now()`
- Publier `TenantSubscriptionCanceledEvent` after-commit

**Idempotence** :

- Si déjà canceled → retour OK (idempotent)
- Si expired → erreur (impossible de cancel un expired)

---

## 4. RenewSubscriptionCommand

**Command** : `RenewSubscriptionCommand(tenantId, newEndsAt?, idempotencyKey?)`

**Handler** : `RenewSubscriptionCommandHandler`

**Responsabilité** :

- Renouveler une subscription (prolonger `ends_at`)
- Vérifier que status actuel permet le renouvellement (ACTIVE ou TRIAL)
- Mettre à jour `ends_at`, `version++`
- Publier `TenantSubscriptionRenewedEvent` after-commit

**Use case** :

- Renouvellement automatique (batch job)
- Renouvellement manuel par admin
- Prolongation de trial

---

## 5. ResumeSubscriptionCommand

**Command** : `ResumeSubscriptionCommand(tenantId, idempotencyKey?)`

**Handler** : `ResumeSubscriptionCommandHandler`

**Responsabilité** :

- Reprendre une subscription suspendue
- Vérifier que status = SUSPENDED
- Mettre à jour `status = ACTIVE`, `version++`
- Publier `TenantSubscriptionResumedEvent` after-commit

**Différence avec Renew** :

- `Resume` : réactivation après suspension (changement de status)
- `Renew` : prolongation de durée (changement de `ends_at`)

---

## 6. SuspendSubscriptionCommand (bonus, si besoin)

**Command** : `SuspendSubscriptionCommand(tenantId, reason?, idempotencyKey?)`

**Handler** : `SuspendSubscriptionCommandHandler`

**Responsabilité** :

- Suspendre une subscription active
- Mettre à jour `status = SUSPENDED`
- Publier `TenantSubscriptionSuspendedEvent` after-commit

**Use case** :

- Non-paiement
- Violation de ToS
- Suspension temporaire par admin

---

## Architecture Commands (pattern)

Tous les handlers doivent suivre le pattern :

```java
@UseCase
@RequiredArgsConstructor
public class XxxCommandHandler implements CommandHandler<XxxCommand, XxxResult> {

  private final SubscriptionPersistencePort persistencePort;
  private final SubscriptionReaderPort readerPort;
  private final PlanCatalog planCatalog; // uniquement si validation plan nécessaire
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public XxxResult handle(XxxCommand cmd) {
    // 1. Validation métier
    // 2. Lecture état actuel
    // 3. Transformation état
    // 4. Persistence (version++)
    // 5. Event after-commit
    AfterCommit.run(() -> eventPublisher.publishEvent(new XxxEvent(...)));
    return result;
  }
}
```

---

## Events à créer

- `TenantSubscriptionUpdatedEvent` (générique, avec action)
- `TenantSubscriptionCanceledEvent`
- `TenantSubscriptionRenewedEvent`
- `TenantSubscriptionResumedEvent`
- `TenantSubscriptionSuspendedEvent`

Tous doivent inclure :

- `tenantId`
- `planCode`
- `status`
- `version`
- `timestamp`
- `initiator`

---

## Conformité

- ✅ `@UseCase` annotation
- ✅ `@TchTx` pour transactions
- ✅ `AfterCommit.run()` pour events
- ✅ Typed IDs (`TenantId`, jamais `UUID`)
- ✅ Validation via `PlanCatalog` API publique uniquement
- ✅ Idempotence via `@Version` optimiste
- ✅ Aucune dépendance vers `catalog/plan/internal`

---

## Ordre d'implémentation suggéré

1. **ApplyTenantPlanCommand** (core, base)
2. **CancelSubscriptionCommand** (simple, pas de dépendance plan)
3. **ResumeSubscriptionCommand** (simple, transition status)
4. **SuspendSubscriptionCommand** (simple, transition status)
5. **RenewSubscriptionCommand** (prolongation durée)
6. **ChangePlanCommand** (complexe, validation plan + transition)
