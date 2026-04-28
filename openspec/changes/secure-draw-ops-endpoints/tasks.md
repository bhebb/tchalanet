## 1. Réactivation des @PreAuthorize (controllers draw)

- [ ] 1.1 `DrawAdminController` — supprimer le commentaire `// @PreAuthorize("hasAuthority('SUPER_ADMIN')") //todo remove testing` et activer l'annotation au niveau classe ; ajouter l'import `org.springframework.security.access.prepost.PreAuthorize`
- [ ] 1.2 `DrawCalendarOpsController` — supprimer le commentaire `// @PreAuthorize("hasAuthority('SUPER_ADMIN')")` et activer l'annotation au niveau classe ; ajouter l'import correspondant
- [ ] 1.3 `DrawResultsOpsController` — ajouter `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` au niveau classe (pas d'annotation existante) + import
- [ ] 1.4 Vérifier `AdminTchalaController` (core.haiti) — les `@PreAuthorize` commentés sont hors scope de ce change ; créer un TODO ticket séparé si nécessaire (ne pas modifier dans ce change)
- [ ] 1.5 Scanner tous les controllers `/admin/**` et `/platform/**` restants pour détecter d'autres annotations absentes ou commentées (audit exhaustif)

## 2. Règle ArchUnit

- [ ] 2.1 Créer `src/test/java/com/tchalanet/server/arch/SecurityArchTest.java` dans le package existant `arch/` — la règle vérifie que tout `@RestController` dont le path `@RequestMapping` commence par `/admin/`, `/platform/`, ou `/_sdr/` porte `@PreAuthorize` au niveau classe ou sur chaque méthode handler publique
- [ ] 2.2 Si un endpoint doit être public dans ces scopes, il DOIT porter `@PreAuthorize("permitAll()")` explicitement (whitelist — pas de bypass silencieux)
- [ ] 2.3 Vérifier que le test compile et passe avec `./mvnw test -pl . -am -Dtest=SecurityArchTest -q`

## 3. Tests @WebMvcTest (sécurité 401/403)

- [ ] 3.1 Créer `DrawAdminControllerSecurityTest.java` — test `@WebMvcTest(DrawAdminController.class)` : requête sans auth sur `GET /admin/draws` → 401, requête avec token invalide → 403
- [ ] 3.2 Créer `DrawCalendarOpsControllerSecurityTest.java` — tests 401 sur `POST /platform/ops/draws/generate`, `POST /platform/ops/draws/close-due`
- [ ] 3.3 Créer `DrawResultsOpsControllerSecurityTest.java` — tests 401 sur `POST /platform/ops/draw-results/override`, `POST /platform/ops/draw-results/manual`
- [ ] 3.4 Vérifier que les 3 tests passent avec `./mvnw test -pl . -am -Dtest="DrawAdminControllerSecurityTest,DrawCalendarOpsControllerSecurityTest,DrawResultsOpsControllerSecurityTest" -q`

## 4. Documentation

- [ ] 4.1 Ajouter une section `## 13) Règle ArchUnit — sécurité des scopes protégés` dans `docs/conventions/api/web_api.md` documentant la règle (path prefixes couverts, comportement en cas de violation, comment whitelister un endpoint public)
- [ ] 4.2 Mettre à jour la checklist PR final (§12 de `web_api.md`) pour inclure `- [ ] Sécurité : `@PreAuthorize` actif sur tout controller admin/ops`

## 5. Vérification finale

- [ ] 5.1 Rechercher `"todo remove testing"` dans le codebase → zéro résultat attendu
- [ ] 5.2 Rechercher `//\s*@PreAuthorize` dans le scope draw → zéro résultat attendu
- [ ] 5.3 Lancer `./mvnw compile -pl . -am -q` → build propre (0 erreurs nouvelles)
- [ ] 5.4 Lancer `./mvnw test -pl . -am -q` — s'assurer que les tests arch et sécurité passent
