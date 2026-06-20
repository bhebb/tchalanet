# Mobile Distribution V0

## Endpoints staging

```text
https://api.stg.tchalanet.com    -> API Spring Boot
https://edge.stg.tchalanet.com   -> Edge service
```

## Auth

Le mobile utilise Firebase Auth côté client. L'API Tchalanet reste responsable
des rôles, permissions, contexte opérationnel et règles métier.

## Distribution

Tant que l'app est en test interne, un staging jetable peut être recréé avec :

```bash
make staging-destroy
make staging-create
```
