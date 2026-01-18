package com.tchalanet.server;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

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
}
