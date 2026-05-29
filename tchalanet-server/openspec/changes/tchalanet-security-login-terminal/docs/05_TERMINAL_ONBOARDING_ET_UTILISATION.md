# Terminal onboarding et utilisation

La source stable près du code est :

```text
tchalanet-core/src/main/java/com/tchalanet/server/core/terminal/terminal_onboarding.md
```

Résumé OpenSpec :

- POS physique V1 : `PHYSICAL + POS`, challenge `POS_PAIRING`, binding `POS_DEVICE`.
- Mobile vendeur V1 : `VIRTUAL + MOBILE`, challenge `MOBILE_OTP`, binding `MOBILE_APP`.
- Web/back-office : `VIRTUAL + WEB|BACK_OFFICE`, sélection admin contrôlée serveur.
- Dev : `MOBILE_OTP` peut être livré via `SLACK` ou `EMAIL` pour éviter les coûts SMS.
- E2E automatisé : `TEST_CAPTURE` permet de récupérer le code clair via une surface test-only.
- Live : SMS seulement pour activation, changement d'appareil, reset binding, suspicion fraude ou step-up risque.
- Le payload de vente ne fournit jamais `sellerId`.
- Une opération sensible exige permission user, capability terminal, outlet flag et session valide.
