# Cashier E2E tests

Trois suites pytest, du plus large au plus ciblé :

| Fichier                  | Portée                                          | Quand le lancer                              |
| ------------------------ | ----------------------------------------------- | -------------------------------------------- |
| `test_happy_path.py`     | Journée complète vendeur (matrice draws × jeux) | Smoke test global après chaque déploiement   |
| `test_single_ticket.py`  | Un ticket multi-jeux sur un tirage              | Itération sur le contrat sell/print/send     |
| `test_layouts.py`        | 8 scénarios paramétrés pour le rendu reçu       | Itération sur le format PDF / ESC-POS        |

## Bootstrap commun

Les trois suites partagent les mêmes prereqs idempotents — exécutés une fois (happy_path,
single_ticket) ou une fois par test (layouts) :

1. `prereqs.app_user.ensure_app_user_synced` — `POST /platform/ops/sync/identity/keycloak-bootstrap-users`
2. `prereqs.draws.ensure_draws_today` — `POST /platform/ops/draws/generate` puis
   `POST /platform/ops/draws/open-due` (horizon 24h)
3. `prereqs.session.ensure_pos_session_open` — `GET /tenant/cashier/session/current`
   sinon `POST /tenant/cashier/session/open`

Tout est sans effet si déjà fait → re-runs gratuits.

## test_happy_path.py

Scénario unique `test_cashier_morning_happy_path`. Pour **chaque** (`draw` × `gameCode`
configuré sur le channel) :

- `preview` → `ACCEPTABLE` attendu
- `sell` (Idempotency-Key fresh) → `ACCEPTED` + `backup` populé
- `print PDF` → bytes commencent par `%PDF`, écrit sous `target/pdf/cashier-happy-path/`
- `send_slack` → `queued: true`
- `get_ticket` → `ticketCode` correspond

Puis `list_tickets` (pagination complète via `hasNext`) → tous les `ticketId` vendus
doivent apparaître.

Sortie : ~55 PDFs (selon le nombre de draws OPEN × jeux configurés).

## test_single_ticket.py

Test rapide pour valider une boucle `preview → sell → print PDF + ESC_POS → send Slack`.
Vend **un seul ticket** sur le **premier draw OPEN**, avec **toutes ses gameCodes** comme
lignes (1 sélection par jeu, profil `_GAME_BET_PROFILE` dans `flows/cashier.py`).

Utile pour :

- valider qu'un nouveau formatter ne plante pas
- inspecter rapidement le contenu d'un PDF (1 fichier vs 55)
- voir le rendu ESC-POS (dump cp437 en stdout)
- tester l'enqueue Slack sans saturer

Sortie : 1 PDF + 1 `.escpos` sous `target/pdf/single/`.

## test_layouts.py

Test paramétré, **8 scénarios** pour vérifier visuellement la mise en page du reçu :

| #     | Nom                       | Cas                                                                     |
| ----- | ------------------------- | ----------------------------------------------------------------------- |
| `01`  | `short_bolet`             | 1 ligne BOLET                                                           |
| `02`  | `medium_mixed`            | 3 jeux (BOLET + MARYAJ + LOTO3), 2 sélections chacun                    |
| `03`  | `long_30_lines`           | ~30 lignes (20 BOLET + 5 MARYAJ + 5 LOTO5) — stress vertical            |
| `10`  | `maryaj_options`          | MARYAJ Ordre exact + Revers / Double                                    |
| `11`  | `loto3_options`           | LOTO 3 Exact + Box                                                      |
| `12`  | `loto4_options`           | LOTO 4 Exact + Box + Front pair (2 chiffres) + Back pair (2 chiffres)   |
| `13`  | `loto5_options`           | LOTO 5 — Lot1+Lot2, Lot1+Lot3, Lot2+Lot3                                |
| `20`  | `large_potential_gain`    | LOTO4 + LOTO5 avec mises élevées (`Gain max` ≥ 250 000)                 |

Chaque scénario sort `{name}.pdf` + `{name}.escpos` sous `target/pdf/layouts/`.

**Comment ajouter un scénario** : éditer `test_layouts.py`, créer une nouvelle fonction
`_xxx_ticket()` qui retourne un `Scenario`, ajouter dans la liste `SCENARIOS`. Le test
est paramétré automatiquement.

### Edge cases à couvrir (TODO)

