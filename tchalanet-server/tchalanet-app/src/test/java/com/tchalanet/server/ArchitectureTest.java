package com.tchalanet.server;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class ArchitectureTest {

  @Test
  void noOneShouldUseInternalPackages() {
    var classes = new ClassFileImporter().importPackages("com.tchalanet.server");

    forbidInternal("catalog.resultslot", classes);
    forbidInternal("catalog.game", classes);
    forbidInternal("catalog.drawresult", classes);
    forbidInternal("catalog.pricing", classes);
    forbidInternal("core.analytics", classes);
  }

  /**
   * TODO(v2): CashierSessionView.from(SalesSession) is a known violation — features.cashier.session
   * directly maps from core.session.internal domain model. Needs SalesSession promoted to
   * core.session.api before this rule can be enforced. Track in core.session refactor.
   */
  @Test
  public void coreSessionInternalShouldNotLeakToAnalytics() {
    JavaClasses classes = new ClassFileImporter()
        .importPackages("com.tchalanet.server.core.analytics..");

    noClasses()
        .that().resideInAPackage("com.tchalanet.server.core.analytics..")
        .should().dependOnClassesThat()
        .resideInAPackage("com.tchalanet.server.core.session.internal..")
        .as("core.analytics must not depend on core.session.internal")
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  public void featuresAndPlatformMustNotAccessCoreAnalyticsInternal() {
    JavaClasses classes = new ClassFileImporter()
        .importPackages(
            "com.tchalanet.server.features..",
            "com.tchalanet.server.platform..");

    noClasses()
        .that().resideInAnyPackage(
            "com.tchalanet.server.features..",
            "com.tchalanet.server.platform..")
        .should().dependOnClassesThat()
        .resideInAPackage("com.tchalanet.server.core.analytics.internal..")
        .as("features and platform must only use core.analytics.api, never core.analytics.internal")
        .check(classes);
  }

  @Test
  void coreSalesApiMustNotDependOnCoreSalesInternal() {
    var classes = new ClassFileImporter()
        .importPackages("com.tchalanet.server.core.sales.api");

    noClasses()
        .that()
        .resideInAPackage("com.tchalanet.server.core.sales.api..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.tchalanet.server.core.sales.internal..")
        .as("core.sales.api must expose public DTOs, not internal sales aggregates")
        .check(classes);
  }

  private static void forbidInternal(String domainPath, JavaClasses classes) {
    String base = "com.tchalanet.server." + domainPath;
    noClasses()
        .that()
        .resideOutsideOfPackage(base + "..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(base + ".internal..")
        .check(classes);
  }

  // Game-Tenantgame split: catalog/game is pure catalog, no tenant logic
  @Test
  public void catalogGameMustNotDependOnCoreTenantgame() {
    JavaClasses classes = new ClassFileImporter()
        .importPackages("com.tchalanet.server.catalog.game");

    ArchRule rule = noClasses()
        .that().resideInAPackage("com.tchalanet.server.catalog.game..")
        .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.core.tenantgame..")
        .as("catalog/game must NOT depend on core/tenantgame");

    rule.check(classes);
  }

  @Test
  public void coreTenantgameMayDependOnCatalogGameApiOnly() {
    JavaClasses classes = new ClassFileImporter()
        .importPackages("com.tchalanet.server.core.tenantgame");

    // Allow dependency on catalog/game/api (public API)
    ArchRule rule = noClasses()
        .that().resideInAPackage("com.tchalanet.server.core.tenantgame..")
        .should().dependOnClassesThat().resideInAnyPackage("com.tchalanet.server.catalog.game.internal..",
                         "com.tchalanet.server.catalog.game.infra..")
        .as("core/tenantgame must NOT depend on catalog/game/internal or infra; only api is allowed")
        .allowEmptyShould(true);

    rule.check(classes);
  }

  @Test
  public void gameAdminControllerMustNotExposeDomainEntities() {
    JavaClasses classes = new ClassFileImporter()
        .importPackages("com.tchalanet.server.catalog.game..",
                       "com.tchalanet.server.core.tenantgame..");

    ArchRule rule = methods()
        .that().areAnnotatedWith(RequestMapping.class)
        .or().areAnnotatedWith(GetMapping.class)
        .or().areAnnotatedWith(PostMapping.class)
        .or().areAnnotatedWith(PutMapping.class)
        .or().areAnnotatedWith(DeleteMapping.class)
        .should().haveRawReturnType(ApiResponse.class)
        .orShould().haveRawReturnType(ResponseEntity.class)
        .as("Controllers must return ApiResponse or ResponseEntity; never raw domain entities");

    rule.check(classes);
  }

  @Test
  public void catalogModulesMustNotDependOnTheirInternals_viaApi() {
    // Enforce: catalog.<name>.api MUST NOT depend on catalog.<name>.internal
    JavaClasses classes = new ClassFileImporter().importPackages("com.tchalanet.server.catalog..");

    ArchRule rule = noClasses()
        .that().resideInAPackage("com.tchalanet.server.catalog..api..")
        .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.catalog..internal..")
        .as("catalog.api must not depend on catalog.internal");

    rule.check(classes);
  }

  @Test
  public void catalogControllersMustReturnApiResponseOrResponseEntity() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.tchalanet.server.catalog..");

    ArchRule rule = methods()
        .that().areAnnotatedWith(RequestMapping.class)
        .or().areAnnotatedWith(GetMapping.class)
        .or().areAnnotatedWith(PostMapping.class)
        .or().areAnnotatedWith(PutMapping.class)
        .or().areAnnotatedWith(DeleteMapping.class)
        .should().haveRawReturnType(ApiResponse.class)
        .orShould().haveRawReturnType(ResponseEntity.class)
        .as("Catalog controllers must return ApiResponse or ResponseEntity");

    rule.check(classes);
  }

  @Test
  public void catalogControllersMustNotAccessPersistence() {
    // Controllers under catalog.*.internal.web must not depend on persistence/repositories directly
    JavaClasses classes = new ClassFileImporter().importPackages("com.tchalanet.server.catalog..");

    ArchRule rule = noClasses()
        .that().resideInAPackage("com.tchalanet.server.catalog..internal.web..")
        .should().dependOnClassesThat().resideInAnyPackage("..persistence..", "..repository..")
        .as("Catalog controllers must not depend on internal.persistence (repositories)");

    rule.check(classes);
  }

  @Test
  public void catalogMustNotEmitDomainEvents() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.tchalanet.server.catalog..");

    ArchRule rule = noClasses()
        .that().resideInAPackage("com.tchalanet.server.catalog..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "com.tchalanet.server.common.event..",
            "com.tchalanet.server.core..domain.event..")
        .as("catalog modules must NOT emit domain events (pure reference data)");

    rule.check(classes);
  }

  @Test
  public void catalogModulesMustNotDependOnCore() {
    JavaClasses classes = new ClassFileImporter().importPackages("com.tchalanet.server.catalog..");

    ArchRule rule = noClasses()
        .that().resideInAPackage("com.tchalanet.server.catalog..")
        .should().dependOnClassesThat().resideInAPackage("com.tchalanet.server.core..")
        .as("catalog must not depend on core modules");

    rule.check(classes);
  }

}
