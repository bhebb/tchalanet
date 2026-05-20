package com.tchalanet.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Sales and OfflineSync module boundary rules")
class SalesOfflineSyncModuleBoundaryTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.tchalanet.server");
    }

    @Test
    @DisplayName("core.offlinesync does not import core.sales internal packages")
    void offlineSyncDoesNotImportSalesInternal() {
        noClasses()
            .that().resideInAPackage("com.tchalanet.server.core.offlinesync..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.tchalanet.server.core.sales.internal..")
            .check(classes);
    }

    @Test
    @DisplayName("features.cashier.tickets only uses core.sales public api")
    void cashierTicketsOnlyUsesSalesApi() {
        noClasses()
            .that().resideInAPackage("com.tchalanet.server.features.cashier.tickets..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.tchalanet.server.core.sales.internal..")
            .check(classes);
    }

    @Test
    @DisplayName("core.sales does not import core.offlinesync internal packages")
    void salesDoesNotImportOfflineSyncInternal() {
        noClasses()
            .that().resideInAPackage("com.tchalanet.server.core.sales..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.tchalanet.server.core.offlinesync.internal..")
            .check(classes);
    }

    @Test
    @DisplayName("features.cashier.tickets does not import core.offlinesync internal packages")
    void cashierDoesNotImportOfflineSyncInternal() {
        noClasses()
            .that().resideInAPackage("com.tchalanet.server.features.cashier.tickets..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.tchalanet.server.core.offlinesync.internal..")
            .check(classes);
    }

    @Test
    @DisplayName("core.sales api packages do not import core.sales internal packages")
    void salesApiDoesNotImportSalesInternal() {
        noClasses()
            .that().resideInAPackage("com.tchalanet.server.core.sales.api..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.tchalanet.server.core.sales.internal..")
            .check(classes);
    }
}
