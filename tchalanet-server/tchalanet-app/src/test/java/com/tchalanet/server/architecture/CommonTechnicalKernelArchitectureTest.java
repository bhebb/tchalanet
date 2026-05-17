package com.tchalanet.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CommonTechnicalKernelArchitectureTest {

  private static final Set<String> EXTRACTED_BUSINESS_ENUMS =
      Set.of(
          "AuditAction",
          "AuditActorType",
          "AuditEntityType",
          "IdempotencyScope",
          "NotificationChannel",
          "NotificationType",
          "ResultQuality",
          "SaleOrigin",
          "TicketResultStatus",
          "TicketSaleStatus",
          "TicketSettlementStatus",
          "TicketSyncStatus",
          "UsLotteryProvider");

  private static JavaClasses classes;

  @BeforeAll
  static void setup() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.tchalanet.server");
  }

  @Test
  void commonDoesNotDependOnHeavyRuntimeTechnologies() {
    noClasses()
        .that()
        .resideInAPackage("com.tchalanet.server.common..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.batch..",
            "net.javacrumbs.shedlock..",
            "org.springframework.web.reactive..",
            "org.springframework.web.client..",
            "org.springframework.data.rest..",
            "org.springframework.data.redis..",
            "io.lettuce..",
            "com.querydsl..",
            "com.github.benmanes.caffeine..",
            "com.networknt..")
        .check(classes);
  }

  @Test
  void commonDoesNotDeclareRuntimeAssemblyAnnotations() {
    noClasses()
        .that()
        .resideInAPackage("com.tchalanet.server.common..")
        .should()
        .beAnnotatedWith(org.springframework.boot.autoconfigure.SpringBootApplication.class)
        .orShould()
        .beAnnotatedWith(org.springframework.batch.core.configuration.annotation.EnableBatchProcessing.class)
        .orShould()
        .beAnnotatedWith(org.springframework.scheduling.annotation.EnableScheduling.class)
        .orShould()
        .beAnnotatedWith(org.springframework.cache.annotation.EnableCaching.class)
        .orShould()
        .beAnnotatedWith(org.springframework.data.jpa.repository.config.EnableJpaRepositories.class)
        .orShould()
        .beAnnotatedWith("org.springframework.boot.autoconfigure.domain.EntityScan")
        .orShould()
        .beAnnotatedWith(org.springframework.data.jpa.repository.config.EnableJpaAuditing.class)
        .orShould()
        .beAnnotatedWith(org.springframework.stereotype.Repository.class)
        .orShould()
        .beAnnotatedWith(org.springframework.stereotype.Service.class)
        .orShould()
        .beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
        .orShould()
        .beAnnotatedWith(jakarta.persistence.Entity.class)
        .check(classes);
  }

  @Test
  void extractedBusinessEnumsDoNotReturnToCommonTypesEnums() {
    assertThat(classes)
        .filteredOn(
            javaClass ->
                javaClass
                    .getPackageName()
                    .equals("com.tchalanet.server.common.types.enums"))
        .noneMatch(javaClass -> EXTRACTED_BUSINESS_ENUMS.contains(javaClass.getSimpleName()));
  }
}
