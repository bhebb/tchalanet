# DrawResult — Domaine fonctionnel

## Rôle

DrawResult ingère, normalise et expose les résultats de tirage. Il ne calcule pas les gains des
tickets.

Pipeline fonctionnel :

1. Le résultat est fetché ou saisi manuellement.
2. Le résultat est projeté en valeurs métier utiles (`lot1`, `lot2`, `lot3`, `lot4`, paires).
3. Le draw fermé est lié au résultat.
4. Sales applique le résultat aux tickets du draw.
5. Settlement persiste les gains réalisés.

Voir aussi : [Draw execution](../flows/draw-execution.md) et
[Configuration admin des jeux](../guides/admin-game-configuration.md).

## Cross-apps

### Web

- Pages admin : Résultats des tirages, détail résultat.
- Pages publiques : résultats, détail résultat.
- Page publique `/rules` : règles des jeux et simulation indicative.

Sur l’admin, le détail résultat utilise un `mat-tab-group` Angular :

| Tab | Composant |
|---|---|
| Résultats | `tch-console-draw-result-summary` |
| Combinaisons | `tch-console-draw-result-combinations` |
| Brut | `tch-console-draw-result-raw` |

La simulation publique reste séparée du résultat officiel : elle vit dans `PublicRulesPage` (`/rules`) et sert d’aide pédagogique.

### API

- `/platform/ops/draw-results/**`
- endpoints publics résultats via public portal.

## Pointeurs (source of truth near-code)

- Backend: `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/drawresult/DOMAIN_DRAWRESULT.md`
