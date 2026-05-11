package com.tchalanet.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Bus naming rules")
class BusNamingArchUnitTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(new ImportOption.DoNotIncludeTests())
          .importPackages("com.tchalanet.server");

  @Test
  @DisplayName("new code should not call legacy bus methods")
  void codeShouldNotCallLegacyBusMethods() {
    noClasses()
        .that()
        .resideOutsideOfPackage("..common.bus..")
        .should(callLegacyBusMethods())
        .check(CLASSES);
  }

  private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass> callLegacyBusMethods() {
    return new ArchCondition<>("call legacy CommandBus/QueryBus methods send or handle") {
      @Override
      public void check(
          com.tngtech.archunit.core.domain.JavaClass item, ConditionEvents events) {
        for (JavaCall<?> call : item.getMethodCallsFromSelf()) {
          var ownerName = call.getTargetOwner().getName();
          var methodName = call.getName();

          boolean isBus =
              ownerName.equals("com.tchalanet.server.common.bus.CommandBus")
                  || ownerName.equals("com.tchalanet.server.common.bus.QueryBus");

          boolean isLegacy = methodName.equals("send") || methodName.equals("handle");

          if (isBus && isLegacy) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    item.getName()
                        + " calls legacy bus method "
                        + ownerName
                        + "."
                        + methodName
                        + " at "
                        + call.getSourceCodeLocation()));
          }
        }
      }
    };
  }
}
