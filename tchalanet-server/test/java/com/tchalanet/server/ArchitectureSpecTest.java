package com.tchalanet.server;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "com.tchalanet.server")
public class ArchitectureSpecTest {

    @ArchTest
    public static final com.tngtech.archunit.lang.ArchRule catalog_api_should_not_depend_on_internal =
        ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..catalog..api..")
            .should().dependOnClassesThat().resideInAPackage("..catalog..internal..");
}
