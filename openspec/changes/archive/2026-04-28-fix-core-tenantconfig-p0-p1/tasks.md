## 1. Nettoyage préalable — suppression code mort

- [x] 1.1 Supprimer `DeactivateTenantCommand.java` (`core/tenantconfig/application/command/model/`)
- [x] 1.2 Supprimer `DeactivateTenantCommandHandler.java` (`core/tenantconfig/application/command/handler/`)
- [x] 1.3 Supprimer le endpoint `POST /{id}/deactivate` dans `TenantAdminController.java`
- [x] 1.4 Supprimer `TenantCreatedEvent.java` (`core/tenantconfig/domain/event/`)
- [x] 1.5 Vérifier qu'aucun import ne référence les classes supprimées (grep + build)

## 2. Domain event — TenantIdentityUpdatedEvent

- [x] 2.1 Créer `core/tenantconfig/domain/event/TenantIdentityUpdatedEvent.java`
  - record avec : `EventId eventId`, `Instant occurredAt`, `TenantId tenantId`, `Set<String> changedFields`
  - implémenter `DomainEvent`
  - Javadoc : consommateurs attendus (cache invalidation, search index)

## 3. Handler — UpdateTenantIdentityCommandHandler

- [x] 3.1 Implémenter `UpdateTenantIdentityCommandHandler.handle(UpdateTenantIdentityCommand cmd)` :
  - `@UseCase`, `@RequiredArgsConstructor`, `@TchTx`
  - Injecter : `TenantCatalog`, `TenantConfigWriterPort`, `IdGenerator`, `DomainEventPublisher`, `Clock`
  - Charger via `tenantCatalog.findRegistryById(cmd.tenantId())` → `ProblemRest.notFound` si absent
  - Construire `TenantConfig.fromRegistryView(registry)`
  - Appliquer conditionnellement : `rename(name, now)` si name non-null, `updateLocale(zone, currency, now)` si timezone ou currency non-null
  - `writer.update(tenant)`
  - Collecter `changedFields` et publier `TenantIdentityUpdatedEvent` after-commit
- [x] 3.2 Vérifier que le handler est `VoidCommandHandler<UpdateTenantIdentityCommand>` (retourne void)

## 4. Controller — TenantAdminIdentityController (allègement)

- [x] 4.1 Supprimer l'injection directe de `TenantCatalog`
- [x] 4.2 Injecter `QueryBus` si pas encore présent
- [x] 4.3 Remplacer les appels directs `tenantCatalog.findRegistryById(...)` par `queryBus.send(new GetTenantByIdQuery(tenantId))` → `TenantConfigView`
- [x] 4.4 Supprimer le guard `if (tenantId == null)` (contexte `@CurrentContext` garantit la valeur)
- [x] 4.5 Supprimer le parsing `ZoneId`/`Currency` du controller
  - La désérialisation JSON gère la validation via `UpdateTenantIdentityRequest` (champs `String` → conversion dans la command ou via `@JsonDeserialize`)
  - Si `UpdateTenantIdentityCommand` attend `ZoneId`/`Currency` typés, créer un `UpdateTenantIdentityRequest` avec champs `String` et construire la command dans le controller de façon minimale
- [x] 4.6 Adapter `updateIdentity` : `commandBus.send(cmd)` puis `queryBus.send(query)` pour la réponse
- [x] 4.7 Adapter `getIdentity` : utiliser `TenantConfigView` retourné par `GetTenantByIdQuery` → mapper vers `TenantIdentityView`

## 5. Handlers lifecycle — injection IdGenerator

- [x] 5.1 `ActivateTenantCommandHandler` : ajouter `private final IdGenerator idGenerator` + remplacer `UUID.randomUUID()` par `idGenerator.newUuid()`
- [x] 5.2 `SuspendTenantCommandHandler` : idem
- [x] 5.3 `ArchiveTenantCommandHandler` : idem

## 6. Mapper — corriger TenantMapper.toEntity()

- [x] 6.1 Supprimer `@Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")` de `toEntity()`
- [x] 6.2 Supprimer `@Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")` de `toEntity()`
- [x] 6.3 Vérifier que `TenantJpaEntity` a `@CreatedDate` et `@LastModifiedDate` sur les champs correspondants
- [x] 6.4 Vérifier que `@EntityListeners(AuditingEntityListener.class)` est présent sur `TenantJpaEntity`

## 7. CreateTenantCommandHandler — champs address et activate

- [x] 7.1 Implémenter `address` : si `cmd.address() != null` → créer adresse via `AddressWriterPort.create(cmd.address())` → récupérer `AddressId` → passer à `TenantConfig.createDraft(...)`
- [x] 7.2 Vérifier que `AddressWriterPort` existe et peut être injecté dans `tenantconfig` (dépendance autorisée core → core via port)
- [x] 7.3 Implémenter `activate` : si `cmd.activate() == Boolean.TRUE` → `tenant = tenant.activate(now)` avant `writer.create(tenant)`
- [x] 7.4 Publier `TenantStatusChangedEvent` si `activate=true` (status DRAFT→ACTIVE)

## 8. Tests unitaires

- [x] 8.1 `UpdateTenantIdentityCommandHandlerTest` — scénario : name seul
- [x] 8.2 `UpdateTenantIdentityCommandHandlerTest` — scénario : locale seule (timezone + currency)
- [x] 8.3 `UpdateTenantIdentityCommandHandlerTest` — scénario : name + locale
- [x] 8.4 `UpdateTenantIdentityCommandHandlerTest` — scénario : tenant not found → 404
- [ ] 8.5 `TenantAdminControllerTest` — vérifier que `POST /platform/tenants/{id}/deactivate` retourne 404

## 9. Vérification finale

- [x] 9.1 `./mvnw compile` → 0 erreur
- [ ] 9.2 `./mvnw test -pl tchalanet-server` → vert
- [ ] 9.3 `PUT /config/identity` testé manuellement → persiste le nom et la locale
- [x] 9.4 Aucun import mort dans les fichiers touchés (IntelliJ / mvnw compile)
