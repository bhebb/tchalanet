package com.tchalanet.server.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
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

  @Test
  void featuresMustNotOwnCommandHandlers() {
    ArchRule rule = noClasses()
        .that().resideInAPackage(FEATURES)
        .should().beAssignableTo(CommandHandler.class)
        .orShould().beAssignableTo(VoidCommandHandler.class)
        .orShould().beAssignableTo(QueryHandler.class)
        .because("features are BFF/orchestration surfaces; domain command handlers belong to core");

    rule.check(classes);
  }

  @Test
  void featuresMustNotUseLegacyLayerPackages() {
    ArchRule rule = noClasses()
        .that().resideInAPackage(FEATURES)
        .should().resideInAnyPackage(
            "..features..application..",
            "..features..adapter..",
            "..features..dto..",
            "..features..handler..",
            "..features..infra..",
            "..features..port..",
            "..features..query..",
            "..features..repo..")
        .because("feature slices use vertical BFF roles such as web, app, model, mapper, dynamic and shared");

    rule.check(classes);
  }

  @Test
  void featuresMustNotUseUseCaseClassNames() {
    ArchRule rule = ArchRuleDefinition.classes()
        .that().resideInAPackage(FEATURES)
        .should().haveSimpleNameNotEndingWith("UseCase")
        .because("feature orchestration classes are named Services or Orchestrators, not UseCases");

    rule.check(classes);
  }

  @Test
  void featuresMustNotUseDtoClassNames() {
    ArchRule rule = ArchRuleDefinition.classes()
        .that().resideInAPackage(FEATURES)
        .should().haveSimpleNameNotEndingWith("Dto")
        .because("feature UI contracts are named Request, Response, View, Item or Summary");

    rule.check(classes);
  }

  @Test
  void featuresMustNotUseHandlerOrQueryClassNames() {
    ArchRule rule = ArchRuleDefinition.classes()
        .that().resideInAPackage(FEATURES)
        .should().haveSimpleNameNotEndingWith("Handler")
        .andShould().haveSimpleNameNotEndingWith("Query")
        .because("feature slices expose BFF services with criteria/models, not command/query handlers");

    rule.check(classes);
  }

  @Test
  void featureAppPackagesMustNotDefineRepositories() {
    ArchRule rule = ArchRuleDefinition.classes()
        .that().resideInAPackage("..features..app..")
        .should().haveSimpleNameNotEndingWith("Repository")
        .because("feature app packages orchestrate; read access contracts are readers or persistence internals");

    rule.check(classes);
  }
}
