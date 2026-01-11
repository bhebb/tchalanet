package com.tchalanet.server;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  @Test
  void no_one_should_use_internal_packages() {
    var classes = new ClassFileImporter().importPackages("com.tchalanet.server");

    forbidInternal("core.resultslot", classes);
    forbidInternal("core.game", classes);
    forbidInternal("core.drawresult", classes);
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
}
