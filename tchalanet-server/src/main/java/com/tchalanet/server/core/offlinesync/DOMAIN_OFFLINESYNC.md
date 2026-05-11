# DOMAIN_OFFLINESYNC

`core.offlinesync` est le domaine de fiabilité de synchronisation offline.

## Possède

- OfflineSalesGrant
- OfflineCodeBatch
- OfflineCodeReservation
- OfflineBatch
- OfflineSaleSubmission
- OfflineSyncAttempt
- Technical gates et risk flags

## Ne possède pas

- Ticket officiel
- payout
- stats Sales
- ledger
- décision finale de vente

## Invariants

1. Une soumission offline n'est jamais un Ticket.
2. Une soumission techniquement rejetée n'appelle pas Sales.
3. Une soumission rejetée/review par Sales reste dans offlinesync avec `sales_ticket_id = null`.
4. Un code offline est réservé côté serveur et consommable une seule fois.
5. Le payload brut est conservé pour audit.

## Lifecycle

```text
RECEIVED -> TECHNICALLY_REJECTED
RECEIVED -> READY_FOR_SALES -> SENT_TO_SALES -> SALES_ACCEPTED
                                      |-> SALES_REJECTED
                                      |-> SALES_CONFLICT
                                      |-> SALES_REVIEW_REQUIRED
```
