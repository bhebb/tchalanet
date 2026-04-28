## 1. Nettoyage préalable — suppression code mort

- [ ] 1.1 Supprimer `DeactivateTenantCommand.java` (`core/tenantconfig/application/command/model/`)
- [ ] 1.2 Supprimer `DeactivateTenantCommandHandler.java` (`core/tenantconfig/application/command/handler/`)
- [ ] 1.3 Supprimer le endpoint `POST /{id}/deactivate` dans `TenantAdminController.java`
- [ ] 1.4 Supprimer `TenantCreatedEvent.java` (`core/tenantconfig/domain/event/`)
- [ ] 1.5 Vérifier qu'aucun import ne référence les classes supprimées (grep + build)

## 2. Domain event — TenantIdentityUpdatedEvent

- [ ] 2.1 Créer `core/tenantconfig/domain/event/TenantIdentityUpdatedEvent.java`
  - record avec : `EventId eventId`, `Instant occurredAt`, `TenantId tenantId`, `Set<String> changedFields`
  - implémenter `DomainEvent`
  - Javadoc : consommateurs attendus (cache invalidation, search index)

## 3. Handler — UpdateTenantIdentityCommandHandler

- [ ] 3.1 Implémenter `UpdateTenantIdentityCommandHandler.handle(UpdateTenantIdentityCommand cmd)` :
  - `@UseCase`, `@RequiredArgsConstructor`, `@TchTx`
  - Injecter : `TenantCatalog`, `TenantConfigWriterPort`, `IdGenerator`, `DomainEventPublisher`, `Clock`
  - Charger via `tenantCatalog.findRegistryById(cmd.tenantId())` → `ProblemRest.notFound` si absent
  - Construire `TenantConfig.fromRegistryView(registry)`
  - Appliquer conditionnellement : `rename(name, now)` si name non-null, `updateLocale(zone, currency, now)` si timezone ou currency non-null
  - `writer.update(tenant)`
  - Collecter `changedFields` et publier `TenantIdentityUpdatedEvent` after-commit
- [ ] 3.2 Vérifier que le handler est `VoidCommandHandler<UpdateTenantIdentityCommand>` (retourne void)

## 4. Controller — TenantAdminIdentityController (allègement)

- [ ] 4.1 Supprimer l'injection directe de `TenantCatalog`
- [ ] 4.2 Injecter `QueryBus` si pas encore présent
- [ ] 4.3 Remplacer les appels directs `tenantCatalog.findRegistryById(...)` par `queryBus.send(new GetTenantByIdQuery(tenantId))` → `TenantConfigView`
- [ ] 4.4 Supprimer le guard `if (tenantId == null)` (contexte `@CurrentContext` garantit la valeur)
- [ ] 4.5 Supprimer le parsing `ZoneId`/`Currency` du controller
  - La désérialisation JSON gère la validation via `UpdateTenantIdentityRequest` (champs `String` → conversion dans la command ou via `@JsonDeserialize`)
  - Si `UpdateTenantIdentityCommand` attend `ZoneId`/`Currency` typés, créer un `UpdateTenantIdentityRequest` avec champs `String` et construire la command dans le controller de façon minimale
- [ ] 4.6 Adapter `updateIdentity` : `commandBus.send(cmd)` puis `queryBus.send(query)` pour la réponse
- [ ] 4.7 Adapter `getIdentity` : utiliser `TenantConfigView` retourné par `GetTenantByIdQuery` → mapper vers `TenantIdentityView`

## 5. Handlers lifecycle — injection IdGenerator

- [ ] 5.1 `ActivateTenantCommandHandler` : ajouter `private final IdGenerator idGenerator` + remplacer `UUID.randomUUID()` par `idGenerator.newUuid()`
- [ ] 5.2 `SuspendTenantCommandHandler` : idem
- [ ] 5.3 `ArchiveTenantCommandHandler` : idem

## 6. Mapper — corriger TenantMapper.toEntity()

- [ ] 6.1 Supprimer `@Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")` de `toEntity()`
- [ ] 6.2 Supprimer `@Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")` de `toEntity()`
- [ ] 6.3 Vérifier que `TenantJpaEntity` a `@CreatedDate` et `@LastModifiedDate` sur les champs correspondants
- [ ] 6.4 Vérifier que `@EntityListeners(AuditingEntityListener.class)` est présent sur `TenantJpaEntity`

## 7. CreateTenantCommandHandler — champs address et activate

- [ ] 7.1 Implémenter `address` : si `cmd.address() != null` → créer adresse via `AddressWriterPort.create(cmd.address())` → récupérer `AddressId` → passer à `TenantConfig.createDraft(...)`
- [ ] 7.2 Vérifier que `AddressWriterPort` existe et peut être injecté dans `tenantconfig` (dépendance autorisée core → core via port)
- [ ] 7.3 Implémenter `activate` : si `cmd.activate() == Boolean.TRUE` → `tenant = tenant.activate(now)` avant `writer.create(tenant)`
- [ ] 7.4 Publier `TenantStatusChangedEvent` si `activate=true` (status DRAFT→ACTIVE)

## 8. Tests unitaires

- [ ] 8.1 `UpdateTenantIdentityCommandHandlerTest` — scénario : name seul
- [ ] 8.2 `UpdateTenantIdentityCommandHandlerTest` — scénario : locale seule (timezone + currency)
- [ ] 8.3 `UpdateTenantIdentityCommandHandlerTest` — scénario : name + locale
- [ ] 8.4 `UpdateTenantIdentityCommandHandlerTest` — scénario : tenant not found → 404
- [ ] 8.5 `TenantAdminControllerTest` — vérifier que `POST /platform/tenants/{id}/deactivate` retourne 404

## 9. Vérification finale

- [ ] 9.1 `./mvnw compile` → 0 erreur
- [ ] 9.2 `./mvnw test -pl tchalanet-server` → vert
- [ ] 9.3 `PUT /config/identity` testé manuellement → persiste le nom et la locale
- [ ] 9.4 Aucun import mort dans les fichiers touchés (IntelliJ / mvnw compile)
