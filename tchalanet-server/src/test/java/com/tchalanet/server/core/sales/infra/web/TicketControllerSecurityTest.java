package com.tchalanet.server.core.sales.infra.web;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.annotation.Secured;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security annotation tests for {@link TicketController}.
 *
 * <p>Verifies that the 4 previously-unsecured endpoints (cancel, print, printEscpos, printPdf)
 * carry {@code @Secured} with the required role set. This provides deterministic build-time
 * proof that the annotations are present and correct, without requiring a Spring context.
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
        assertHasSecured("cancel");
    }

    @Test
    void printEndpoint_hasSecuredWithRequiredRoles() {
        assertHasSecured("print");
    }

    @Test
    void printEscposEndpoint_hasSecuredWithRequiredRoles() {
        assertHasSecured("printEscpos");
    }

    @Test
    void printPdfEndpoint_hasSecuredWithRequiredRoles() {
        assertHasSecured("printPdf");
    }

    @Test
    void ticketController_hasTenMethods_withSecured() {
        long count = Arrays.stream(TicketController.class.getMethods())
                .filter(m -> m.isAnnotationPresent(Secured.class))
                .count();
        assertThat(count)
                .as("TicketController should have exactly 10 @Secured methods")
                .isEqualTo(10);
    }

    private void assertHasSecured(String methodPrefix) {
        List<Method> matches = Arrays.stream(TicketController.class.getMethods())
                .filter(m -> m.getName().equals(methodPrefix) || m.getName().startsWith(methodPrefix + "("))
                .filter(m -> m.isAnnotationPresent(Secured.class))
                .toList();

        assertThat(matches)
                .as("Method '%s' in TicketController should have @Secured annotation", methodPrefix)
                .isNotEmpty();

        for (Method m : matches) {
            Secured secured = m.getAnnotation(Secured.class);
            Set<String> actualRoles = Set.of(secured.value());
            assertThat(actualRoles)
                    .as("Method '%s' @Secured should contain all required roles", methodPrefix)
                    .containsAll(REQUIRED_ROLES);
        }
    }
}

