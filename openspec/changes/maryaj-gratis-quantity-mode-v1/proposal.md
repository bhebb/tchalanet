# maryaj-gratis-quantity-mode-v1

## Why

Maryaj gratis doit supporter trois modes métier : une quantité fixe de lignes gratuites, une quantité calculée par tranche régulière de montant payé, et une quantité calculée par paliers commerciaux. Le calcul final doit être serveur pour rester cohérent entre POS/web, tickets, rapports, audit et settlement.

## What

- Ajouter `quantityMode = FIXED | PER_PAID_AMOUNT | TIERED_PAID_AMOUNT`.
- Ajouter `stepPaidAmount`, `quantityPerStep`, `maxQuantity`.
- Ajouter `quantityTiers` pour les offres du type 100-199 HTG => 1, 200-499 HTG => 2, 500+ HTG => 3.
- Calculer la quantité effective dans l'évaluation promotion backend.
- Exposer les champs dans l'activation Maryaj gratis côté admin web avec des libellés métier, incluant le gain par Maryaj gagnant.
- Conserver la compatibilité du mode fixe existant.

## Impact

- Backend core promotion et sales via effets existants.
- Web admin Maryaj gratis.
- Migration DB sur `promotion_rule_effect`.

## Non-goals

- Refondre toutes les promotions.
- Déplacer Maryaj gratis hors du domaine promotion.
- Implémenter un workflow POS complet de preview/régénération si non déjà présent.
