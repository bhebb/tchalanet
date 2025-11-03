# Scripts Keycloak

Scripts pour gérer Keycloak : realms, import/export, providers.

## 📜 Scripts disponibles

### `get-realm.sh`

**Objectif :** Générer le fichier realm JSON depuis le template

**Usage :**

```bash
./scripts/keycloak/get-realm.sh <env>
```

**Actions :**

- Lit `keycloak/realms/realm.base.json`
- Injecte variables d'environnement
- Génère `keycloak/realms/realm-<env>.json`

**Variables utilisées :**

- `APP_WEB_ORIGIN`, `APP_MOBILE_ORIGIN`
- `EXTRA_WEB_REDIRECTS`
- Etc.

## 📂 Structure Keycloak

```
keycloak/
├── realms/
│   ├── realm.base.json        # Template
│   └── realm-staging.json     # Généré
├── themes/
│   └── tchalanet/             # Theme personnalisé
└── providers/
    └── tchalanet-*.jar        # Provider custom
```
