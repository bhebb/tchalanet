# Current inventory from uploaded `tchalanet-common.zip`

## Source files

The uploaded module contains 337 files under `src/main/java`, including Java files and package/docs.

## Top-level package observations

| Package | Observation | Initial decision |
|---|---|---|
| `common.batch` | Spring Batch gates, launch, notification/runtime services | Move to app/job or platform.job |
| `common.bus` | CommandBus/QueryBus contracts and simple implementation | Keep |
| `common.cache` | Mix of cache contracts and runtime managers/config | Split |
| `common.client.http` | WebClient/RestClient runtime config | Move |
| `common.constant` | Technical and security constants mixed | Review/split |
| `common.context` | Request context, HTTP filter, operational context hints | Keep partial |
| `common.event` | DomainEvent/Publisher + Spring adapter | Keep |
| `common.job` | New generic job language plus validator | Keep partial |
| `common.json` | Typed ID Jackson support and converters | Keep partial/review duplicates |
| `common.mapper` | CommonIdMapper | Keep |
| `common.persistence` | Base entities, datasource config, RLS, converters | Split |
| `common.security` | ApiScope/auth context plus permissions | Split |
| `common.selection` | Game/bet selection canonicalizer | Move |
| `common.stereotype` | UseCase/TchTx annotations | Keep |
| `common.time` | Time/date helpers | Keep |
| `common.tx` | AfterCommit | Keep |
| `common.types` | IDs, money/time primitives, many business enums | Keep IDs/primitives, move business enums |
| `common.util` | Hashing/JSON/schema/role utilities | Split |
| `common.web` | Web response/paging/error/converters + Data REST config | Keep partial; move Data REST config |

## Heavy dependencies observed in POM

These should be removed from `tchalanet-common` after moving offending classes:

```text
spring-batch-core
shedlock-core
shedlock-spring
shedlock-provider-jdbc-template
spring-webflux
spring-data-redis
lettuce-core
spring-data-rest-core
spring-data-rest-webmvc
spring-boot-starter-data-jpa
querydsl-jpa
querydsl-apt
spring-security-oauth2-jose
json-schema-validator
```

## Business enums observed under `common.types.enums`

Examples that should be moved to owning modules:

```text
ApprovalRole
AuditAction
AuditActorType
AuditEntityType
AutonomyLevel
AutonomyTargetType
BetType
BreachOutcome
DrawSource
GameCode
IdempotencyScope
NotificationChannel
NotificationType
OperationType
ResultQuality
RuleKey
SaleOrigin
ScopeType
TargetType
TenantStatus
TenantType
TenantUserStatus
ThemeMode
TicketResultStatus
TicketSaleStatus
TicketSettlementStatus
TicketSyncStatus
UsLotteryProvider
UserStatus
```

`TchRole` needs a deliberate decision. It may remain temporarily if required by `TchRequestContext`, but long-term ownership is likely `platform.identity` or `platform.accesscontrol`.

## Duplicate converter observation

Typed ID converters exist in both:

```text
common.json.converter
common.web.converter
```

Consolidate to one location. Prefer:

```text
common.web.converter.StringToXxxIdConverter
```

Jackson typed ID serialization should stay under `common.json`.

## Full uploaded source listing

