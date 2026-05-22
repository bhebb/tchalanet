package com.tchalanet.server.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Static enforcement of timezone and time-source discipline in core business modules.
 *
 * <p>Rules:
 * <ol>
 *   <li>{@code LocalDateTime} MUST NOT be used in domain or application layers.</li>
 *   <li>{@code ZoneId.systemDefault()} MUST NOT be called in business modules.</li>
 *   <li>{@code Instant.now()} (no-arg) and {@code LocalDate.now()} (no-arg) MUST NOT be called
 *       in core domain or application layers — use {@code TchTimeProvider} or pass an
 *       explicit {@code Clock}.</li>
 * </ol>
 *
 * <p>Scope is intentionally limited to actively maintained core domains (draw, sales, session,
 * offlinesync). Known pre-existing violations in other domains are tracked in separate tasks.
 */
@DisplayName("Timezone enforcement (ArchUnit)")
class TimezoneEnforcementArchTest {

    /**
     * Core domains that have been audited and must comply with timezone rules.
     * Expand this list as each domain is reviewed.
     */
    private static final String[] COMPLIANT_CORE_PACKAGES = {
        "com.tchalanet.server.core.draw..",
        "com.tchalanet.server.core.sales..",
        "com.tchalanet.server.core.session..",
        "com.tchalanet.server.core.offlinesync..",
    };

    private static JavaClasses coreClasses;

    @BeforeAll
    static void importClasses() {
        coreClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.tchalanet.server.core");
    }

    @Nested
    @DisplayName("No LocalDateTime in domain or application layers")
    class NoLocalDateTime {

        @Test
        @DisplayName("draw and sales domain/application must not depend on LocalDateTime")
        void coreDomainAndApplicationMustNotUseLocalDateTime() {
            ArchRule rule = noClasses()
                .that().resideInAnyPackage(COMPLIANT_CORE_PACKAGES)
                .and().resideInAnyPackage("..domain..", "..application..")
                .and().resideOutsideOfPackage("..infra..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(LocalDateTime.class.getName())
                .because(
                    "LocalDateTime has no timezone context and must not be used in domain or application "
                        + "layers. Use Instant for event moments, LocalDate for commercial dates, "
                        + "or ZonedDateTime when zone context is required. "
                        + "See docs/conventions/timezone.md");

            rule.allowEmptyShould(true).check(coreClasses);
        }
    }

    @Nested
    @DisplayName("No ZoneId.systemDefault() in business code")
    class NoSystemDefaultZone {

        @Test
        @DisplayName("core draw/sales/session must not call ZoneId.systemDefault()")
        void coreBusinessMustNotUseSystemDefaultZone() {
            // ZoneId.systemDefault() depends on JVM locale and is non-deterministic in tests.
            // Use channel.timezone, tenant.timezone, or a ZoneId from config instead.
            ArchRule rule = noClasses()
                .that().resideInAnyPackage(COMPLIANT_CORE_PACKAGES)
                .and().resideOutsideOfPackage("..infra..")
                .should().callMethod(ZoneId.class, "systemDefault")
                .because(
                    "ZoneId.systemDefault() relies on JVM locale and is non-deterministic across "
                        + "deployments and timezones. Use channel.timezone or tenant.timezone "
                        + "explicitly. See docs/conventions/timezone.md");

            rule.allowEmptyShould(true).check(coreClasses);
        }
    }

    @Nested
    @DisplayName("No raw Instant.now() or LocalDate.now() in domain/application")
    class NoDirectNow {

        @Test
        @DisplayName("core draw/sales domain and application must not call Instant.now() (no-arg)")
        void coreMustNotCallInstantNowNoArg() {
            // Instant.now() uses Clock.systemDefaultZone() under the hood — not testable.
            // Use TchTimeProvider.now() or Instant.now(clock) with an injected Clock.
            ArchRule rule = noClasses()
                .that().resideInAnyPackage(COMPLIANT_CORE_PACKAGES)
                .and().resideInAnyPackage("..domain..", "..application..")
                .and().resideOutsideOfPackage("..infra..")
                .should().callMethod(Instant.class, "now")
                .because(
                    "Instant.now() without a Clock argument is not testable. "
                        + "Inject TchTimeProvider and call timeProvider.now() instead. "
                        + "See docs/conventions/timezone.md");

            rule.allowEmptyShould(true).check(coreClasses);
        }

        @Test
        @DisplayName("core draw/sales domain and application must not call LocalDate.now() (no-arg)")
        void coreMustNotCallLocalDateNowNoArg() {
            ArchRule rule = noClasses()
                .that().resideInAnyPackage(COMPLIANT_CORE_PACKAGES)
                .and().resideInAnyPackage("..domain..", "..application..")
                .and().resideOutsideOfPackage("..infra..")
                .should().callMethod(LocalDate.class, "now")
                .because(
                    "LocalDate.now() uses the JVM system clock without timezone context. "
                        + "Use TchTimeProvider.today(channelZoneId) instead. "
                        + "See docs/conventions/timezone.md");

            rule.allowEmptyShould(true).check(coreClasses);
        }
    }
}

