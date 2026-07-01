# Mobile Distribution V0

> Voir le runbook complet : [RB-03 — Distribution mobile Android](runbooks/RB-03-mobile-distribution.md)

## Endpoints staging

```text
https://api.stg.tchalanet.com    -> API Spring Boot
https://edge.stg.tchalanet.com   -> Edge service
```

## Auth

Le mobile utilise Firebase Auth côté client (projet `tchalanet-39115`). L'API Tchalanet reste responsable
des rôles, permissions, contexte opérationnel et règles métier.

## Configuration build

L'app Flutter reçoit sa config via `--dart-define` au build :
- `API_BASE_URL` — URL absolue de l'API (staging ou prod)
- `TERMINAL_EMAIL_DOMAIN` — domaine email des terminaux vendeurs
- `POS_DEVICE_BINDING` — credential de binding terminal (vide en prod)

## Distribution

Distribution interne via Firebase App Distribution. Builds manuels uniquement (pas d'auto-build sur PR/push).

Pour recréer le serveur staging :

```bash
make staging-destroy
make staging-create
```
