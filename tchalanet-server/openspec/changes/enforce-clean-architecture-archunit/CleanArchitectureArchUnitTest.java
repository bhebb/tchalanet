package com.tchalanet.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

@DisplayName("Clean architecture package rules")
class CleanArchitectureArchUnitTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(new ImportOption.DoNotIncludeTests())
          .importPackages("com.tchalanet.server");

  @Test
  @DisplayName("domain must not depend on application or infra or Spring/JPA/Web")
  void domainMustRemainPure() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..application..",
            "..infra..",
            "org.springframework..",
            "jakarta.persistence..",
            "javax.persistence..",
            "org.hibernate..")
        .check(CLASSES);
  }

  @Test
  @DisplayName("application must not depend on infra packages")
  void applicationMustNotDependOnInfra() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..infra.web..",
            "..infra.persistence..",
            "..infra.cache..",
            "..infra.batch..",
            "..infra.scheduler..")
        .check(CLASSES);
  }

  @Test
  @DisplayName("web must not depend directly on persistence")
  void webMustNotDependOnPersistence() {
    noClasses()
        .that()
        .resideInAPackage("..infra.web..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infra.persistence..")
        .check(CLASSES);
  }

  @Test
  @DisplayName("controllers must live in web packages")
  void controllersMustLiveInWebPackages() {
    classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .or()
        .areAnnotatedWith(Controller.class)
        .should()
        .resideInAnyPackage("..infra.web..", "..web..")
        .check(CLASSES);
  }

  @Test
  @DisplayName("JPA entities must live in infra persistence")
  void jpaEntitiesMustLiveInPersistence() {
    classes()
        .that()
        .areAnnotatedWith("jakarta.persistence.Entity")
        .should()
        .resideInAPackage("..infra.persistence..")
        .check(CLASSES);
  }

  @Test
  @DisplayName("port.in packages are forbidden by default")
  void portInPackagesAreForbiddenByDefault() {
    noClasses()
        .should()
        .resideInAnyPackage("..port.in..", "..port.input..", "..application.port.in..")
        .check(CLASSES);
  }

  @Test
  @DisplayName("modules must not depend on another module infra")
  void modulesMustNotDependOnOtherModuleInfra() {
    noClasses()
        .that()
        .resideInAPackage("..core.(*)..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..core.(*)..infra..")
        // NOTE: ArchUnit capture groups need refinement in real repo.
        // Keep this as a starting point; replace with slices/rules if too broad.
        .because("a domain must never depend on another domain's infra")
        .check(CLASSES);
  }
}
