package com.tchalanet.server.common.web.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.web.api.CommonErrorCodes;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
