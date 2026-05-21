# Scripts E2E — bootstrap, vente cashier et PDF

## Objectif

Les scripts E2E couvrent maintenant le flow minimal nécessaire pour démarrer un client mobile vendeur:

- authentification JWT côté app/mobile ;
- récupération des draws vendables ;
- vente cashier avec contexte POS ;
- génération de PDF de reçu pour les tickets vendus.

### Phase 1 — `scripts/e2e-phase1-bootstrap.sh`

Le script automatise le bootstrap local avant une vente cashier:

1. ouvrir/forcer l’ouverture des tirages du jour via `POST /platform/ops/draws/open-today` ;
2. vérifier la session POS courante via `GET /tenant/sessions/current?terminalId=...` ;
3. ouvrir une session si nécessaire via `POST /tenant/sessions/open` ;
4. lister les tirages du jour via `GET /admin/draws/today` ;
5. afficher les `export` à réutiliser pour la phase 2.

### Phase 2 — `scripts/e2e-phase2-sell-pdf.sh`

Le script automatise la vente d’au moins un ticket par draw vendable et produit les PDF:

1. bootstrap automatique via la phase 1 si `TCH_SESSION_ID` est absent ;
2. liste les draws vendables via `GET /tenant/cashier/draws/sellable` ;
3. vend un ticket par draw via `POST /tenant/cashier/sell` avec plusieurs sélections dans `lines` ;
4. décode le PDF embarqué dans la réponse cashier (`data.receipt.base64`) ;
5. télécharge aussi `GET /tenant/tickets/{ticketId}/print.pdf` pour valider le rendu binaire ;
6. affiche un lien local `file://` vers le PDF artefact et l’URL API `print.pdf` ;
7. stocke les artefacts PDF/JSON sur disque.

## IDs seed utilisés par défaut

| Variable | Valeur | Source |
| --- | --- | --- |
| `TCH_TENANT_ID` | `00000000-0000-0000-0000-000000000003` | `V205__seed_outlet_terminal_pos.sql` |
| `TCH_OUTLET_ID` | `00000000-0000-0000-0000-000000003001` | `V205__seed_outlet_terminal_pos.sql` |
| `TCH_TERMINAL_ID` | `00000000-0000-0000-0000-000000003101` | `V205__seed_outlet_terminal_pos.sql` |

## Variables attendues

| Variable | Obligatoire | Défaut | Rôle |
| --- | --- | --- | --- |
| `TCH_BASE_URL` | non | `http://localhost:8083/api/v1` | base API locale |
| `TCH_SUPER_ADMIN_TOKEN` | oui* | — | token pour `open-today` et fallback régénération |
| `TCH_SELLER_TOKEN` | oui* | — | token tenant/cashier pour session + draws |
| `TCH_SUPER_ADMIN_USERNAME` | non | — | login super-admin pour auto-récupération token |
| `TCH_SUPER_ADMIN_PASSWORD` | non | — | mot de passe super-admin pour auto-récupération token |
| `TCH_SELLER_USERNAME` | non | — | login vendeur pour auto-récupération token |
| `TCH_SELLER_PASSWORD` | non | — | mot de passe vendeur pour auto-récupération token |
| `TCH_AUTH_ISSUER_URI` | non | `https://auth.localtest.me/realms/tchalanet` | issuer OIDC utilisé pour déduire le token endpoint |
| `TCH_AUTH_TOKEN_URL` | non | `${TCH_AUTH_ISSUER_URI}/protocol/openid-connect/token` | endpoint token OAuth2 (prioritaire sur issuer) |
| `TCH_AUTH_CLIENT_ID` | non | `tchalanet-swagger` | client OAuth2 utilisé pour récupérer les tokens |
| `TCH_AUTH_CLIENT_SECRET` | non | — | secret client OAuth2 (si client confidentiel) |
| `TCH_OPENING_FLOAT` | non | `100.00` | float de session si ouverture nécessaire |
| `TCH_DRAWS_PAGE_SIZE` | non | `100` | taille de page pour `/admin/draws/today` |
| `TCH_SESSION_ID` | non | — | session POS déjà ouverte (sinon phase 2 relance phase 1) |
| `TCH_DRAW_LIMIT` | non | `20` | nombre max de draws vendables à traiter en phase 2 |
| `TCH_CURRENCY` | non | `CAD` | devise de la vente cashier |
| `TCH_STAKE` | non | `1.00` | mise unitaire par ticket |
| `TCH_GAME_CODE` | non | `HT_BOLET` | jeu utilisé pour la vente automatisée |
| `TCH_BET_TYPE` | non | `MATCH_1_2D` | type de pari utilisé pour la vente automatisée |
| `TCH_GAME_PROFILES` | non | — | profils multi-jeux fallback (ex: `BOLET,MARYAJ,LOTO3` ou `HT_BOLET:MATCH_1_2D,HT_MARYAJ:MARRIAGE_2D2D,HT_LOTO3:LOTTO3_3D`) |
| `TCH_SELECTION_PLAN` | non | — | plan exact des lignes du ticket (ex: `BOLET=2,MARYAJ=3,LOTO3=2,LOTO4=2,LOTO5=1`, total 10 pour le scénario matin) |
| `TCH_SELL_MODE` | non | `ONE_TICKET_PER_GAME` | stratégie de vente: `ONE_TICKET_PER_GAME` ou `SINGLE_TICKET_MULTI_GAME` |
| `TCH_SELECTIONS_PER_TICKET` | non | `3` | nombre de sélections `lines` envoyées dans un ticket |
| `TCH_ARTIFACT_DIR` | non | `.tmp/e2e-phase2-pdfs` | dossier de sortie des PDF et JSON |

