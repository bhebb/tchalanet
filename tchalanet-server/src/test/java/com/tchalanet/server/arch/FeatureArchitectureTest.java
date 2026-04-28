package com.tchalanet.server.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class FeatureArchitectureTest {

  private static final String FEATURES = "com.tchalanet.server.features..";
  private static final String CORE = "com.tchalanet.server.core..";
  private static final String CATALOG = "com.tchalanet.server.catalog..";

  private final JavaClasses classes = new ClassFileImporter().importPackages("com.tchalanet.server");

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
}
