package com.tchalanet.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests enforcing clean architecture boundaries and dependency rules.
 */
class CleanArchitectureRulesTest {

  private static JavaClasses classes;

  @BeforeAll
  static void setup() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.tchalanet.server");
  }

  @Test
  @DisplayName("Common layer has no dependency on core, catalog, platform, or features")
  void commonDoesNotDependOnBusinessLayers() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.tchalanet.server.common..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.tchalanet.server.core..",
                "com.tchalanet.server.catalog..",
                "com.tchalanet.server.platform..",
                "com.tchalanet.server.features..");

    rule.check(classes);
  }

  @Test
  @DisplayName("Core layer has no dependency on features")
  void coreDoesNotDependOnFeatures() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.tchalanet.server.core..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.tchalanet.server.features..");

    rule.check(classes);
  }

  @Test
  @DisplayName("Catalog API has no dependency on catalog internal")
  void catalogApiDoesNotDependOnInternal() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.tchalanet.server.catalog..api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.tchalanet.server.catalog..internal..");

    rule.check(classes);
  }

  @Test
  @DisplayName("Features do not depend on JPA repositories/entities")
  void featuresDoNotAccessPersistenceDirectly() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.tchalanet.server.features..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infra.persistence..", "..persistence..")
            .orShould()
            .dependOnClassesThat()
            .areAssignableTo(jakarta.persistence.EntityManager.class)
            .orShould()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Entity")
            .andShould()
            .dependOnClassesThat()
            .resideInAPackage("..infra..");

    rule.check(classes);
  }

  @Test
  @DisplayName("Domain packages do not depend on Spring/JPA/web/infra")
  void domainIsFrameworkFree() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate..",
                "jakarta.servlet..",
                "..infra..",
                "..web..");

    rule.check(classes);
  }

  @Test
  @DisplayName("Controllers do not depend on repositories")
  void controllersDoNotAccessRepositories() {
    ArchRule rule =
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository")
            .orShould()
            .dependOnClassesThat()
            .resideInAPackage("..infra.persistence..");

    rule.check(classes);
  }

  @Test
  @DisplayName("Controllers dispatch use-cases through CommandBus or QueryBus")
  void controllersUseCommandOrQueryBus() {
    // This is an optional rule - we verify controllers have a CommandBus or QueryBus field
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..features..")
            .and()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameMatching(".*(CommandBus|QueryBus)");

    rule.check(classes);
  }

  @Test
  @DisplayName("Layered architecture is respected (includes platform layer)")
  void layeredArchitectureIsRespected() {
    layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Common")
        .definedBy("com.tchalanet.server.common..")
        .layer("Catalog")
        .definedBy("com.tchalanet.server.catalog..")
        .layer("Platform")
        .definedBy("com.tchalanet.server.platform..")
        .layer("Core")
        .definedBy("com.tchalanet.server.core..")
        .layer("Features")
        .definedBy("com.tchalanet.server.features..")
        .whereLayer("Common")
        .mayNotAccessAnyLayer()
        .whereLayer("Catalog")
        .mayOnlyAccessLayers("Common")
        .whereLayer("Platform")
        .mayOnlyAccessLayers("Common", "Catalog")
        .whereLayer("Core")
        .mayOnlyAccessLayers("Common", "Catalog", "Platform")
        .whereLayer("Features")
        .mayOnlyAccessLayers("Common", "Catalog", "Platform", "Core")
        .check(classes);
  }

  @Test
  @DisplayName("No cyclic dependencies between slices")
  void noCyclicDependencies() {
    SlicesRuleDefinition.slices()
        .matching("com.tchalanet.server.(*)..")
        .should()
        .beFreeOfCycles()
        .check(classes);
  }
}

