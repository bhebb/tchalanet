# DOMAIN_SALES_OFFLINE

`core.sales` est la vérité métier des tickets acceptés.

## Règle centrale

Sales crée un `Ticket` seulement si tous les gates métier passent.

## Money model

```text
stakeAmount = montant joué
feeAmount   = frais SMS/service/taxe
totalAmount = stakeAmount + feeAmount
```

Payout et potential payout se basent sur `stakeAmount`. La session cash reçoit `totalAmount`.

## Gates métier offline

- draw autorisé par grant
- cutoff respecté
- pas de résultat déjà connu selon policy
- pricing/odds cohérents
- money breakdown valide
- limitpolicy non bloquante
- session acceptable
- code/clientTicket/localSequence non déjà accepté

## Pas de Ticket pour

- TECHNICALLY_REJECTED
- SALES_REJECTED
- SALES_CONFLICT
- SALES_REVIEW_REQUIRED v0

## Event

`TicketPlacedEvent` est publié after-commit uniquement pour un ticket accepté.
