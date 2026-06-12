package com.tchalanet.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;

/**
 * ArchUnit gates for the platform layer migration.
 *
 * <p>Active rules enforce the dependency graph NOW.
 * Pending rules (annotated {@link Disabled}) must be enabled one by one as migrations complete.
 *
 * <p>Allowlist removal conditions are documented per test.
 */
class PlatformLayerGatesTest {

  private static JavaClasses allClasses;

  @BeforeAll
  static void setup() {
    allClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.tchalanet.server");
  }

  // ── Active gates ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Platform layer — dependency graph")
  class PlatformDependencyRules {

    @Test
    @DisplayName("platform must not depend on core")
    void platformMustNotDependOnCore() {
      noClasses()
          .that().resideInAPackage("com.tchalanet.server.platform..")
          .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.core..")
          .as("platform must not depend on core (dependency graph: platform -> common, catalog)")
          .check(allClasses);
    }

    @Test
    @DisplayName("platform must not depend on features")
    void platformMustNotDependOnFeatures() {
      noClasses()
          .that().resideInAPackage("com.tchalanet.server.platform..")
          .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.features..")
          .as("platform must not depend on features (features are leaves)")
          .check(allClasses);
    }

    @Test
    @DisplayName("platform internal packages must not be imported by other modules")
    void platformInternalMustNotBeImportedByOtherModules() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform..")
          .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.platform..internal..")
          .as("no module may import platform.<capability>.internal; only platform.<capability>.api is public")
          .check(allClasses);
    }