- Devise mixte sur le ticket (HTG + CAD) — pas encore exposé par le contrat sell
- Sélection longue (>12 chars) si un jeu futur l'autorise
- `Gain max` dans le milliard (`1 000 000 000.00`) — vérifier que la colonne ne wrap pas
- Ticket sans buyer charges (uniquement TENANT charges)
- Channel papier 58mm (vérifier que la colonne `Gain` n'overflow pas)

## Configuration

`.env.local` à `tchalanet-server/scripts/.env.local`. Variables :

| Variable                       | Défaut                              | Rôle                                |
| ------------------------------ | ----------------------------------- | ----------------------------------- |
| `TCH_BASE_URL`                 | (obligatoire)                       | URL API locale                      |
| `TCH_KEYCLOAK_TOKEN_URL`       | (obligatoire)                       | Endpoint OAuth2 token               |
| `TCH_KEYCLOAK_CLIENT_ID`       | `tchalanet-swagger`                 |                                     |
| `TCH_KEYCLOAK_CLIENT_SECRET`   | (vide si client public)             |                                     |
| `TCH_SUPER_ADMIN_USERNAME/PASSWORD` | (obligatoire)                  | bootstrap + ops endpoints           |
| `TCH_SELLER_USERNAME/PASSWORD` | (obligatoire)                       | Cashier user                        |
| `TCH_TENANT_ID`                | `00000000-0000-0000-0000-000000000003` | Seed UUID tenant                 |
| `TCH_OUTLET_ID`                | `00000000-0000-0000-0000-000000003001` | Seed UUID outlet                 |
| `TCH_TERMINAL_ID`              | `00000000-0000-0000-0000-000000003101` | Seed UUID terminal               |
| `TCH_STAKE_CENTS`              | `100` (= 1.00)                      | Mise unitaire (cents)               |
| `TCH_GENERATE_DAYS`            | `7`                                 | Profondeur génération draws         |
| `TCH_E2E_VERIFY_SSL`           | `false`                             | SSL verify (dev mkcert)             |
| `TCH_TEST_SLACK_CHANNEL_KEY`   | `delivery`                          | Canal Slack par défaut pour `send`  |
| `TCH_ARTIFACT_DIR`             | `tchalanet-app/target/pdf/...`      | Override la racine d'écriture PDF   |

## Lancer

```bash
cd tchalanet-server/tests/e2e
uv sync                                       # ou: pip install -e .
uv run pytest tests/cashier -v -s             # tout
uv run pytest tests/cashier/test_layouts.py::test_receipt_layout -k loto4 -v -s
```

`-s` est important pour voir les `print(...)` de progression (paths des artefacts,
ticketCode vendu, backup text, etc.).

## Profils de jeu — `_GAME_BET_PROFILE`

Défini dans `flows/cashier.py`. Mapping `gameCode → (betType, [selections...], betOption)` :

```python
"HT_BOLET":  ("MATCH_1_2D",      ["11", "22", "33"],   None),
"HT_MARYAJ": ("MARRIAGE_2D2D",   ["21-25", "33-77"],   1),
"HT_LOTO3":  ("LOTTO3_3D",       ["012", "345"],       1),
"HT_LOTO4":  ("LOTTO4_PATTERN",  ["1234", "5678"],     1),
"HT_LOTO5":  ("LOTTO5_PATTERN",  ["12345", "67890"],   1),
```

⚠️ Le profil utilise le **betOption par défaut** pour chaque jeu (ex: `1` = Exact /
Ordre exact). Pour tester d'autres options, soit ajouter dans `_GAME_BET_PROFILE` une
entrée séparée, soit utiliser `flow.sell_lines(draw, [LineSpec(...)])` comme dans
`test_layouts.py` (raw line specs, pas de profil).

Matrice betType/betOption/selection complète : voir
`tchalanet-features/.../cashier/MOBILE_FLOW.md` §7.

## Trois patterns d'erreur fréquents

| Symptôme côté test                                    | Cause probable                                       | Remède                                             |
| ----------------------------------------------------- | ---------------------------------------------------- | -------------------------------------------------- |
| `Sell did not result in ACCEPTED — outcome=REJECTED`  | `selection` incompatible avec `betType + betOption`  | Voir matrice mobile §7 ; ex LOTO4 front=2 chiffres |
| `403 seller_context.permission_denied`                | Cashier user non synchro avec app_user              | Vérifier `ensure_app_user_synced` + role CASHIER   |
| `Expected (...) ... got 500: ...`                     | Bug serveur (post-commit listener ?)                 | `errorId` dans la réponse → grep dans log Spring   |

## Artefacts générés (gitignored)

- `tchalanet-app/target/pdf/cashier-happy-path/` (happy path)
- `tchalanet-app/target/pdf/single/` (single ticket)
- `tchalanet-app/target/pdf/layouts/` (layout scenarios)

`target/` est ignoré par Maven (`tchalanet-server/.gitignore`).