`*` Obligatoire si les credentials (`USERNAME`/`PASSWORD`) correspondants ne sont pas fournis.

## Lancer le script

```zsh
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server
chmod +x scripts/e2e-phase1-bootstrap.sh
export TCH_SUPER_ADMIN_TOKEN="<super-admin-jwt>"
export TCH_SELLER_TOKEN="<cashier-ou-tenant-admin-jwt>"
zsh scripts/e2e-phase1-bootstrap.sh
```

Alternative (récupération automatique des tokens) :

```zsh
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server
chmod +x scripts/e2e-phase1-bootstrap.sh
export TCH_SUPER_ADMIN_USERNAME="superadmin"
export TCH_SUPER_ADMIN_PASSWORD="<password>"
export TCH_SELLER_USERNAME="cashier"
export TCH_SELLER_PASSWORD="<password>"
zsh scripts/e2e-phase1-bootstrap.sh
```

Puis pour vendre et générer les PDF:

```zsh
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server
chmod +x scripts/e2e-phase2-sell-pdf.sh
zsh scripts/e2e-phase2-sell-pdf.sh
```

Exemple multi-jeux avec plan exact de 10 sélections par ticket:

```zsh
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server
export TCH_SELECTION_PLAN='BOLET=2,MARYAJ=3,LOTO3=2,LOTO4=2,LOTO5=1'
export TCH_SELL_MODE='SINGLE_TICKET_MULTI_GAME'
zsh scripts/e2e-phase2-sell-pdf.sh
```

## Exemple de sortie

```text
==> Phase 1 · ouverture des tirages du jour
    Tirages ouverts/confirmés: 2
==> Vérification de la session POS courante
    Aucune session ouverte, création en cours
    Session créée: 00000000-0000-0000-0000-000000003202
==> Listing des tirages du jour
    Total tirages du jour: 3
    Tirages OPEN: 2
      - HT_NY_MID | 11111111-1111-1111-1111-111111111111 | 2026-05-19T14:30:00Z
      - HT_FL_EVE | 22222222-2222-2222-2222-222222222222 | 2026-05-19T22:30:00Z

==> Exports pour la phase 2
export TCH_BASE_URL="http://localhost:8083/api/v1"
export TCH_TENANT_ID="00000000-0000-0000-0000-000000000003"
export TCH_OUTLET_ID="00000000-0000-0000-0000-000000003001"
export TCH_TERMINAL_ID="00000000-0000-0000-0000-000000003101"
export TCH_SESSION_ID="00000000-0000-0000-0000-000000003202"
```

## Flow mobile minimal recommandé

Pour qu’une app mobile vendeur puisse fonctionner avec le backend actuel, la séquence recommandée est:

1. **connexion** → récupérer un JWT `CASHIER` / `TENANT_ADMIN` ;
2. **session POS** → récupérer ou ouvrir la session (`/tenant/sessions/current`, `/tenant/sessions/open`) ;
3. **draws vendables** → `GET /tenant/cashier/draws/sellable` ;
4. **vente** → `POST /tenant/cashier/sell` avec les headers:
   - `X-Tch-Terminal-Id`
   - `X-Tch-Outlet-Id`
   - `X-Tch-Sales-Session-Id`
