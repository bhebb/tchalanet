package com.tchalanet.server;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tchalanet.server.common.web.api.ApiResponse;
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
        .should().dependOnClassesThat()
        .resideInAPackage("com.tchalanet.server.catalog.game.internal..",
                         "com.tchalanet.server.catalog.game.infra..")
        .as("core/tenantgame must NOT depend on catalog/game/internal or infra; only api is allowed");

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
        .should().haveReturnType(ApiResponse.class)
        .orShould().haveReturnTypeAssignableTo(ResponseEntity.class)
        .as("Controllers must return ApiResponse or ResponseEntity; never raw domain entities");

    rule.check(classes);
  }

  @Test
  public void catalogGameMustNotEmitDomainEvents() {
    JavaClasses classes = new ClassFileImporter()
        .importPackages("com.tchalanet.server.catalog.game");

    ArchRule rule = noClasses()
        .that().resideInAPackage("com.tchalanet.server.catalog.game..")
        .should().haveAnyMembersThat().haveSimpleName("publishEvent")
        .orShould().haveAnyMembersThat().haveSimpleName("emit")
        .as("catalog/game must NOT emit domain events (pure reference data)");

    rule.check(classes);
  }
}
