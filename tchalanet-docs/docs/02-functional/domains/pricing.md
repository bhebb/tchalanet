# Pricing — Domaine fonctionnel

## Rôle

Le pricing définit les règles de gain utilisées au moment de la vente. Ces règles sont runtime et
tenant-scoped : elles ne sont pas un simple catalogue plateforme.

Deux familles existent :

| Famille | Usage |
|---|---|
| Multiplicateur de mise | Jeux payants : gain réalisé = mise x multiplicateur. |
| Montant fixe | Maryaj gratis : gain réalisé = montant configuré. |

Les seller-terminals peuvent avoir des overrides. Un override modifie la valeur effective pour un
terminal donné, mais ne change pas le type de règle configuré au niveau tenant.

Voir aussi : [Configuration admin des jeux](../guides/admin-game-configuration.md).

## Cross-apps

### Web

- Pages admin : Jeux & tarifs, Barèmes / pricing rules, overrides seller-terminal.

### API

- `/admin/controls/pricing-rules`

## Pointeurs (source of truth near-code)

- Backend: `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/pricing/DOMAIN_PRICING.md`
