# Game — Domaine fonctionnel

## Rôle

Le domaine game décrit les jeux disponibles, leurs types de pari et leurs options commerciales.
Le tenant choisit ensuite quels jeux et options sont réellement proposés à la vente.

Exemples d'options :

| Jeu | Options |
|---|---|
| Maryaj | Exact, permuté |
| Loto 3 | Exact, box, exact + box |
| Maryaj gratis | Exact ou permuté selon la configuration du jeu |

Les options visibles au POS doivent correspondre à l'offre commerciale du tenant. Le backend valide
toujours l'option envoyée par la vente.

Voir aussi : [Configuration admin des jeux](../guides/admin-game-configuration.md).

## Cross-apps

### Web

- Pages admin : Jeux & tarifs, configuration du jeu, options de vente.

### API

- `/admin/games`
- `/admin/games/{gameCode}/bet-options`

## Pointeurs (source of truth near-code)

- Backend: `tchalanet-server/platform/tenantgame` pour la configuration tenant.
- Catalog source: `catalog.game.api`.
