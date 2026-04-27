package com.tchalanet.server.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * ArchUnit rule — Sécurité des scopes protégés.
 *
 * <p>Tout {@link RestController} dont le {@code @RequestMapping} commence par
 * {@code /admin/}, {@code /platform/} ou {@code /_sdr/} DOIT porter
 * {@code @PreAuthorize} au niveau classe OU sur chaque méthode handler publique.
 *
 * <p>Si un endpoint doit être public dans ces scopes, il DOIT porter
 * {@code @PreAuthorize("permitAll()")} explicitement (whitelist obligatoire).
 *
 * <p>Voir : {@code docs/conventions/api/web_api.md} §13
 */
class SecurityArchTest {

    private static final List<String> PROTECTED_PREFIXES =
            List.of("/admin/", "/platform/", "/_sdr/");

    private final JavaClasses classes =
            new ClassFileImporter().importPackages("com.tchalanet.server");

    @Test
    void protectedScopeControllersMustHavePreAuthorize() {
        classes()
                .that().areAnnotatedWith(RestController.class)
                .and(new InProtectedScopeCondition())
                .should(new HavePreAuthorizeCondition())
                .check(classes);
    }

    /** Predicate : controller dont le @RequestMapping est dans un scope protégé. */
    private static class InProtectedScopeCondition
            extends com.tngtech.archunit.base.DescribedPredicate<JavaClass> {

        InProtectedScopeCondition() {
            super("mapped under a protected scope prefix (/admin/, /platform/, /_sdr/)");
        }

        @Override
        public boolean test(JavaClass javaClass) {
            return javaClass.isAnnotatedWith(RequestMapping.class)
                    && Arrays.stream(
                                    javaClass
                                            .getAnnotationOfType(RequestMapping.class)
                                            .value())
                            .anyMatch(
                                    path ->
                                            PROTECTED_PREFIXES.stream()
                                                    .anyMatch(path::startsWith));
        }
    }

    /** Condition : @PreAuthorize présent au niveau classe ou sur chaque méthode handler publique. */
    private static class HavePreAuthorizeCondition extends ArchCondition<JavaClass> {

        HavePreAuthorizeCondition() {
            super(
                    "have @PreAuthorize at class level or on every public handler method"
                            + " (use @PreAuthorize(\"permitAll()\") to explicitly whitelist public endpoints)");
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            boolean classLevelAnnotated =
                    javaClass.isAnnotatedWith(PreAuthorize.class);

            if (classLevelAnnotated) {
                // class-level @PreAuthorize covers all handlers — OK
                return;
            }

            // Check that every public method carries @PreAuthorize
            List<JavaMethod> handlerMethods =
                    javaClass.getMethods().stream()
                            .filter(m -> m.getModifiers()
                                    .contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC))
                            .filter(m -> !m.isAnnotatedWith(Override.class))
                            .filter(m -> !m.getRawReturnType()
                                    .isEquivalentTo(void.class))
                            .filter(HavePreAuthorizeCondition::isHandlerMethod)
                            .toList();

            if (handlerMethods.isEmpty()) {
                // No handler methods — edge case, pass
                return;
            }

            List<JavaMethod> unsecured =
                    handlerMethods.stream()
                            .filter(m -> !m.isAnnotatedWith(PreAuthorize.class))
                            .toList();

            if (!unsecured.isEmpty()) {
                String detail =
                        "Controller ["
                                + javaClass.getName()
                                + "] is in a protected scope but missing @PreAuthorize"
                                + " at class level. Unsecured handler methods: "
                                + unsecured.stream()
                                        .map(JavaMethod::getName)
                                        .toList();
                events.add(SimpleConditionEvent.violated(javaClass, detail));
            }
        }

        private static boolean isHandlerMethod(JavaMethod method) {
            return method.isAnnotatedWith(
                            org.springframework.web.bind.annotation.GetMapping.class)
                    || method.isAnnotatedWith(
                            org.springframework.web.bind.annotation.PostMapping.class)
                    || method.isAnnotatedWith(
                            org.springframework.web.bind.annotation.PutMapping.class)
                    || method.isAnnotatedWith(
                            org.springframework.web.bind.annotation.DeleteMapping.class)
                    || method.isAnnotatedWith(
                            org.springframework.web.bind.annotation.PatchMapping.class)
                    || method.isAnnotatedWith(
                            org.springframework.web.bind.annotation.RequestMapping.class);
        }
    }
}

