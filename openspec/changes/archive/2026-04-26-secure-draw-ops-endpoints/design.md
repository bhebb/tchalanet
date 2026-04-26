## Context

Le domaine `core.draw` expose deux controllers à haut risque :

- `DrawAdminController` (`/admin/draws`) — création/modification/override de draws et résultats
- `DrawCalendarOpsController` (`/platform/ops/draws`) — génération, ouverture, fermeture, application des résultats de tirage par un opérateur plateforme

Un troisième controller, `DrawResultsOpsController` (`/platform/ops/draw-results`), permet d'insérer manuellement des résultats, de les overrider, et de les "refresher" — sans `@PreAuthorize` au niveau classe.

Les sibling controllers (`OpsBatchJobController`, `OpsBatchExecutionController`, `OpsBatchGateController`) ont déjà `@PreAuthorize("hasRole('SUPER_ADMIN')")` actif — preuve que le pattern existe dans le codebase.

La convention `security_permissions.md §2` impose : **controllers MUST déclarer leurs requirements via annotations**. La règle n'est pas appliquée mécaniquement → risque de régression future.

## Goals / Non-Goals

**Goals:**

- Réactiver `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` sur les 3 controllers draw sans annotation active
- Supprimer les commentaires `//todo remove testing` résiduels
- Créer un test ArchUnit bloquant en CI qui détecte toute absence future de `@PreAuthorize` sur les paths protégés
- Ajouter des tests `@WebMvcTest` couvrant le cas 401/403 pour chaque controller protégé
- Documenter la règle ArchUnit dans `web_api.md`

**Non-Goals:**

- Migration vers `ApiResponse<T>` (`align-draw-events-and-conventions`)
- Refactor des handlers ou commandes draw
- Changement du modèle de permissions (RBAC roles/scopes) — on réutilise `hasAuthority('SUPER_ADMIN')` déjà utilisé dans le même périmètre
- Sécurisation d'autres domaines hors draw

## Decisions

### D1 — `hasAuthority` vs `hasRole`

Les sibling controllers ops utilisent `hasRole('SUPER_ADMIN')`, mais `DrawAdminController` et `DrawCalendarOpsController` avaient `hasAuthority('SUPER_ADMIN')` dans le code commenté (style Spring Security natif).

**Décision** : standardiser sur `hasAuthority('SUPER_ADMIN')` pour les controllers du domaine draw, en cohérence avec la valeur d'origine commentée. Aligner les controllers batch (`hasRole`) est hors scope (autre change).

Justification : `hasAuthority` et `hasRole('X')` sont équivalents si les tokens Keycloak incluent `ROLE_SUPER_ADMIN` — à vérifier côté infra. `hasAuthority('SUPER_ADMIN')` est plus explicite et correspond au claim Keycloak réel (`SUPER_ADMIN` sans préfixe `ROLE_`).

### D2 — Niveau de l'annotation : classe vs méthode

**Décision** : annotation au **niveau classe** pour les 3 controllers (toutes les méthodes requièrent `SUPER_ADMIN`). Conforme à DOMAIN_DRAW.md §4 « `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` ».

### D3 — ArchUnit : scope de la règle

**Décision** : la règle couvre tout `@RestController` dont `@RequestMapping` contient un path commençant par `/admin/`, `/platform/`, ou `/_sdr/`. Elle vérifie qu'au moins UNE méthode ou la classe porte `@PreAuthorize`.

Alternative rejetée : exiger `@PreAuthorize` uniquement au niveau classe — trop rigide, certains controllers mixtes (public + admin) pourraient légitimement avoir l'annotation par méthode.

Exception explicite : si un endpoint doit être public dans un controller admin, il DOIT porter `@PreAuthorize("permitAll()")` (whitelist explicite, pas de bypass silencieux).

### D4 — Module de test ArchUnit

**Décision** : placer `SecurityArchTest.java` dans `src/test/java/.../arch/` à côté des autres arch tests existants (si le package existe) ou le créer. Utiliser `archunit-junit5` (déjà présent dans de nombreux projets Spring Boot — à vérifier dans le POM).

## Risks / Trade-offs

- **[Risque] Régression d'intégration** : réactiver `@PreAuthorize` peut casser des appels d'intégration qui tournaient sans token (ex : cronjobs, appels internes). → Mitigation : vérifier les tests d'intégration existants ; les appels scheduler passent par `CommandBus` directement, pas via HTTP.
- **[Risque] `hasAuthority` vs `hasRole` discordance** : si Keycloak émet `ROLE_SUPER_ADMIN` au lieu de `SUPER_ADMIN`, `hasAuthority('SUPER_ADMIN')` rejettera les tokens valides. → Mitigation : vérifier la config Keycloak realm (`CLIENT_ROLES` vs `REALM_ROLES` mapper) avant de merger.
- **[Trade-off] ArchUnit scope large** : la règle ArchUnit peut générer des faux positifs si un controller `/platform/` légitime est créé sans avoir encore ses annotations. → Mitigation : le build échoue exprès — c'est le comportement voulu.

## Migration Plan

1. Ajouter `archunit-junit5` dans `pom.xml` si absent (scope `test`)
2. Réactiver `@PreAuthorize` sur les 3 controllers (supprimer les commentaires)
3. Ajouter import `org.springframework.security.access.prepost.PreAuthorize` si manquant
4. Créer `SecurityArchTest.java`
5. Créer les 3 `@WebMvcTest` de sécurité
6. Mettre à jour `web_api.md`
7. `./mvnw test -pl . -am -q` → build + tests doivent passer

Rollback : supprimer les annotations si bug critique en prod → mais documenter comme ADR.

## Open Questions

- Q1 : Keycloak émet-il `SUPER_ADMIN` ou `ROLE_SUPER_ADMIN` dans les claims ? (impact `hasAuthority` vs `hasRole`)
- Q2 : Faut-il sécuriser `AdminTchalaController` (core.haiti) dans le même change ? (actuellement hors scope — il a ses propres `@PreAuthorize` commentés)
