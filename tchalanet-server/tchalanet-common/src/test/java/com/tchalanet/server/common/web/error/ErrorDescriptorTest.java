package com.tchalanet.server.common.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorDescriptorTest {

  @Test
  void codeFirstProblemCarriesStableMetadataWithoutMessageInference() {
    var descriptor =
        new ErrorDescriptor(
            "sales.ticket.closed",
            ErrorCategory.BUSINESS_RULE,
            HttpStatus.CONFLICT,
            ErrorRetryPolicy.AFTER_USER_ACTION,
            Set.of(ErrorAudience.WEB_ADMIN),
            Set.of());

    var problem = ProblemRest.of(descriptor).getProblem();

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getDetail()).isEqualTo("Request could not be completed");
    assertThat(problem.getProperties())
        .containsEntry("code", "sales.ticket.closed")
        .containsEntry("category", "business_rule")
        .containsEntry("retryPolicy", "AFTER_USER_ACTION")
        .containsEntry("retryable", true);
  }

  @Test
  void rejectsProseAndDynamicErrorCodeShapes() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new ErrorDescriptor(
                    "Draw is not open",
                    ErrorCategory.BUSINESS_RULE,
                    HttpStatus.CONFLICT,
                    ErrorRetryPolicy.NEVER,
                    Set.of(),
                    Set.of()));
  }

  @Test
  void preservesCauseForServerDiagnosticsWithoutUsingItAsProblemDetail() {
    var descriptor =
        new ErrorDescriptor(
            "pos.receipt.print_options_invalid",
            ErrorCategory.VALIDATION,
            HttpStatus.BAD_REQUEST,
            ErrorRetryPolicy.AFTER_USER_ACTION,
            Set.of(ErrorAudience.WEB_ADMIN),
            Set.of());
    var cause = new IllegalArgumentException("ESC_POS cannot use A4");

    var exception = ProblemRest.badRequest(descriptor, cause);

    assertThat(exception.getCause()).isSameAs(cause);
    assertThat(exception.getProblem().getDetail()).isEqualTo("Request could not be completed");
    assertThat(exception.getProblem().getDetail()).doesNotContain("ESC_POS");
  }

  @Test
  void rejectsUndeclaredOrNonScalarPublicParameters() {
    var descriptor =
        new ErrorDescriptor(
            "limits.selection.blocked",
            ErrorCategory.BUSINESS_RULE,
            HttpStatus.UNPROCESSABLE_ENTITY,
            ErrorRetryPolicy.AFTER_USER_ACTION,
            Set.of(ErrorAudience.WEB_ADMIN),
            Set.of(new ErrorParamSpec("remainingMinorUnits", ErrorParamType.INTEGER)));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> ProblemRest.of(descriptor, java.util.Map.of("tenantId", "internal"), null));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                ProblemRest.of(
                    descriptor, java.util.Map.of("remainingMinorUnits", java.util.Map.of("bad", 1)), null));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> ProblemRest.of(descriptor, java.util.Map.of("remainingMinorUnits", "100"), null));
  }

  @Test
  void registryRejectsDuplicateCodesAndRetainsOwnerDescriptors() {
    var descriptor =
        new ErrorDescriptor(
            "access.context.tenant_required",
            ErrorCategory.BUSINESS_RULE,
            HttpStatus.UNPROCESSABLE_ENTITY,
            ErrorRetryPolicy.AFTER_USER_ACTION,
            Set.of(ErrorAudience.WEB_ADMIN),
            Set.of());

    assertThat(ErrorDescriptorRegistry.validate(
            Set.of(new ErrorDescriptorRegistry.DescriptorRegistration("common.context", descriptor))))
        .containsEntry(descriptor.code(), descriptor);
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                ErrorDescriptorRegistry.validate(
                    java.util.List.of(
                        new ErrorDescriptorRegistry.DescriptorRegistration("first", descriptor),
                        new ErrorDescriptorRegistry.DescriptorRegistration("second", descriptor))))
        .withMessageContaining("Duplicate error descriptor code");
  }

  @Test
  void registryBuildsAnImmutableStartupCatalogFromOwnerContributors() {
    var descriptor =
        new ErrorDescriptor(
            "sales.ticket.closed",
            ErrorCategory.BUSINESS_RULE,
            HttpStatus.CONFLICT,
            ErrorRetryPolicy.AFTER_USER_ACTION,
            Set.of(ErrorAudience.WEB_ADMIN),
            Set.of());
    var registry =
        new ErrorDescriptorRegistry(
            java.util.List.of(
                new ErrorDescriptorContributor() {
                  @Override
                  public String owner() {
                    return "core.sales";
                  }

                  @Override
                  public java.util.Collection<ErrorDescriptor> descriptors() {
                    return Set.of(descriptor);
                  }
                }));

    assertThat(registry.require(descriptor.code())).isEqualTo(descriptor);
    assertThatIllegalArgumentException().isThrownBy(() -> registry.require("sales.unknown"));
    org.assertj.core.api.Assertions.assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> registry.descriptors().put("sales.other", descriptor));
  }
}
