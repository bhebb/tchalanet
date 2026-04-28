## Status: DRAFT

## 1. Vérification préalable

- [ ] 1.1 Vérifier la config Jackson des typed IDs : chercher `@JsonValue` / `JsonSerializer` sur `UserId` et `TenantId` — confirmer que la sérialisation produit une string UUID et non un objet `{value: "..."}`
- [ ] 1.2 Inventorier les call sites de `ApproveUserCommand` (`grep -r "ApproveUserCommand" tchalanet-server/src/`)
- [ ] 1.3 Inventorier les consommateurs de `TenantContext.tenantId` et `UserDetails.tenantId`

## 2. Migration AppUser.approvedBy

- [ ] 2.1 Modifier `AppUser.approvedBy` : `UUID` → `@Nullable UserId`
- [ ] 2.2 Mettre à jour `UserMapper` : `entity.getApprovedBy()` → `UserId.of(entity.getApprovedBy())` (null-safe)
- [ ] 2.3 Vérifier que `AppUserJpaEntity.approvedBy` reste `UUID` brut (pas de modification)

## 3. Migration ApproveUserCommand.approvedBy

- [ ] 3.1 Modifier `ApproveUserCommand.approvedBy` : `UUID` → `UserId`
- [ ] 3.2 Adapter `ApproveUserCommandHandler` si nécessaire (appel à `.value()` pour persistance)
- [ ] 3.3 Adapter le web mapper qui construit `ApproveUserCommand` (conversion `UUID` → `UserId`)

## 4. Migration TenantContext et UserDetails

- [ ] 4.1 Modifier `TenantContext.tenantId` : `UUID` → `TenantId`
- [ ] 4.2 Modifier `UserDetails.tenantId` : `UUID` → `TenantId`
- [ ] 4.3 Adapter les handlers qui construisent ces records (`TenantId.of(uuid)`)
- [ ] 4.4 Adapter les consommateurs controllers/tests si nécessaire

## 5. Tests

- [ ] 5.1 Mettre à jour les tests unitaires des handlers impactés
- [ ] 5.2 Vérifier la sérialisation JSON via test `MockMvc` ou `@JsonTest` : `TenantId` et `UserId` sérialisés en string UUID

## 6. Vérification finale

- [ ] 6.1 `./mvnw clean verify -pl tchalanet-server` → build vert + tous tests
- [ ] 6.2 Mettre à jour CHANGELOG (`REFACTOR: typed IDs UserId/TenantId dans AppUser, ApproveUserCommand, TenantContext, UserDetails`)
