# tchalanet-mobile — OpenSpec Project Context

## Périmètre

Ce OpenSpec couvre **uniquement** l'application Flutter / POS.

Périmètres inclus :

- Flutter 3 + Riverpod + GoRouter + Material 3
- Flow vendeur (sell ticket, résumé, historique)
- POS : impression, scan, terminal Capacitor
- Auth mobile (Keycloak OIDC, token refresh)
- Offline foundation
- Intégration API mobile

## Ne pas inclure ici

- Changes backend Java → `tchalanet-server/openspec/`
- Changes Angular → `tchalanet-web/openspec/`
- Changes edge / notifications → `tchalanet-edge-service/openspec/`
- Coordination cross-projet → `openspec/` (racine)

## Conventions d'archivage

```bash
cd tchalanet-mobile
openspec archive <change-id> --yes
openspec validate --strict
```
