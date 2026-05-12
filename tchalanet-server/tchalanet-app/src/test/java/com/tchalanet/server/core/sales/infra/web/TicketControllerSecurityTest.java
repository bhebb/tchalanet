package com.tchalanet.server.core.sales.infra.web;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.annotation.Secured;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security annotation tests for split ticket controllers.
 *
 * <p>Verifies that tenant cashier endpoints carry {@code @Secured} with the required role set.
 * This provides deterministic build-time proof that the annotations are present and correct,
 * without requiring a Spring context.
 *
 * <p>Runtime enforcement (HTTP 401/403) is guaranteed by:
 * <ul>
 *   <li>{@code SecurityArchTest} — ArchUnit build-time rule that prevents regressions</li>
 *   <li>{@code @EnableMethodSecurity(securedEnabled = true)} in {@code SecurityConfig}</li>
 * </ul>
 */
class TicketControllerSecurityTest {

    private static final Set<String> REQUIRED_ROLES =
            Set.of("ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");

    // 5.2 / 5.3 / 5.4 — Verify @Secured present with correct roles on all 4 endpoints

    @Test
    void cancelEndpoint_hasSecuredWithRequiredRoles() {
        assertHasSecured(TicketLifecycleController.class, "cancel", REQUIRED_ROLES);
    }

    @Test
    void printEscposEndpoint_hasSecuredWithRequiredRoles() {
        assertHasSecured(
            com.tchalanet.server.features.receipt.ReceiptController.class,
            "printEscpos",
            REQUIRED_ROLES);
    }

    @Test
    void printPdfEndpoint_hasSecuredWithRequiredRoles() {
        assertHasSecured(
            com.tchalanet.server.features.receipt.ReceiptController.class,
            "printPdf",
            REQUIRED_ROLES);
    }

    @Test
    void qrEndpoint_hasSecuredWithRequiredRoles() {
        assertHasSecured(
            com.tchalanet.server.features.receipt.ReceiptController.class,
            "qrPng",
            REQUIRED_ROLES);
    }

    @Test
    void base64PrintEndpoint_isRemoved() {
        var names =
            Arrays.stream(com.tchalanet.server.features.receipt.ReceiptController.class.getMethods())
                .map(Method::getName)
                .toList();

        assertThat(names).doesNotContain("print");
    }

    private void assertHasSecured(Class<?> controller, String methodPrefix, Set<String> requiredRoles) {
        List<Method> matches = Arrays.stream(controller.getMethods())
                .filter(m -> m.getName().equals(methodPrefix) || m.getName().startsWith(methodPrefix + "("))
                .filter(m -> m.isAnnotationPresent(Secured.class))
                .toList();

        assertThat(matches)
                .as("Method '%s' in %s should have @Secured annotation", methodPrefix, controller.getSimpleName())
                .isNotEmpty();

        for (Method m : matches) {
            Secured secured = m.getAnnotation(Secured.class);
            Set<String> actualRoles = Set.of(secured.value());
            assertThat(actualRoles)
                    .as("Method '%s' @Secured should contain all required roles", methodPrefix)
                    .containsAll(requiredRoles);
        }
    }
}
