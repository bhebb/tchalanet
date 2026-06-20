# Docker Scripts

## Publish API image

```bash
./scripts/docker/publish-images.sh <org> <tag> ghcr.io
```

Le script :

- build l'API;
- push l'image;
- met à jour `IMAGE_TAG` et `API_IMAGE_BASE`.

L'authentification Firebase ne nécessite pas d'image Docker infra.
