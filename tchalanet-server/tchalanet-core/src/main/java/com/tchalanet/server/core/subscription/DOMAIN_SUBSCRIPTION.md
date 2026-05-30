# Domain — Subscription

## Responsabilité

`core.subscription` gère le cycle de vie des abonnements tenant : plan appliqué, renouvellements, suspensions, annulations, reprises.

Il répond à :
- Quel plan un tenant a-t-il actif ?
- Quand expire-t-il ?
- Quelle est la fréquence de facturation ?

---

## États

```
TRIAL → ACTIVE → SUSPENDED → CANCELED
                           → EXPIRED
```

| Statut | Signification |
|---|---|
| `TRIAL` | Période d'essai active |
| `ACTIVE` | Abonnement valide et payant |
| `SUSPENDED` | Temporairement suspendu (non payé, décision admin) |
| `CANCELED` | Résilié explicitement |
| `EXPIRED` | Date de fin dépassée sans renouvellement |

---

## Fréquences de facturation

| Valeur | Usage |
|---|---|
| `MONTHLY` | Renouvellement mensuel |
| `YEARLY` | Renouvellement annuel |

---

## Commandes

| Commande | Rôle |
|---|---|
| `ApplyTenantPlanCommand` | Appliquer un plan à un tenant (onboarding, changement de plan) |
| `ChangePlanCommand` | Changer le plan actif |
| `RenewSubscriptionCommand` | Renouveler l'abonnement courant |
| `SuspendSubscriptionCommand` | Suspendre un abonnement actif |
| `ResumeSubscriptionCommand` | Reprendre un abonnement suspendu |
| `CancelSubscriptionCommand` | Résilier définitivement |

---

## Queries

| Query | Vue produite |
|---|---|
| `ResolveTenantSubscriptionQuery` | Résoudre l'abonnement actif d'un tenant |
| `GetPlatformSubscriptionStatsQuery` | Stats globales plateforme (SUPER_ADMIN) |

---

## Invariants

- Un tenant ne peut avoir qu'un seul abonnement `ACTIVE` ou `TRIAL` à la fois.
- Un abonnement `CANCELED` ou `EXPIRED` ne peut pas être repris — il faut en créer un nouveau.
- Le changement de plan (`ChangePlanCommand`) crée une transition, pas un nouvel abonnement.

---

## Règles

- Ne pas appeler `core.subscription` directement depuis `core.sales` — si une gate est nécessaire, passer par `platform.entitlement.api`.
- La suspension/annulation est une décision platform/admin, pas un effet automatique d'un événement de paiement.
- Voir conventions : `docs/conventions/command_query_handlers.md` · `docs/conventions/event_model.md`