5. **PDF ticket** → soit lire `data.receipt.base64` dans la réponse de vente, soit appeler `GET /tenant/tickets/{ticketId}/print.pdf`.

Par défaut, la phase 2 envoie `TCH_SELECTIONS_PER_TICKET=3` sélections dans le même ticket. Changez cette valeur si vous voulez vendre un ticket simple ou un ticket plus riche.

Avec `TCH_GAME_PROFILES='BOLET,MARYAJ,LOTO3'`:

- mode `ONE_TICKET_PER_GAME` (défaut phase 2) : **un ticket par draw et par type de jeu** ;
- mode `SINGLE_TICKET_MULTI_GAME` : **un seul ticket par draw** qui mélange BOLET + MARYAJ + LOTO3 dans `lines`.

Avec `TCH_SELECTION_PLAN='BOLET=2,MARYAJ=3,LOTO3=2,LOTO4=2,LOTO5=1'`, la phase 2 ignore `TCH_SELECTIONS_PER_TICKET` et génère exactement 10 lignes dans le ticket unique multi-jeux.

Profils utilisés:

- `BOLET` → `HT_BOLET/MATCH_1_2D` (sélections aléatoires `00..99`)
- `MARYAJ` ou `2X2` → `HT_MARYAJ/MARRIAGE_2D2D` (paires aléatoires `00-99`)
- `LOTO3` → `HT_LOTO3/LOTTO3_3D` (valeurs issues de `00..99`, encodées en `0xx`)
- `LOTO4` → `HT_LOTO4/LOTTO4_PATTERN` avec `betOption=1`
- `LOTO5` → `HT_LOTO5/LOTTO5_PATTERN` avec `betOption=1`

## Exécution automatique tous les matins à 7h

Le repo fournit `scripts/e2e-cashier-morning.sh` comme wrapper cron. Il force `TCH_SELL_MODE=SINGLE_TICKET_MULTI_GAME` pour le scénario "cashier morning" (ticket unique multi-jeux) et utilise par défaut `TCH_SELECTION_PLAN=BOLET=2,MARYAJ=3,LOTO3=2,LOTO4=2,LOTO5=1`.

### Configuration Keycloak pour récupérer les tokens

Pour cette première passe, utiliser le client `tchalanet-swagger`.

Dans Keycloak Admin UI:

1. ouvrir le realm `tchalanet`;
2. ouvrir le client `tchalanet-swagger`;
3. activer **Direct access grants** pour autoriser `grant_type=password`;
4. laisser le client public pour tester sans secret, ou activer **Client authentication** pour tester avec secret;
5. si **Client authentication** est activé, copier le secret depuis l’onglet Credentials dans `TCH_AUTH_CLIENT_SECRET`;
6. vérifier que le token contient les rôles dans `realm_access.roles` ou dans le claim plat `roles` (`SUPER_ADMIN`, `TENANT_ADMIN`, `CASHIER` selon le compte).

Variables minimales:

```zsh
export TCH_AUTH_TOKEN_URL='http://auth.tchalanet.lan/realms/tchalanet/protocol/openid-connect/token'
export TCH_AUTH_CLIENT_ID='tchalanet-swagger'
export TCH_AUTH_CLIENT_SECRET='' # ou le secret si Client authentication = ON
export TCH_SUPER_ADMIN_USERNAME='super_admin'
export TCH_SUPER_ADMIN_PASSWORD='Changeme1!'
export TCH_SELLER_USERNAME='agent'
export TCH_SELLER_PASSWORD='Changeme1!'
```

Exemple de ligne `crontab -e` complète (07:00 tous les jours):

```zsh
0 7 * * * TCH_BASE_URL='http://api.tchalanet.lan/api/v1' TCH_AUTH_TOKEN_URL='http://auth.tchalanet.lan/realms/tchalanet/protocol/openid-connect/token' TCH_AUTH_CLIENT_ID='tchalanet-swagger' TCH_SUPER_ADMIN_USERNAME='super_admin' TCH_SUPER_ADMIN_PASSWORD='***' TCH_SELLER_USERNAME='agent' TCH_SELLER_PASSWORD='***' TCH_SELECTION_PLAN='BOLET=2,MARYAJ=3,LOTO3=2,LOTO4=2,LOTO5=1' TCH_DRAW_LIMIT='20' /bin/zsh /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/scripts/e2e-cashier-morning.sh
```

