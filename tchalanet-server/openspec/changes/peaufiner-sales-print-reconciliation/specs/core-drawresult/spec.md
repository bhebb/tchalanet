# Spec — core.drawresult read support for reconciliation

## Requirement: Expose resulted draws for reconciliation

`core.drawresult` MUST expose a read query that returns resulted draws/draw results for a tenant business date.

### Query: ListReconciliationDrawResultsQuery

Input:

```java
public record ListReconciliationDrawResultsQuery(
    TenantId tenantId,
    LocalDate businessDate
) implements Query<List<ReconciliationDrawResultRow>> {}
```

Output:

```java
public record ReconciliationDrawResultRow(
    DrawId drawId,
    DrawResultId drawResultId,
    DrawChannelId drawChannelId,
    LocalDate drawDate,
    Instant scheduledAt,
    Instant resultedAt,
    DrawStatus drawStatus,
    DrawResultStatus resultStatus
) {}
```

### Scenario: business date lookup

Given a tenant business date
When reconciliation asks for draw results
Then the query returns only draws/results eligible for reconciliation
And uses tenant/date semantics consistently with draw generation/result application.

## Requirement: Do not expose persistence entities

The reconciliation query MUST return immutable rows/read models only.
