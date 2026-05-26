# Prompt pour challenger le cadrage

Nous avons besoin de challenger ce cadrage Tchalanet V1.

Contexte :
- Tchalanet modélise un réseau de vente.
- User = authentification.
- Seller = machann / vendeur transactionnel métier.
- Cashier = feature UI/POS.
- Outlet = lieu, institution, banque, partenaire, point mobile ou canal de vente.
- Sales = création ticket et snapshots.
- LimitPolicy = limites de vente, scope SELLER inclus.
- Promotion V1 = FREE_GAME_LINE, BOOST_ODDS, WAIVE_CHARGE.
- Pas de `core.compensation` générique en V1.
- Pas de `core.partner` séparé en V1.
- Pas de prepaid ledger financier en V1 ; prepaid-like = limitpolicy scope SELLER si simple.

Décision proposée :
- Remplacer `core.agent` par `core.seller`.
- `core.seller` contient seulement Seller, SellerOutletAssignment historisé, SellerCommissionPolicy simple.
- `core.outlet` contient OutletKind pour couvrir institutions/banques/partenaires.
- `features.cashier` orchestre l'UI.
- `core.sales` résout seller, revalide seller/assignment transactionnellement, puis snapshotte seller/commission/promotion/charges.

Questions :
1. Est-ce que `core.seller` est la bonne frontière ou faut-il garder le nom `agent` ?
2. Est-ce que `partner` doit rester un OutletKind V1 ou devenir un domaine maintenant ?
3. Est-ce que seller commission simple doit rester dans `core.seller` ou faut-il déjà extraire compensation ?
4. Est-ce que prepaid-like comme `LimitPolicy scope SELLER` suffit pour V1 ?
5. Quels risques voyez-vous dans le flow Sales qui résout seller par user + outlet + session puis revalide transactionnellement ?
6. Quels impacts oubliés dans settlement/payout/stats ?
