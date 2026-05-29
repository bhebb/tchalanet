# Spec — platform.communication support for reconciliation notifications

## Requirement: Send reconciliation anomaly email with CSV attachment

`core.reconciliation` may delegate email delivery to `platform.communication.api` after commit.

### Scenario: anomalies detected

Given a reconciliation run completes with critical or high anomalies
When the transaction commits
Then an email notification is sent to configured recipients
And a CSV attachment is included
And the email states that no automatic correction was applied.

## Email template

Subject:

```text
[Tchalanet] Anomalies de reconciliation détectées — <tenantName> — <businessDate>
```

Body summary:

```text
Bonjour,

Le batch de reconciliation quotidien a détecté des anomalies pour le tenant <tenantName>.

Date métier : <businessDate>
Run ID : <reconciliationRunId>
Statut du run : COMPLETED_WITH_ANOMALIES

Résumé :
- Draws vérifiés : <checkedDrawCount>
- Tickets vérifiés : <checkedTicketCount>
- Anomalies totales : <anomalyCount>
- Anomalies critiques : <criticalCount>
- Anomalies high : <highCount>
- Anomalies medium : <mediumCount>

Les détails sont disponibles dans le fichier CSV joint.

Aucune correction automatique n’a été appliquée.
Les anomalies doivent être revues par un administrateur autorisé avant toute action de réparation.

Cordialement,
Tchalanet
```

## CSV filename

```text
reconciliation-anomalies_<tenantCode>_<businessDate>_<runId>.csv
```

## CSV format

- UTF-8
- comma separator
- double quote escaping
- ISO-8601 UTC timestamps
- decimal plain strings for amounts
