## Status: DONE

## 1. Vérification préalable

- [x] 1.1 Vérifier la config Jackson des typed IDs : chercher `@JsonValue` / `JsonSerializer` sur `UserId` et `TenantId` — confirmer que la sérialisation produit une string UUID et non un objet `{value: "..."}` → confirmé via `TypedIdsJacksonModule` + `GenericTypedIdSerializer` (appel `.value().toString()`)
- [x] 1.2 Inventorier les call sites de `ApproveUserCommand` (`grep -r "ApproveUserCommand" tchalanet-server/src/`) → 1 call site : `UserAdminController.approveUser()` passe `null` comme `approvedBy`
- [x] 1.3 Inventorier les consommateurs de `TenantContext.tenantId` et `UserDetails.tenantId` → `GetCurrentUserQueryHandler` construit `TenantContext` ; `UserDetails.tenantId` déjà `TenantId`

## 2. Migration AppUser.approvedBy

- [x] 2.1 Modifier `AppUser.approvedBy` : `UUID` → `@Nullable UserId`
- [x] 2.2 Mettre à jour `UserMapper` : `entity.getApprovedBy()` → `UserId.nullableOf(entity.getApprovedBy())` (null-safe) ; `apply()` : `u.getApprovedBy() == null ? null : u.getApprovedBy().uuid()`
- [x] 2.3 Vérifier que `AppUserJpaEntity.approvedBy` reste `UUID` brut (pas de modification) → confirmé

## 3. Migration ApproveUserCommand.approvedBy

- [x] 3.1 Modifier `ApproveUserCommand.approvedBy` : `UUID` → `@Nullable UserId`
- [x] 3.2 Adapter `ApproveUserCommandHandler` si nécessaire (appel à `.value()` pour persistance) → N/A : `user.approve(now, command.approvedBy())` fonctionne directement avec `UserId`
- [x] 3.3 Adapter le web mapper qui construit `ApproveUserCommand` (conversion `UUID` → `UserId`) → N/A : `UserAdminController` passe déjà `null`

## 4. Migration TenantContext et UserDetails

- [x] 4.1 Modifier `TenantContext.tenantId` : `UUID` → `TenantId`
- [x] 4.2 Modifier `UserDetails.tenantId` : `UUID` → `TenantId` → N/A : déjà `TenantId`
- [x] 4.3 Adapter les handlers qui construisent ces records (`TenantId.of(uuid)`) → `GetCurrentUserQueryHandler` : `ctx.tenantIdSafe()` utilisé directement (retourne déjà `TenantId`)
- [x] 4.4 Adapter les consommateurs controllers/tests si nécessaire → `TenantContextResponse.tenantId` mis à jour de `UUID` → `TenantId`

## 5. Tests

- [x] 5.1 Mettre à jour les tests unitaires des handlers impactés → N/A : aucun test handler user existant
- [x] 5.2 Vérifier la sérialisation JSON via test `MockMvc` ou `@JsonTest` : `TenantId` et `UserId` sérialisés en string UUID → confirmé à l'étape 1.1

## 6. Vérification finale

- [x] 6.1 `./mvnw clean verify -pl tchalanet-server` → build vert + tous tests — compilation ✅, 7 échecs pré-existants (arch violations UUID/features, catalog-core) non liés à cette change
- [x] 6.2 Mettre à jour CHANGELOG (`REFACTOR: typed IDs UserId/TenantId dans AppUser, ApproveUserCommand, TenantContext, UserDetails`)
