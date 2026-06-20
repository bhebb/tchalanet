# Build Local vs Published

## Local build

`compose/docker-compose.local-build.yml` construit les images applicatives
locales, principalement l'API.

```bash
make rebuild-api ENV=dev
make rebuild-edge ENV=dev
make rebuild-all ENV=dev
```

## Published images

Les images publiées sont référencées par :

```bash
IMAGE_TAG=<tag>
API_IMAGE_BASE=<registry>/<org>/tchalanet-api
```

L'authentification Firebase ne nécessite aucune image infra locale.