### Exemple curl mobile vendeur

```zsh
curl "$TCH_BASE_URL/tenant/cashier/draws/sellable?limit=20" \
  -H "Authorization: Bearer $TCH_SELLER_TOKEN"

curl "$TCH_BASE_URL/tenant/cashier/sell" \
  -X POST \
  -H "Authorization: Bearer $TCH_SELLER_TOKEN" \
  -H 'Content-Type: application/json' \
  -H "X-Tch-Terminal-Id: $TCH_TERMINAL_ID" \
  -H "X-Tch-Outlet-Id: $TCH_OUTLET_ID" \
  -H "X-Tch-Sales-Session-Id: $TCH_SESSION_ID" \
  --data '{
    "terminalId": "00000000-0000-0000-0000-000000003101",
    "drawId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1",
    "currency": "CAD",
    "lines": [
      {
        "gameCode": "HT_BOLET",
        "selection": "11",
        "stake": 1.00,
        "betType": "MATCH_1_2D"
      },
      {
        "gameCode": "HT_BOLET",
        "selection": "12",
        "stake": 1.00,
        "betType": "MATCH_1_2D"
      }
    ],
    "printFormat": "PDF"
  }'

curl "$TCH_BASE_URL/tenant/tickets/<ticketId>/print.pdf" \
  -H "Authorization: Bearer $TCH_SELLER_TOKEN" \
  -H 'Accept: application/pdf' \
  --output ticket.pdf
```

## Erreurs rencontrées et solutions appliquées

| Erreur rencontrée | Impact | Solution appliquée |
| --- | --- | --- |
| Les fichiers `scripts/e2e-phase1-bootstrap.sh` et `scripts/E2E_README.md` n’étaient pas présents dans le workspace malgré le plan précédent. | Impossible de lancer ou relire le bootstrap annoncé. | Création effective du script et de sa documentation dans `scripts/`. |
| L’endpoint `GET /tenant/sessions/current` retourne `204 No Content` quand aucune session n’existe, pas `404`. | Un script naïf aurait interprété l’absence de session comme une erreur. | Gestion explicite du statut `204` pour ouvrir automatiquement la session POS. |
| La liste des tirages admin est paginée sous `ApiResponse.data.content`, pas renvoyée comme tableau racine. | Le parsing JSON pouvait rater les tirages du jour. | Ajout d’un parseur Python ciblé sur `data.content` et filtrage sur `status == OPEN`. |
| Les identifiants de session ne sont pas au même chemin JSON selon l’endpoint (`data.id` pour current, `data.sessionId` pour open). | Le script pouvait exporter une valeur vide. | Parsing différencié selon la réponse courante vs. réponse de création. |
| Le test live dépend de JWT réels et d’un backend local. | Validation incomplète si on se limite à `zsh -n`. | Ajout d’un harnais `scripts/tests/test_e2e_phase1_bootstrap.py` qui exécute le script de bout en bout sur faux serveur. |
| Sous `zsh`, le nom de variable spéciale `status` est en lecture seule. | Le script tombait immédiatement avec `read-only variable: status`. | Renommage systématique en `http_code` pour les variables internes liées aux retours HTTP. |
| Le flow mobile/cashier n’exposait pas de query HTTP avec `drawId` pour lister les draws réellement vendables. | Le mobile ne pouvait pas découvrir les draws `OPEN` à vendre sans endpoint admin. | Ajout de `GET /tenant/cashier/draws/sellable`, basé sur une nouvelle query core dédiée aux draws `OPEN` du jour. |
| Générer seulement le PDF base64 depuis la réponse cashier ne validait pas le endpoint binaire de reçu. | Risque de backend partiellement fonctionnel pour le mobile. | La phase 2 génère maintenant deux preuves: PDF depuis `data.receipt.base64` et PDF depuis `GET /tenant/tickets/{ticketId}/print.pdf`. |
| La phase 2 devait rester lançable même quand aucune session POS n’est exportée au préalable. | Friction forte pour les tests manuels et pour les futures itérations mobile. | Le script `e2e-phase2-sell-pdf.sh` relance automatiquement la phase 1 si `TCH_SESSION_ID` est absent. |

## Validation syntaxe

```zsh
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server
zsh -n scripts/e2e-phase1-bootstrap.sh
zsh -n scripts/e2e-phase2-sell-pdf.sh
```
