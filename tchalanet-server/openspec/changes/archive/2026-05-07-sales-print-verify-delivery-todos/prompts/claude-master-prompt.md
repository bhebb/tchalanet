# Claude master prompt

Tu es dans le repo Tchalanet. Travaille sur `tchalanet-server` Java 25 / Spring Boot 4.

Lis d’abord les règles projet :

- `AGENTS.md`
- `10-non-negotiables.md`
- `80-core-rules.md`
- `81-feature-rules.md`
- `web_api.md`
- `command_query_handlers.md`
- `persistence.md`
- `rls.md`
- `event_model.md`
- `cache.md`
- `audit.md`
- `typed_ids.md`

Objectif : implémenter progressivement le plan `tchalanet-sales-print-verify-delivery-todos`.

Contraintes absolues :

- Ne déplace pas `/sell` hors `core.sales`.
- Ne déplace pas `/print` hors `core.sales`.
- Déplace seulement public verify vers `features.ticketverify`.
- Ajoute `features.ticketdelivery` comme vertical slice, pas architecture hexagonale.
- Une feature ne lit jamais JPA/repositories/entities directement.
- Ne retourne jamais internal IDs dans public verify.
- QR print doit pointer vers `/ticket/{publicCode}`, pas `/api/v1`.
- Query handlers side-effect free.
- Command handlers transactionnels `@UseCase + @TchTx`.
- Pas de `Instant.now()` direct, utiliser `Clock`.
- Pas de `UUID.randomUUID()` direct hors infra, utiliser `IdGenerator`/typed IDs.
- Application pagination = `TchPageRequest`, pas Spring `PageRequest`.

Travaille en petits commits. À chaque étape :

1. Explique brièvement le changement.
2. Montre les fichiers modifiés.
3. Ajoute les tests correspondants.
4. Ne mélange pas les changes OpenSpec.

Ordre d’exécution :

1. DB views.
2. Print pipeline.
3. Feature ticketverify.
4. Feature ticketdelivery.
5. Controllers/mappers/events cleanup.
