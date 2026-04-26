package com.tchalanet.server.arch;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.base.DescribedPredicate.not;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class PageModelArchTest {

  private final JavaClasses classes = new ClassFileImporter().importPackages("com.tchalanet.server");

  @Test
  void pagemodel_does_not_import_core_domain_or_infra() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..features.pagemodel..")
        .should().dependOnClassesThat(
            resideInAPackage("..core..domain..")
                .or(resideInAPackage("..core..infra.."))
                .and(not(resideInAPackage("..core.pagemodel..")))
        )
        .because("features.pagemodel providers must use QueryBus, not core domain/infra directly (except core.pagemodel types)");

    rule.check(classes);
  }

  @Test
  void pagemodel_does_not_send_admin_queries() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..features.pagemodel..")
        .should().dependOnClassesThat()
        .haveSimpleNameEndingWith("AdminQuery")
        .because("features.pagemodel must use public/BFF queries, not admin queries");

    rule.check(classes);
  }
}
