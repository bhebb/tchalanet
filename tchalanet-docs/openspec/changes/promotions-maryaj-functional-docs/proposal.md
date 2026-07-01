# promotions-maryaj-functional-docs

## Why

La configuration de Maryaj gratis et des promotions doit être expliquée dans la documentation fonctionnelle. Maryaj gratis n'est pas seulement une campagne promotionnelle générique : elle génère des lignes de jeu gratuites pendant la vente et doit rester cohérente avec le POS, l'impression, les rapports et le settlement.

## What

- Ajouter une page fonctionnelle `maryaj-gratis.md`.
- Ajouter une page fonctionnelle `autres-promotions.md`.
- Documenter le calcul du total des lignes payantes pendant la saisie.
- Documenter les modes d'attribution Maryaj gratis : quantité fixe et par tranche de montant payé.
- Clarifier ce qui appartient au backend, au POS/web, aux tickets imprimés et aux rapports.

## Impact

- Documentation uniquement.
- Pas de changement backend ou web dans ce change OpenSpec.

## Non-goals

- Définir les schémas API définitifs.
- Implémenter `quantityMode = PER_PAID_AMOUNT`.
- Décrire toutes les promotions futures en détail.
