package com.tchalanet.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OperationalContextArchitectureTest {

    private static com.tngtech.archunit.core.domain.JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.tchalanet.server");
    }

    @Test
    void commonOperationalContextDoesNotImportBusinessLayersOrPersistence() {
        noClasses()
            .that()
            .resideInAPackage("com.tchalanet.server.common.context.operational..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.tchalanet.server.platform..",
                "com.tchalanet.server.core..",
                "com.tchalanet.server.catalog..",
                "com.tchalanet.server.features..",
                "..repository..",
                "..persistence..")
            .check(classes);
    }

    @Test
    void commonOperationalContextDoesNotImportBuses() {
        noClasses()
            .that()
            .resideInAPackage("com.tchalanet.server.common.context.operational..")
            .should()
            .dependOnClassesThat()
            .haveSimpleName("CommandBus")
            .orShould()
            .dependOnClassesThat()
            .haveSimpleName("QueryBus")
            .check(classes);
    }

    @Test
    void operationalContextFilterDoesNotExist() {
        assertThat(classes)
            .noneMatch(javaClass -> javaClass.getSimpleName().equals("OperationalContextFilter"));
    }

    @Test
    void platformUserContextPackageDoesNotExist() {
        assertThat(classes)
            .noneMatch(javaClass -> javaClass.getPackageName().startsWith(
                "com.tchalanet.server.platform.usercontext"));
    }

    @Test
    void flatOperationalContextBridgeUsageIsConstrainedDuringMigration() {
        noClasses()
            .that()
            .resideOutsideOfPackages(
                "com.tchalanet.server.common.context..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(OperationalRequestContext.class)
            .check(classes);
    }
}
