# Vite Allowed Hosts

Hôtes locaux utiles :

```text
https://app.localtest.me
https://api.localtest.me
http://localhost:4200
```

Firebase Auth est externe ou émulé localement sur `localhost:9099`; aucun hôte
Traefik d'authentification n'est requis.

Exemple CORS API :

```bash
APP_CORS_ALLOWED_ORIGINS=http://localhost:*,https://app.localtest.me
```
