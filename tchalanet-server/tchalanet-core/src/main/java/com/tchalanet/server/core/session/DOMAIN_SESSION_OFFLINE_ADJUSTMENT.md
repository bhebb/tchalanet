# DOMAIN_SESSION_OFFLINE_ADJUSTMENT

Objectif : gérer les tickets offline acceptés après fermeture de session sans modifier silencieusement la caisse fermée.

## Règle

```text
Session ouverte à la sync -> posting normal
Session fermée mais policy OK -> SalesSessionOfflineAdjustment
Session finalisée/comptabilisée -> review required
```

## Reporting

```text
expected cash at close
+ accepted offline after close
= adjusted expected cash
```
