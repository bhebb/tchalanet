# Tasks

- [x] Ajouter le modèle backend de mode quantité.
- [x] Persister les paramètres de tranche et de paliers.
- [x] Calculer la quantité effective côté backend.
- [x] Adapter l'API admin Maryaj gratis.
- [x] Adapter l'écran admin Maryaj gratis.
- [x] Documenter les modes fixe, tranche régulière et paliers.
- [x] Ajouter ou mettre à jour les tests ciblés.
- [ ] Valider backend et web.

Validation partielle :

- [x] Web : `pnpm exec tsc -p apps/admin-portal/tsconfig.app.json --noEmit`
- [x] Workspace : `git diff --check`
- [ ] Backend : `./mvnw -pl tchalanet-core -Dtest=PromotionRuleEvaluatorTest,InstantiateDefaultMaryajGratisCommandHandlerTest test`
  échoue à la compilation sur `FetchExternalResultsWindowCommandHandler` (`ResultSlotCatalog.existsLive(...)`
  absent), hors de ce changement.
