# Keycloak Realms Structure

## 📁 Structure des fichiers

```
keycloak/realms/
├── templates/
│   └── realm.base.json          # Template de base (NE PAS importer)
├── overlays/
│   ├── dev.json                 # Overlay spécifique dev
│   ├── staging.json             # Overlay spécifique staging
│   └── prod.json                # Overlay spécifique prod
├── tchalanet-realm.json         # Realm généré pour dev (importé par Keycloak)
└── README.md                    # Ce fichier
```

## 🔧 Génération du realm

Le script `scripts/keycloak/get-realm.sh` génère un fichier realm à partir du template :

```bash
# Générer le realm pour dev
make get-realm ENV=dev

# Générer le realm pour staging
make get-realm ENV=staging

# Générer le realm pour prod
make get-realm ENV=prod
```

## 📝 Workflow

1. **Template** : `templates/realm.base.json` contient la structure de base du realm, sans utilisateurs de démo
2. **Script** : `get-realm.sh` lit le template et applique les variables d'environnement
3. **Overlay** : Si un fichier `overlays/<ENV>.json` existe, il est fusionné avec le realm généré
4. **Output** : Le fichier `<realm-name>-realm.json` est généré dans `realms/`
5. **Import** : Keycloak importe uniquement le fichier généré (pas le template ni les overlays)

## ⚠️ Important

- **Ne PAS déplacer** `realm.base.json` dans le dossier racine `realms/`
- **Ne PAS monter** le dossier `templates/` ou `overlays/` dans le conteneur Keycloak
- **Seul le fichier généré** `<realm-name>-realm.json` doit être monté dans `/opt/keycloak/data/import`
- Les utilisateurs locaux (`super_admin`, `admin`, `operator`, `cashier*`) vivent uniquement dans `overlays/dev.json`
- Les overlays staging/prod ne doivent jamais contenir d'utilisateurs de démo

## 🔑 Clients et contrat token

Le realm de base définit les clients publics `tchalanet-web`, `tchalanet-mobile-pos` et `tchalanet-swagger` en Authorization Code + PKCE S256. `tchalanet-api` reste un resource server sans login utilisateur.

Les tokens exposent les claims stables attendus par les applications :

- identité OIDC : `sub`, `preferred_username`, `email`, `given_name`, `family_name`, `name`, `locale`
- autorisation large : `roles`, `groups`
- contexte Tchalanet stable : claim JSON `tch`

Les permissions fines, terminal/outlet/session courants, limites de vente et droits offline restent évalués côté Tchalanet runtime, pas comme vérité de token.

## 🔐 Variables d'environnement

Le script utilise les variables suivantes :

- `KC_REALM` : Nom du realm (défaut: `tchalanet`)
- `KC_HOSTNAME` / `KC_HOSTNAME_URL` : issuer externe de Keycloak
- `DEFAULT_LOCALE` : Locale par défaut (défaut: `fr`)
- `SUPPORTED_LOCALES` : Locales supportées (CSV)
- `APP_WEB_ORIGIN` : Origin de l'app web
- `TEST_USER_PASSWORD` : Mot de passe des utilisateurs de test

Ces variables sont définies dans `envs/<ENV>/*.env`.

En dev, le défaut reste `https://auth.localtest.me` pour le travail local sur Mac. Pour lancer la stack sur une autre machine du réseau et coder depuis le Mac, overridez `KC_HOSTNAME=auth.tchalanet.lan` et `KC_HOSTNAME_URL=http://auth.tchalanet.lan`, avec le DNS/hosts du Mac pointant `*.tchalanet.lan` vers l'IP de cette machine.

## 📖 Exemples

### Générer un realm custom

```bash
TEMPLATE=/path/to/custom-template.json \
OUT_FILE=/path/to/output.json \
./scripts/keycloak/get-realm.sh dev
```

### Ajouter un overlay

Créez `overlays/dev.json` avec des modifications spécifiques :

```json
{
  "smtpServer": {
    "host": "smtp.dev.example.com"
  }
}
```

Le script fusionnera automatiquement l'overlay avec le realm généré.