    @Test
    @DisplayName("communication provider adapters are internal only")
    void communicationProviderAdaptersAreInternalOnly() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform.communication.internal..")
          .should().dependOnClassesThat()
          .resideInAnyPackage(
              "com.tchalanet.server.platform.communication.internal.adapter..",
              "com.tchalanet.server.platform.communication.internal.provider..")
          .as("Slack/email/SMS providers are implementation details of platform.communication")
          .check(allClasses);
    }

    @Test
    @DisplayName("identity provider adapters are internal only")
    void identityProviderAdaptersAreInternalOnly() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform.identity..")
          .should().dependOnClassesThat()
          .resideInAnyPackage(
              "com.tchalanet.server.platform.identity.internal.firebase..",
              "com.tchalanet.server.platform.identity.internal.keycloak..",
              "com.tchalanet.server.platform.identity.internal.local..",
              "com.tchalanet.server.platform.identity.internal.clerk..")
          .as("identity provider adapters are private to platform.identity")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    @Test
    @DisplayName("provider SDKs are confined to their identity adapters")
    void providerSdksAreConfinedToIdentityAdapters() {
      noClasses()
          .that()
          .resideOutsideOfPackages(
              "com.tchalanet.server.platform.identity.internal.keycloak..",
              // Transitional allowlist: move legacy admin/provisioning adapters to internal.keycloak.
              "com.tchalanet.server.platform.identity.internal.service.keycloak..")
          .should().dependOnClassesThat().resideInAPackage("org.keycloak..")
          .as("Keycloak SDK classes are private to the Keycloak identity adapter")
          .check(allClasses);

      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform.identity.internal.firebase..")
          .should().dependOnClassesThat().resideInAPackage("com.google.firebase..")
          .as("Firebase SDK classes are private to the Firebase identity adapter")
          .allowEmptyShould(true)
          .check(allClasses);

      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform.identity.internal.clerk..")
          .should().dependOnClassesThat().resideInAPackage("com.clerk..")
          .as("Clerk SDK classes are private to a future Clerk identity adapter")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    @Test
    @DisplayName("controllers and handlers must not parse JWTs directly")
    void controllersAndHandlersMustNotParseJwtsDirectly() {
      noClasses()
          .that().haveSimpleNameEndingWith("Controller")
          .or().haveSimpleNameEndingWith("Handler")
          .should().dependOnClassesThat().resideInAPackage("org.springframework.security.oauth2.jwt..")
          .as("controllers and handlers use provider-neutral identity/context APIs")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    @Test
    @DisplayName("notification internals are private to platform.notification")
    void notificationInternalsArePrivateToPlatformNotification() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform.notification..")
          .should().dependOnClassesThat()
          .resideInAnyPackage(
              "com.tchalanet.server.platform.notification.internal..",
              "com.tchalanet.server.platform.notification.internal.persistence..")
          .as("notification persistence and internal services are owned by platform.notification")
          .check(allClasses);
    }

    @Test
    @DisplayName("notification must not call communication")
    void notificationMustNotCallCommunication() {
      noClasses()
          .that().resideInAPackage("com.tchalanet.server.platform.notification..")
          .should().dependOnClassesThat()
          .resideInAPackage("com.tchalanet.server.platform.communication..")
          .as("platform.notification creates in-app records; external delivery belongs to platform.communication")
          .check(allClasses);
    }

    @Test
    @DisplayName("accesscontrol internals are private to platform.accesscontrol")
    void accessControlInternalsArePrivateToAccessControl() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform.accesscontrol..")
          .should().dependOnClassesThat()
          .resideInAPackage("com.tchalanet.server.platform.accesscontrol.internal..")
          .as("accesscontrol internals are private; other modules use platform.accesscontrol.api")
          .check(allClasses);
    }

    @Test
    @DisplayName("audit internals are private to platform.audit")
    void auditInternalsArePrivateToAudit() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform.audit..")
          .should().dependOnClassesThat()
          .resideInAPackage("com.tchalanet.server.platform.audit.internal..")
          .as("audit internals are private; other modules use platform.audit.api")
          .check(allClasses);
    }

    @Test
    @DisplayName("idempotence internals are private to platform.idempotence")
    void idempotenceInternalsArePrivateToIdempotence() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform.idempotence..")
          .should().dependOnClassesThat()
          .resideInAPackage("com.tchalanet.server.platform.idempotence.internal..")
          .as("idempotence internals are private; other modules use platform.idempotence.api")
          .check(allClasses);
    }

    @Test
    @DisplayName("archive internals are private to platform.archive")
    void archiveInternalsArePrivateToArchive() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.platform.archive..")
          .should().dependOnClassesThat()
          .resideInAPackage("com.tchalanet.server.platform.archive.internal..")
          .as("archive internals are private; other modules use platform.archive.api")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    @Test
    @DisplayName("platform.archive must not depend on core internals")
    void archiveMustNotDependOnCoreInternals() {
      noClasses()
          .that().resideInAPackage("com.tchalanet.server.platform.archive..")
          .should().dependOnClassesThat()
          .resideInAPackage("com.tchalanet.server.core..internal..")
          .as("platform.archive must not import core.<domain>.internal; use ArchiveDatasetProvider interface instead")
          .allowEmptyShould(true)
          .check(allClasses);
    }
  }

  @Nested
  @DisplayName("Catalog layer — dependency graph")
  class CatalogDependencyRules {

    @Test
    @DisplayName("catalog must not depend on platform")
    void catalogMustNotDependOnPlatform() {
      noClasses()
          .that().resideInAPackage("com.tchalanet.server.catalog..")
          .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.platform..")
          .as("catalog must not depend on platform (catalog -> common only)")
          .check(allClasses);
    }
  }

  @Nested
  @DisplayName("Common layer — dependency graph")
  class CommonDependencyRules {

    @Test
    @DisplayName("common must not depend on platform")
    void commonMustNotDependOnPlatform() {
      noClasses()
          .that().resideInAPackage("com.tchalanet.server.common..")
          .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.platform..")
          .as("common must not depend on platform (common depends on nothing)")
          .check(allClasses);
    }

    @Test
    @DisplayName("common.persistence must not contain platform cross-cutting persistence")
    void commonPersistenceMustNotContainPlatformCrossCuttingPersistence() {
      var misplaced =
          allClasses.stream()
              .filter(javaClass -> javaClass.getPackageName().contains(".common.persistence"))
              .filter(
                  javaClass ->
                      !java.util.Set.of("AuditableEntity", "BaseEntity", "BaseTenantEntity")
                          .contains(javaClass.getSimpleName()))
              .filter(
                  javaClass -> {
                    var name = javaClass.getSimpleName().toLowerCase();
                    return name.contains("audit")
                        || name.contains("permission")
                        || name.contains("role")
                        || name.contains("idempot")
                        || name.contains("processedevent");
                  })
              .map(javaClass -> javaClass.getName())
              .sorted()
              .toList();

      assertThat(misplaced)
          .as("functional audit/accesscontrol/idempotence persistence belongs to platform, not common.persistence")
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("Core layer — platform event boundary")
  class CorePlatformEventRules {

    @Test
    @DisplayName("core must not listen to platform events")
    void coreMustNotListenToPlatformEvents() {
      noClasses()
          .that().resideInAPackage("com.tchalanet.server.core..")
          .and().areAnnotatedWith(EventListener.class)
          .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.platform..")
          .as("core modules must not @EventListener platform events; only core events are consumed by core")
          .allowEmptyShould(true)
          .check(allClasses);
    }
  }

  @Nested
  @DisplayName("Features are leaf modules")
  class FeaturesLeafRules {

    @Test
    @DisplayName("no module may import features packages")
    void featuresAreLeaves() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.features..")
          .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.features..")
          .as("features are leaf modules — no other module may depend on features")
          .check(allClasses);
    }
  }

  @Nested
  @DisplayName("No cross-module internal imports")
  class InternalBoundaryRules {

    @Test
    @DisplayName("core internal packages must not be imported by other modules")
    void coreInternalMustNotBeImportedByOtherModules() {
      noClasses()
          .that().resideOutsideOfPackage("com.tchalanet.server.core..")
          .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.core..internal..")
          .as("no module may import core.<domain>.internal; only core.<domain>.api is public")
          .check(allClasses);
    }
  }

  // ── Final migration gates ────────────────────────────────────────────────

  @Nested
  @DisplayName("Pending: legacy core modules must not exist after migration")
  class LegacyMigrationPendingGates {

    @Test
    @DisplayName("platform.usercontext must not exist")
    void platformUserContextMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.platform.usercontext..")
          .as("platform.usercontext must not exist — use platform.identity")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Removal condition: no Java class under com.tchalanet.server.core.audit remains.
     */
    @Test
    @DisplayName("core.audit must not exist")
    void coreAuditMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.audit..")
          .as("core.audit must not exist — migrate to platform.audit")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Removal condition: no Java class under com.tchalanet.server.core.accesscontrol remains.
     */
    @Test
    @DisplayName("core.accesscontrol must not exist")
    void coreAccessControlMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.accesscontrol..")
          .as("core.accesscontrol must not exist — migrate to platform.accesscontrol")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Removal condition: no Java class under com.tchalanet.server.core.tenantuser remains.
     */
    @Test
    @DisplayName("core.tenantuser must not exist")
    void coreTenantUserMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.tenantuser..")
          .as("core.tenantuser must not exist — migrate to platform.identity")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Removal condition: no Java class under com.tchalanet.server.core.tenantconfig remains.
     */
    @Test
    @DisplayName("core.tenantconfig must not exist")
    void coreTenantConfigMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.tenantconfig..")
          .as("core.tenantconfig must not exist — migrate to platform.tenantconfig")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Removal condition: no Java class under com.tchalanet.server.core.tenanttheme remains.
     */
    @Test
    @DisplayName("core.tenanttheme must not exist")
    void coreTenantThemeMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.tenanttheme..")
          .as("core.tenanttheme must not exist — migrate to platform.tenanttheme")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Removal condition: no Java class under com.tchalanet.server.core.notification remains,
     * OR an ADR explicitly justifies keeping it in core.
     */
    @Test
    @DisplayName("core.notification must not exist")
    void coreNotificationMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.notification..")
          .as("core.notification must not exist — migrate to platform.notification or document ADR exception")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Migrated: core.address → platform.address.
     * Removal condition: no Java class under com.tchalanet.server.core.address remains.
     */
    @Test
    @DisplayName("core.address must not exist")
    void coreAddressMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.address..")
          .as("core.address must not exist — migrate to platform.address")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Migrated: core.external → platform.external.
     * Removal condition: no Java class under com.tchalanet.server.core.external remains.
     */
    @Test
    @DisplayName("core.external must not exist")
    void coreExternalMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.external..")
          .as("core.external must not exist — migrate to platform.external")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Migrated: core.featureflags → platform.featureflags.
     * Removal condition: no Java class under com.tchalanet.server.core.featureflags remains.
     */
    @Test
    @DisplayName("core.featureflags must not exist")
    void coreFeatureFlagsMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.featureflags..")
          .as("core.featureflags must not exist — migrate to platform.featureflags")
          .allowEmptyShould(true)
          .check(allClasses);
    }

    /**
     * Migrated: core.tenantgame → platform.tenantgame.
     * Removal condition: no Java class under com.tchalanet.server.core.tenantgame remains.
     */
    @Test
    @DisplayName("core.tenantgame must not exist")
    void coreTenantGameMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.tenantgame..")
          .as("core.tenantgame must not exist — migrate to platform.tenantgame")
          .allowEmptyShould(true)
          .check(allClasses);
    }
  }
}
