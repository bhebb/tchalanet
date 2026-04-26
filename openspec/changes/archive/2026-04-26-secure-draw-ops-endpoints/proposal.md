## Why

Plusieurs controllers draw admin/ops ont leur `@PreAuthorize` commenté avec un TODO de test, exposant des endpoints capables d'insérer des résultats de tirage, générer ou annuler des draws sans aucune authentification. C'est un trou de sécurité P0 qui doit être colmaté avant tout autre changement sur ce domaine.

## What Changes

- Réactiver `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` sur `DrawAdminController` (classe entière, supprimer le commentaire `//todo remove testing`)
- Réactiver `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` sur `DrawCalendarOpsController` (classe entière)
- Vérifier et activer `@PreAuthorize` sur `DrawResultsOpsController` (actuellement sans annotation au niveau classe)
- Vérifier tous les autres controllers sous `/admin/**` et `/platform/ops/**` pour détecter d'éventuels `@PreAuthorize` absents ou commentés
- Créer une règle ArchUnit `SecurityArchTest.java` : tout `@RestController` dont le mapping commence par `/admin/`, `/platform/`, ou `/_sdr/` **DOIT** porter un `@PreAuthorize` au niveau classe ou méthode — le test fait échouer le build si violation
- Ajouter des tests `@WebMvcTest` par controller protégé pour vérifier que requête sans auth → 401/403
- Documenter la règle ArchUnit dans `docs/conventions/api/web_api.md`

## Capabilities

### New Capabilities

- `auth-rbac`: Règles d'autorité (RBAC) exigées par scope de route (`/admin/`, `/platform/`, `/_sdr/`) — définit quelles autorités Spring Security sont requises et comment la conformité est vérifiée via ArchUnit.

### Modified Capabilities

_(aucune — pas de changement de comportement fonctionnel, uniquement réactivation de sécurité et ajout de garde-fous qualité)_

## Impact

- **Code modifié** : `DrawAdminController.java`, `DrawCalendarOpsController.java`, `DrawResultsOpsController.java`
- **Code créé** : `SecurityArchTest.java` (test), spec `openspec/specs/auth-rbac/spec.md`
- **Docs mises à jour** : `docs/conventions/api/web_api.md` (section ArchUnit)
- **Tests ajoutés** : `DrawAdminControllerSecurityTest.java`, `DrawCalendarOpsControllerSecurityTest.java`, `DrawResultsOpsControllerSecurityTest.java`
- **API** : aucun changement de contrat — seule l'authN est rétablie
- **Dépendances** : `com.tngtech.archunit:archunit-junit5` doit être présente dans le POM (à vérifier)
