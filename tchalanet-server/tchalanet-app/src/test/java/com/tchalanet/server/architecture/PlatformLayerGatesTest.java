package com.tchalanet.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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

  // ── Pending gates — enable ONE BY ONE as each migration completes ─────────

  @Nested
  @DisplayName("Pending: legacy core modules must not exist after migration")
  class LegacyMigrationPendingGates {

    /**
     * TODO: Enable after core.audit is fully migrated to platform.audit.
     * Removal condition: no Java class under com.tchalanet.server.core.audit remains.
     */
    @Test
    @Disabled("Allowlist: core.audit pending migration to platform.audit")
    @DisplayName("core.audit must not exist")
    void coreAuditMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.audit..")
          .as("core.audit must not exist — migrate to platform.audit")
          .check(allClasses);
    }

    /**
     * TODO: Enable after core.accesscontrol is fully migrated to platform.accesscontrol.
     * Removal condition: no Java class under com.tchalanet.server.core.accesscontrol remains.
     */
    @Test
    @Disabled("Allowlist: core.accesscontrol pending migration to platform.accesscontrol")
    @DisplayName("core.accesscontrol must not exist")
    void coreAccessControlMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.accesscontrol..")
          .as("core.accesscontrol must not exist — migrate to platform.accesscontrol")
          .check(allClasses);
    }

    /**
     * TODO: Enable after core.tenantuser is fully migrated to platform.identity.
     * Removal condition: no Java class under com.tchalanet.server.core.tenantuser remains.
     */
    @Test
    @Disabled("Allowlist: core.tenantuser pending migration to platform.identity")
    @DisplayName("core.tenantuser must not exist")
    void coreTenantUserMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.tenantuser..")
          .as("core.tenantuser must not exist — migrate to platform.identity")
          .check(allClasses);
    }

    /**
     * TODO: Enable after core.tenantconfig is fully migrated to platform.tenantconfig.
     * Removal condition: no Java class under com.tchalanet.server.core.tenantconfig remains.
     */
    @Test
    @Disabled("Allowlist: core.tenantconfig pending migration to platform.tenantconfig")
    @DisplayName("core.tenantconfig must not exist")
    void coreTenantConfigMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.tenantconfig..")
          .as("core.tenantconfig must not exist — migrate to platform.tenantconfig")
          .check(allClasses);
    }

    /**
     * TODO: Enable after core.tenanttheme is fully migrated to platform.tenanttheme.
     * Removal condition: no Java class under com.tchalanet.server.core.tenanttheme remains.
     */
    @Test
    @Disabled("Allowlist: core.tenanttheme pending migration to platform.tenanttheme")
    @DisplayName("core.tenanttheme must not exist")
    void coreTenantThemeMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.tenanttheme..")
          .as("core.tenanttheme must not exist — migrate to platform.tenanttheme")
          .check(allClasses);
    }

    /**
     * TODO: Enable after core.notification is migrated (or ADR exception documented).
     * Removal condition: no Java class under com.tchalanet.server.core.notification remains,
     * OR an ADR explicitly justifies keeping it in core.
     */
    @Test
    @Disabled("Allowlist: core.notification pending migration to platform.notification or ADR exception")
    @DisplayName("core.notification must not exist")
    void coreNotificationMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.notification..")
          .as("core.notification must not exist — migrate to platform.notification or document ADR exception")
          .check(allClasses);
    }

    /**
     * Migrated: core.address → platform.address.
     * Removal condition: no Java class under com.tchalanet.server.core.address remains.
     */
    @Test
    @Disabled("Allowlist: core.address pending migration to platform.address")
    @DisplayName("core.address must not exist")
    void coreAddressMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.address..")
          .as("core.address must not exist — migrate to platform.address")
          .check(allClasses);
    }

    /**
     * Migrated: core.external → platform.external.
     * Removal condition: no Java class under com.tchalanet.server.core.external remains.
     */
    @Test
    @Disabled("Allowlist: core.external pending migration to platform.external")
    @DisplayName("core.external must not exist")
    void coreExternalMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.external..")
          .as("core.external must not exist — migrate to platform.external")
          .check(allClasses);
    }

    /**
     * Migrated: core.featureflags → platform.featureflags.
     * Removal condition: no Java class under com.tchalanet.server.core.featureflags remains.
     */
    @Test
    @Disabled("Allowlist: core.featureflags pending migration to platform.featureflags")
    @DisplayName("core.featureflags must not exist")
    void coreFeatureFlagsMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.featureflags..")
          .as("core.featureflags must not exist — migrate to platform.featureflags")
          .check(allClasses);
    }

    /**
     * Migrated: core.tenantgame → platform.tenantgame.
     * Removal condition: no Java class under com.tchalanet.server.core.tenantgame remains.
     */
    @Test
    @Disabled("Allowlist: core.tenantgame pending migration to platform.tenantgame")
    @DisplayName("core.tenantgame must not exist")
    void coreTenantGameMustNotExist() {
      noClasses()
          .should().resideInAPackage("com.tchalanet.server.core.tenantgame..")
          .as("core.tenantgame must not exist — migrate to platform.tenantgame")
          .check(allClasses);
    }
  }
}
