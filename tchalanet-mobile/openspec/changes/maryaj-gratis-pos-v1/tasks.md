# Tasks — maryaj-gratis-pos-v1

## 1. Data layer

- [ ] DTOs `SalePreparationView`, `SalePreparationPromotionLineView`, `ConfirmPreparedSaleResult` (miroir du contrat serveur).
- [ ] Client API : `POST /tenant/sales/preparations` (avec terminal proof), `.../promotion-lines/{lineRef}/regenerate`, `.../confirm` (proof + Idempotency-Key).
- [ ] Mapping des erreurs `sales.preparation.*` vers des messages vendeur.

## 2. Flux de vente

- [ ] Remplacer (ou doubler derrière un flag) le preview stateless par `prepare` dans le flux cashier.
- [ ] Gérer le TTL : compte à rebours / re-préparation si `expired`.
- [ ] Confirm par `preparationId` + clé d'idempotence générée localement ; rejouer la même clé sur retry réseau.

## 3. UI

- [ ] Afficher la section « Promotion appliquée » : ligne Maryaj gratuit + numéros + mise de base.
- [ ] Bouton « Régénérer » si `regenerable && regenerationsRemaining > 0` ; afficher le compteur restant.
- [ ] Désactiver toute régénération après confirmation.
- [ ] i18n fr/ht/en des libellés (« Maryaj gratuit offert », « Régénérer »).

## 4. Impression / reçu

- [ ] Vérifier que le reçu imprimé affiche la ligne offerte avec les numéros confirmés (dépend slice 10 serveur).

## 5. Tests

- [ ] Tests widget : ligne promo affichée, bouton régénérer, compteur.
- [ ] Tests flux : prepare -> regenerate -> confirm ; replay idempotent ; expiration -> re-préparation.