```text
com/tchalanet/server/common/COMMON_GUIDELINES.md
com/tchalanet/server/common/README.md
com/tchalanet/server/common/batch/gate/BatchCacheConfig.java
com/tchalanet/server/common/batch/gate/BatchDisabledException.java
com/tchalanet/server/common/batch/gate/BatchFlagCache.java
com/tchalanet/server/common/batch/gate/BatchGate.java
com/tchalanet/server/common/batch/gate/BatchGateCache.java
com/tchalanet/server/common/batch/gate/BatchGateCacheImpl.java
com/tchalanet/server/common/batch/gate/BatchGateFlagStore.java
com/tchalanet/server/common/batch/gate/BatchGateResolver.java
com/tchalanet/server/common/batch/gate/package-info.java
com/tchalanet/server/common/batch/launch/BatchJobStarter.java
com/tchalanet/server/common/batch/launch/package-info.java
com/tchalanet/server/common/batch/service/BatchEventNotificationService.java
com/tchalanet/server/common/batch/service/BatchNotification.java
com/tchalanet/server/common/batch/service/BatchNotificationCacheSpecProvider.java
com/tchalanet/server/common/batch/service/BatchNotificationPolicy.java
com/tchalanet/server/common/batch/service/BatchNotificationStatus.java
com/tchalanet/server/common/batch/service/package-info.java
com/tchalanet/server/common/bus/BusRegistrationException.java
com/tchalanet/server/common/bus/Command.java
com/tchalanet/server/common/bus/CommandBus.java
com/tchalanet/server/common/bus/CommandHandler.java
com/tchalanet/server/common/bus/DuplicateHandlerException.java
com/tchalanet/server/common/bus/HandlerRegistry.java
com/tchalanet/server/common/bus/HandlerTypeResolver.java
com/tchalanet/server/common/bus/InvalidHandlerException.java
com/tchalanet/server/common/bus/NoHandlerException.java
com/tchalanet/server/common/bus/Query.java
com/tchalanet/server/common/bus/QueryBus.java
com/tchalanet/server/common/bus/QueryHandler.java
com/tchalanet/server/common/bus/SimpleCommandBus.java
com/tchalanet/server/common/bus/SimpleQueryBus.java
com/tchalanet/server/common/bus/VoidCommandHandler.java
com/tchalanet/server/common/bus/package-info.java
com/tchalanet/server/common/cache/CACHE.md
com/tchalanet/server/common/cache/CacheConfig.java
com/tchalanet/server/common/cache/CacheKeyBuilder.java
com/tchalanet/server/common/cache/CacheSpec.java
com/tchalanet/server/common/cache/CacheSpecAwareCaffeineCacheManager.java
com/tchalanet/server/common/cache/CacheSpecProvider.java
com/tchalanet/server/common/cache/CombinedCache.java
com/tchalanet/server/common/cache/CombinedCacheManager.java
com/tchalanet/server/common/cache/RedisConfig.java
com/tchalanet/server/common/cache/TchCacheProperties.java
com/tchalanet/server/common/cache/package-info.java
com/tchalanet/server/common/client/http/HttpClientConfig.java
com/tchalanet/server/common/client/http/HttpClientProperties.java
com/tchalanet/server/common/client/http/RestClientFactory.java
com/tchalanet/server/common/client/http/package-info.java
com/tchalanet/server/common/constant/CommonConstants.java
com/tchalanet/server/common/constant/ContextKeys.java
com/tchalanet/server/common/constant/SecurityClaims.java
com/tchalanet/server/common/constant/TchHeaders.java
com/tchalanet/server/common/constant/package-info.java
com/tchalanet/server/common/context/ActorContextResolver.java
com/tchalanet/server/common/context/AuthContextExtractor.java
com/tchalanet/server/common/context/OperationalContextSource.java
com/tchalanet/server/common/context/OperationalRequestContext.java
com/tchalanet/server/common/context/SpringContextHolder.java
com/tchalanet/server/common/context/TchContext.java
com/tchalanet/server/common/context/TchContextBinder.java
com/tchalanet/server/common/context/TchContextProperties.java
com/tchalanet/server/common/context/TchContextResolver.java
com/tchalanet/server/common/context/TchContextScope.java
com/tchalanet/server/common/context/TchRequestContext.java
com/tchalanet/server/common/context/TenantContextInfo.java
com/tchalanet/server/common/context/TrustLevel.java
com/tchalanet/server/common/context/operational/AdminOperationalContext.java
com/tchalanet/server/common/context/operational/AdminPosSelection.java
com/tchalanet/server/common/context/operational/AdminPosSelectionLookup.java
com/tchalanet/server/common/context/operational/MissingOperationalContextException.java
com/tchalanet/server/common/context/operational/NoopAdminPosSelectionLookup.java
com/tchalanet/server/common/context/operational/OperationalContextHeaders.java
com/tchalanet/server/common/context/operational/OperationalContextHint.java
com/tchalanet/server/common/context/operational/OperationalContextRole.java
com/tchalanet/server/common/context/operational/OperationalContextSource.java
com/tchalanet/server/common/context/operational/OperationalRequestContext.java
com/tchalanet/server/common/context/operational/PosOperationalContext.java
com/tchalanet/server/common/context/operational/SellerOperationalContext.java
com/tchalanet/server/common/context/operational/SuperAdminOperationalContext.java
com/tchalanet/server/common/context/operational/TrustLevel.java
com/tchalanet/server/common/context/operational/UntrustedOperationalContextException.java
com/tchalanet/server/common/context/package-info.java
com/tchalanet/server/common/context/system/SystemContextProperties.java
com/tchalanet/server/common/context/system/package-info.java
com/tchalanet/server/common/context/tenant/TenantContextLookup.java
com/tchalanet/server/common/context/tenant/TenantContextResolver.java
com/tchalanet/server/common/context/tenant/package-info.java
com/tchalanet/server/common/context/web/ApiScopeResolver.java
com/tchalanet/server/common/context/web/CurrentContext.java
com/tchalanet/server/common/context/web/CurrentContextArgumentResolver.java
com/tchalanet/server/common/context/web/CurrentContextWebMvcConfig.java
com/tchalanet/server/common/context/web/OperationalContextHeaderParser.java
com/tchalanet/server/common/context/web/TchContextFilter.java
com/tchalanet/server/common/context/web/TchRequestContextFactory.java
com/tchalanet/server/common/event/DomainEvent.java
com/tchalanet/server/common/event/DomainEventPublisher.java
com/tchalanet/server/common/event/infra/spring/LoggingDomainEventListener.java
com/tchalanet/server/common/event/infra/spring/SpringDomainEventPublisher.java
com/tchalanet/server/common/event/infra/spring/package-info.java
com/tchalanet/server/common/event/package-info.java
com/tchalanet/server/common/job/annotation/TchJob.java
com/tchalanet/server/common/job/annotation/package-info.java
com/tchalanet/server/common/job/exception/JobContextClearException.java
com/tchalanet/server/common/job/exception/JobPartialFailureException.java
com/tchalanet/server/common/job/exception/JobSkippedException.java
com/tchalanet/server/common/job/exception/package-info.java
com/tchalanet/server/common/job/key/BatchJobKeys.java
com/tchalanet/server/common/job/key/JobKey.java
com/tchalanet/server/common/job/key/package-info.java
com/tchalanet/server/common/job/params/JobParamKeys.java
com/tchalanet/server/common/job/params/JobParamReader.java
com/tchalanet/server/common/job/params/JobParamsValidator.java
com/tchalanet/server/common/job/params/package-info.java
com/tchalanet/server/common/json/GenericTypedIdDeserializer.java
com/tchalanet/server/common/json/GenericTypedIdSerializer.java
com/tchalanet/server/common/json/JacksonConfig.java
com/tchalanet/server/common/json/TypedIdsJacksonModule.java
com/tchalanet/server/common/json/config/ObjectMapperHolder.java
com/tchalanet/server/common/json/converter/StringToAddressIdConverter.java
com/tchalanet/server/common/json/converter/StringToDrawChannelGameIdConverter.java
com/tchalanet/server/common/json/converter/StringToDrawChannelIdConverter.java
com/tchalanet/server/common/json/converter/StringToDrawIdConverter.java
com/tchalanet/server/common/json/converter/StringToDrawResultIdConverter.java
com/tchalanet/server/common/json/converter/StringToGameIdConverter.java
com/tchalanet/server/common/json/converter/StringToI18nOverrideIdConverter.java
com/tchalanet/server/common/json/converter/StringToLimitAssignmentIdConverter.java
com/tchalanet/server/common/json/converter/StringToNotificationIdConverter.java
com/tchalanet/server/common/json/converter/StringToOutletIdConverter.java
com/tchalanet/server/common/json/converter/StringToPageModelIdConverter.java
com/tchalanet/server/common/json/converter/StringToPageModelTemplateIdConverter.java
com/tchalanet/server/common/json/converter/StringToPayoutIdConverter.java
com/tchalanet/server/common/json/converter/StringToPlanIdConverter.java
com/tchalanet/server/common/json/converter/StringToPricingOddsIdConverter.java
com/tchalanet/server/common/json/converter/StringToResultSlotIdConverter.java
com/tchalanet/server/common/json/converter/StringToRoleIdConverter.java
com/tchalanet/server/common/json/converter/StringToSessionIdConverter.java
com/tchalanet/server/common/json/converter/StringToSettingIdConverter.java
com/tchalanet/server/common/json/converter/StringToTchalaEntryIdConverter.java
com/tchalanet/server/common/json/converter/StringToTenantGameIdConverter.java
com/tchalanet/server/common/json/converter/StringToTenantIdConverter.java
com/tchalanet/server/common/json/converter/StringToTerminalIdConverter.java
com/tchalanet/server/common/json/converter/StringToThemePresetIdConverter.java
com/tchalanet/server/common/json/converter/StringToTicketIdConverter.java
com/tchalanet/server/common/json/converter/StringToUserIdConverter.java
com/tchalanet/server/common/json/converter/package-info.java
com/tchalanet/server/common/json/mapper/package-info.java
com/tchalanet/server/common/json/package-info.java
com/tchalanet/server/common/mapper/CommonIdMapper.java
com/tchalanet/server/common/mapper/package-info.java
com/tchalanet/server/common/persistence/AuditableEntity.java
com/tchalanet/server/common/persistence/BaseEntity.java
com/tchalanet/server/common/persistence/BaseTenantEntity.java
com/tchalanet/server/common/persistence/audit/TenantEntityListener.java
com/tchalanet/server/common/persistence/audit/package-info.java
com/tchalanet/server/common/persistence/config/DataSourceConfig.java
com/tchalanet/server/common/persistence/config/PersistenceConfig.java
com/tchalanet/server/common/persistence/converter/CurrencyAttributeConverter.java
com/tchalanet/server/common/persistence/converter/ListToJsonConverter.java
com/tchalanet/server/common/persistence/converter/LocaleAttributeConverter.java
com/tchalanet/server/common/persistence/converter/MapStringToJsonConverter.java
com/tchalanet/server/common/persistence/converter/MapToJsonConverter.java
com/tchalanet/server/common/persistence/converter/ZoneIdAttributeConverter.java
com/tchalanet/server/common/persistence/converter/package-info.java
com/tchalanet/server/common/persistence/package-info.java
com/tchalanet/server/common/persistence/rls/ResetOnCloseConnection.java
com/tchalanet/server/common/persistence/rls/RlsAwareDataSource.java
com/tchalanet/server/common/persistence/rls/package-info.java
com/tchalanet/server/common/security/ApiScope.java
com/tchalanet/server/common/security/ExtractedAuthContext.java
com/tchalanet/server/common/security/Permissions.java
com/tchalanet/server/common/security/TchSecurityProperties.java
com/tchalanet/server/common/security/package-info.java
com/tchalanet/server/common/selection/SelectionKeyCanonicalizer.java
com/tchalanet/server/common/selection/package-info.java
com/tchalanet/server/common/stereotype/TchTx.java
com/tchalanet/server/common/stereotype/UseCase.java
com/tchalanet/server/common/stereotype/package-info.java
com/tchalanet/server/common/time/DateWindows.java
com/tchalanet/server/common/time/DaysOfWeekFormatter.java
com/tchalanet/server/common/time/DaysOfWeekParser.java
com/tchalanet/server/common/time/DefaultTimeZone.java
com/tchalanet/server/common/time/OccurredAtResolver.java
com/tchalanet/server/common/time/TchRuntimeProperties.java
com/tchalanet/server/common/time/TimeConfig.java
com/tchalanet/server/common/time/TimeProvider.java
com/tchalanet/server/common/time/package-info.java
com/tchalanet/server/common/tx/AfterCommit.java
com/tchalanet/server/common/tx/package-info.java
com/tchalanet/server/common/types/codes/LocaleCode.java
com/tchalanet/server/common/types/codes/SelectionKey.java
com/tchalanet/server/common/types/codes/package-info.java
com/tchalanet/server/common/types/enums/ApprovalRole.java
com/tchalanet/server/common/types/enums/AuditAction.java
com/tchalanet/server/common/types/enums/AuditActorType.java
com/tchalanet/server/common/types/enums/AuditEntityType.java
com/tchalanet/server/common/types/enums/AutonomyLevel.java
com/tchalanet/server/common/types/enums/AutonomyTargetType.java
com/tchalanet/server/common/types/enums/BetType.java
com/tchalanet/server/common/types/enums/BreachOutcome.java
com/tchalanet/server/common/types/enums/DrawSource.java
com/tchalanet/server/common/types/enums/GameCode.java
com/tchalanet/server/common/types/enums/IdempotencyScope.java
com/tchalanet/server/common/types/enums/NotificationChannel.java
com/tchalanet/server/common/types/enums/NotificationType.java
com/tchalanet/server/common/types/enums/OperationType.java
com/tchalanet/server/common/types/enums/ResultQuality.java
com/tchalanet/server/common/types/enums/RuleKey.java
com/tchalanet/server/common/types/enums/SaleOrigin.java
com/tchalanet/server/common/types/enums/ScopeType.java
com/tchalanet/server/common/types/enums/TargetType.java
com/tchalanet/server/common/types/enums/TchRole.java
com/tchalanet/server/common/types/enums/TenantStatus.java
com/tchalanet/server/common/types/enums/TenantType.java
com/tchalanet/server/common/types/enums/TenantUserStatus.java
com/tchalanet/server/common/types/enums/ThemeMode.java
com/tchalanet/server/common/types/enums/TicketResultStatus.java
com/tchalanet/server/common/types/enums/TicketSaleStatus.java
com/tchalanet/server/common/types/enums/TicketSettlementStatus.java
com/tchalanet/server/common/types/enums/TicketSyncStatus.java
com/tchalanet/server/common/types/enums/UpsertOutcome.java
com/tchalanet/server/common/types/enums/UsLotteryProvider.java
com/tchalanet/server/common/types/enums/UserStatus.java
com/tchalanet/server/common/types/enums/package-info.java
com/tchalanet/server/common/types/id/AddressId.java
com/tchalanet/server/common/types/id/ApprovalRequestId.java
com/tchalanet/server/common/types/id/AutonomyPolicyRuleId.java
com/tchalanet/server/common/types/id/DrawChannelGameId.java
com/tchalanet/server/common/types/id/DrawChannelId.java
com/tchalanet/server/common/types/id/DrawId.java
com/tchalanet/server/common/types/id/DrawResultId.java
com/tchalanet/server/common/types/id/EventId.java
com/tchalanet/server/common/types/id/GameId.java
com/tchalanet/server/common/types/id/I18nOverrideId.java
com/tchalanet/server/common/types/id/IdGenerator.java
com/tchalanet/server/common/types/id/KeycloakUserSub.java
com/tchalanet/server/common/types/id/LedgerEntryId.java
com/tchalanet/server/common/types/id/LimitAssignmentId.java
com/tchalanet/server/common/types/id/NotificationDeliveryId.java
com/tchalanet/server/common/types/id/NotificationId.java
com/tchalanet/server/common/types/id/NotificationPreferenceId.java
com/tchalanet/server/common/types/id/OfflineBatchId.java
com/tchalanet/server/common/types/id/OfflineCodeBatchId.java
com/tchalanet/server/common/types/id/OfflineCodeReservationId.java
com/tchalanet/server/common/types/id/OfflineSaleSubmissionId.java
com/tchalanet/server/common/types/id/OfflineSalesGrantId.java
com/tchalanet/server/common/types/id/OfflineTicketId.java
com/tchalanet/server/common/types/id/OutletId.java
com/tchalanet/server/common/types/id/PageModelId.java
com/tchalanet/server/common/types/id/PageModelTemplateId.java
com/tchalanet/server/common/types/id/PayoutId.java
com/tchalanet/server/common/types/id/PlanId.java
com/tchalanet/server/common/types/id/PricingOddsId.java
com/tchalanet/server/common/types/id/ResultSlotId.java
com/tchalanet/server/common/types/id/RoleId.java
com/tchalanet/server/common/types/id/SalesSessionId.java
com/tchalanet/server/common/types/id/SettingId.java
com/tchalanet/server/common/types/id/SubscriptionId.java
com/tchalanet/server/common/types/id/TchalaEntryId.java
com/tchalanet/server/common/types/id/TenantGameId.java
com/tchalanet/server/common/types/id/TenantId.java
com/tchalanet/server/common/types/id/TerminalId.java
com/tchalanet/server/common/types/id/ThemePresetId.java
com/tchalanet/server/common/types/id/TicketId.java
com/tchalanet/server/common/types/id/TypedIdRegistry.java
com/tchalanet/server/common/types/id/UserId.java
com/tchalanet/server/common/types/id/UuidV4Generator.java
com/tchalanet/server/common/types/id/package-info.java
com/tchalanet/server/common/types/money/CurrencyCode.java
com/tchalanet/server/common/types/money/Money.java
com/tchalanet/server/common/types/money/Percent.java
com/tchalanet/server/common/types/money/package-info.java
com/tchalanet/server/common/types/time/DateRange.java
com/tchalanet/server/common/types/time/TimeWindow.java
com/tchalanet/server/common/types/time/package-info.java
com/tchalanet/server/common/util/Hashing.java
com/tchalanet/server/common/util/JsonSchemaValidatorUtil.java
com/tchalanet/server/common/util/JsonUtils.java
com/tchalanet/server/common/util/JsonUtilsHolder.java
com/tchalanet/server/common/util/JsonbUtils.java
com/tchalanet/server/common/util/RoleUtils.java
com/tchalanet/server/common/util/package-info.java
com/tchalanet/server/common/web/advice/ApiResponseBodyAdvice.java
com/tchalanet/server/common/web/advice/ApiResponseConfig.java
com/tchalanet/server/common/web/advice/ApiResponseContext.java
com/tchalanet/server/common/web/advice/ApiResponseContextFilter.java
com/tchalanet/server/common/web/advice/package-info.java
com/tchalanet/server/common/web/api/ApiNotice.java
com/tchalanet/server/common/web/api/ApiResponse.java
com/tchalanet/server/common/web/api/ApiStatus.java
com/tchalanet/server/common/web/api/NoticeSeverity.java
com/tchalanet/server/common/web/api/ServiceHealth.java
com/tchalanet/server/common/web/api/ServiceStatus.java
com/tchalanet/server/common/web/api/package-info.java
com/tchalanet/server/common/web/config/DataRestConfig.java
com/tchalanet/server/common/web/converter/StringToAddressIdConverter.java
com/tchalanet/server/common/web/converter/StringToDrawChannelGameIdConverter.java
com/tchalanet/server/common/web/converter/StringToDrawChannelIdConverter.java
com/tchalanet/server/common/web/converter/StringToDrawIdConverter.java
com/tchalanet/server/common/web/converter/StringToDrawResultIdConverter.java
com/tchalanet/server/common/web/converter/StringToGameIdConverter.java
com/tchalanet/server/common/web/converter/StringToI18nOverrideIdConverter.java
com/tchalanet/server/common/web/converter/StringToLimitAssignmentIdConverter.java
com/tchalanet/server/common/web/converter/StringToNotificationIdConverter.java
com/tchalanet/server/common/web/converter/StringToOutletIdConverter.java
com/tchalanet/server/common/web/converter/StringToPageModelIdConverter.java
com/tchalanet/server/common/web/converter/StringToPageModelTemplateIdConverter.java
com/tchalanet/server/common/web/converter/StringToPayoutIdConverter.java
com/tchalanet/server/common/web/converter/StringToPlanIdConverter.java
com/tchalanet/server/common/web/converter/StringToPricingOddsIdConverter.java
com/tchalanet/server/common/web/converter/StringToResultSlotIdConverter.java
com/tchalanet/server/common/web/converter/StringToRoleIdConverter.java
com/tchalanet/server/common/web/converter/StringToSessionIdConverter.java
com/tchalanet/server/common/web/converter/StringToSettingIdConverter.java
com/tchalanet/server/common/web/converter/StringToTchalaEntryIdConverter.java
com/tchalanet/server/common/web/converter/StringToTenantGameIdConverter.java
com/tchalanet/server/common/web/converter/StringToTenantIdConverter.java
com/tchalanet/server/common/web/converter/StringToTerminalIdConverter.java
com/tchalanet/server/common/web/converter/StringToThemePresetIdConverter.java
com/tchalanet/server/common/web/converter/StringToTicketIdConverter.java
com/tchalanet/server/common/web/converter/StringToUserIdConverter.java
com/tchalanet/server/common/web/converter/package-info.java
com/tchalanet/server/common/web/error/GlobalErrorHandler.java
com/tchalanet/server/common/web/error/NotFoundException.java
com/tchalanet/server/common/web/error/PermissionsDeniedException.java
com/tchalanet/server/common/web/error/ProblemRest.java
com/tchalanet/server/common/web/error/ProblemRestException.java
com/tchalanet/server/common/web/error/TchGenericAppException.java
com/tchalanet/server/common/web/error/package-info.java
com/tchalanet/server/common/web/paging/TchPage.java
com/tchalanet/server/common/web/paging/TchPageMapper.java
com/tchalanet/server/common/web/paging/TchPageRequest.java
com/tchalanet/server/common/web/paging/TchPaging.java
com/tchalanet/server/common/web/paging/TchPagingArgumentResolver.java
com/tchalanet/server/common/web/paging/TchPagingWebConfig.java
com/tchalanet/server/common/web/paging/package-info.java

```
