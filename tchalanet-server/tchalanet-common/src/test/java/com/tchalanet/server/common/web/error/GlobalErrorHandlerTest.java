package com.tchalanet.server.common.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.api.CommonErrorCodes;
import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

class GlobalErrorHandlerTest {

  private final GlobalErrorHandler handler = new GlobalErrorHandler();

  @Test
  void mapsBeanValidationToStableFieldViolationsWithoutDefaultMessages() throws Exception {
    var bindingResult = new BeanPropertyBindingResult(new RequestPayload(), "request");
    bindingResult.rejectValue("email", "Email", "email must include an @ character");

    var response =
        handler.handleMethodArgumentNotValid(
            new MethodArgumentNotValidException(methodParameter(), bindingResult), request());

    var problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getDetail()).isEqualTo("Validation failed");
    assertThat(problem.getProperties()).doesNotContainKey("errors");
    assertThat(problem.getProperties().toString()).doesNotContain("email must include");

    var violations = violations(problem.getProperties().get("violations"));
    assertThat(violations)
        .containsExactly(
            Map.of(
                "code", CommonErrorCodes.VALIDATION_INVALID_FORMAT,
                "field", "email",
                "target", "email"));
  }

  @Test
  void doesNotExposeDeserializerMessages() {
    var response =
        handler.handleNotReadable(
            new HttpMessageNotReadableException(
                "unexpected token contains a secret", new MockHttpInputMessage(new byte[0])),
            request());

    var problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getDetail()).isEqualTo("Malformed request body");
    assertThat(problem.getProperties().toString()).doesNotContain("secret");
    assertThat(problem.getProperties()).doesNotContainKey("cause");
  }

  @Test
  void doesNotExposeLegacyIllegalStateMessageOrClassName() {
    var response =
        handler.handleLegacyIllegalState(
            new IllegalStateException("seller PIN 123456 should never leave the server"), request());

    var problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(problem.getDetail()).doesNotContain("123456");
    assertThat(problem.getProperties().toString()).doesNotContain("IllegalStateException");
    assertThat(problem.getProperties()).doesNotContainKey("cause");
  }

  @Test
  void legacyProblemRestProseIsNeverReturnedAndGetsAFallbackCode() {
    var response =
        handler.handleProblemRest(
            ProblemRest.internal("No usage provider found for usageKey: confidential_usage_key"),
            request());

    var problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getDetail()).isEqualTo("Request could not be completed");
    assertThat(problem.getProperties())
        .containsEntry("code", CommonErrorCodes.INTERNAL_UNEXPECTED)
        .containsEntry("category", "unexpected")
        .doesNotContainKey("cause");
    assertThat(problem.toString()).doesNotContain("confidential_usage_key");
  }

  @Test
  void legacyCodeInDetailBecomesStructuredCodeWithoutLeakingProperties() {
    var response =
        handler.handleProblemRest(
            ProblemRest.forbidden(
                "entitlement.feature_required", Map.of("feature", "private_feature_key")), request());

    var problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getDetail()).isEqualTo("Request could not be completed");
    assertThat(problem.getProperties())
        .containsEntry("code", "entitlement.feature_required")
        .doesNotContainKey("feature");
    assertThat(problem.toString()).doesNotContain("private_feature_key");
  }

  @Test
  void securityDenialDoesNotExposeTenantOrPolicyDiagnostics() {
    var response =
        handler.handleAccessDenied(
            new AccessDeniedException("tenant=private-tenant permission=results.override"), request());

    var problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getProperties()).containsEntry("code", CommonErrorCodes.ACCESS_DENIED);
    assertThat(problem.toString())
        .doesNotContain("private-tenant")
        .doesNotContain("results.override");
  }

  @Test
  void persistenceNotFoundDoesNotExposeRlsOrQueryDiagnostics() {
    var response =
        handler.handleJpaNotFound(
            new EntityNotFoundException("RLS tenant=private-tenant query=select secret_pin"), request());

    var problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getProperties()).containsEntry("code", CommonErrorCodes.RESOURCE_NOT_FOUND);
    assertThat(problem.toString())
        .doesNotContain("private-tenant")
        .doesNotContain("secret_pin");
  }

  @Test
  void mapsMissingRequestParameterToAStableFieldViolation() throws Exception {
    var response =
        handler.handleMissingParam(new MissingServletRequestParameterException("amount", "number"), request());

    var problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getDetail()).isEqualTo("Missing request parameter");
    assertThat(violations(problem.getProperties().get("violations")))
        .containsExactly(
            Map.of(
                "code", CommonErrorCodes.VALIDATION_REQUIRED,
                "field", "amount",
                "target", "amount"));
  }

  private static MethodParameter methodParameter() throws NoSuchMethodException {
    Method method = GlobalErrorHandlerTest.class.getDeclaredMethod("requestPayload", String.class);
    return new MethodParameter(method, 0);
  }

  private static MockHttpServletRequest request() {
    return new MockHttpServletRequest("POST", "/api/v1/test");
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, String>> violations(Object property) {
    return (List<Map<String, String>>) property;
  }

  @SuppressWarnings("unused")
  private void requestPayload(String email) {}

  private static final class RequestPayload {
    @SuppressWarnings("unused")
    private String email;

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }
  }
}
