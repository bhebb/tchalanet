package com.tchalanet.server.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

class FeatureArchitectureTest {

  private static final String FEATURES = "com.tchalanet.server.features..";
  private static final String CORE = "com.tchalanet.server.core..";
  private static final String CATALOG = "com.tchalanet.server.catalog..";
  private static final String COMMON = "com.tchalanet.server.common..";

  private final JavaClasses classes = new ClassFileImporter().importPackages("com.tchalanet.server");

  @Test
  void featuresMustNotAccessSpringDataOrJpa() {
    ArchRule rule = noClasses()
        .that().resideInAPackage(FEATURES)
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework.data..",
            "org.springframework.orm..",
            "org.springframework.jdbc..",
            "jakarta.persistence..",
            "org.hibernate.."
        );

    rule.check(classes);
  }

  @Test
  void featuresMustNotDependOnInfraPersistencePackages() {
    ArchRule rule = noClasses()
        .that().resideInAPackage(FEATURES)
        .should().dependOnClassesThat().resideInAnyPackage(
            "..infra.persistence.."
        );

    rule.check(classes);
  }

  @Test
  void featuresMustNotReferenceJpaEntities() {
    ArchRule rule = noClasses()
        .that().resideInAPackage(FEATURES)
        .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    rule.check(classes);
  }

  @Test
  void coreMustNotDependOnFeatures() {
    ArchRule rule = noClasses()
        .that().resideInAPackage(CORE)
        .should().dependOnClassesThat().resideInAPackage(FEATURES);

    rule.check(classes);
  }

  @Test
  void catalogMustNotDependOnCoreOrFeatures() {
    ArchRule rule = noClasses()
        .that().resideInAPackage(CATALOG)
        .should().dependOnClassesThat().resideInAnyPackage(CORE, FEATURES);

    rule.check(classes);
  }

  @Test
  void featuresMustNotUseUuid() {
    ArchRule rule = noClasses()
        .that().resideInAPackage(FEATURES)
        .should().dependOnClassesThat().haveFullyQualifiedName("java.util.UUID");

    rule.check(classes);
  }
}
