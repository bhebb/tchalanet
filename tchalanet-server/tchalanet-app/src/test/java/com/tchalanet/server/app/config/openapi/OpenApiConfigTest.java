package com.tchalanet.server.app.config.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OpenApiConfigTest {

  @Test
  void exposesOnlyProviderNeutralBearerAuthentication() {
    var config = new OpenApiConfig();
    ReflectionTestUtils.setField(config, "basePath", "/api");
    ReflectionTestUtils.setField(config, "apiVersion", "v1");
    ReflectionTestUtils.setField(config, "apiBaseUrl", "http://localhost:8083");
    ReflectionTestUtils.setField(config, "swaggerDefaultRequestId", "tch_req_swagger_test_0001");

    var openApi = config.customOpenAPI();
    var schemes = openApi.getComponents().getSecuritySchemes();

    assertThat(schemes).containsOnlyKeys("bearerAuth");
    assertThat(schemes.get("bearerAuth").getType()).isEqualTo(SecurityScheme.Type.HTTP);
    assertThat(schemes.get("bearerAuth").getScheme()).isEqualTo("bearer");
    assertThat(schemes.get("bearerAuth").getBearerFormat()).isEqualTo("JWT");
    assertThat(schemes.get("bearerAuth").getDescription())
        .doesNotContain("Firebase", "Keycloak", "Clerk");
    assertThat(openApi.getSecurity())
        .singleElement()
        .satisfies(requirement -> assertThat(requirement).containsOnlyKeys("bearerAuth"));
  }

  @Test
  void addsRequiredEditableRequestIdHeaderToEveryOperation() {
    var config = new OpenApiConfig();
    ReflectionTestUtils.setField(config, "swaggerDefaultRequestId", "tch_req_swagger_test_0001");

    var operation = config.requestIdHeaderCustomizer().customize(new Operation(), null);

    assertThat(operation.getParameters())
        .singleElement()
        .satisfies(
            parameter -> {
              assertThat(parameter.getName()).isEqualTo("X-Request-Id");
              assertThat(parameter.getIn()).isEqualTo("header");
              assertThat(parameter.getRequired()).isTrue();
              assertThat(parameter.getExample()).isEqualTo("tch_req_swagger_test_0001");
              assertThat(parameter.getSchema().getDefault())
                  .isEqualTo("tch_req_swagger_test_0001");
            });
  }

  @Test
  void doesNotDuplicateAnExplicitRequestIdHeader() {
    var config = new OpenApiConfig();
    ReflectionTestUtils.setField(config, "swaggerDefaultRequestId", "tch_req_swagger_test_0001");
    var operation =
        new Operation()
            .addParametersItem(
                new io.swagger.v3.oas.models.parameters.Parameter()
                    .in("header")
                    .name("X-Request-Id"));

    config.requestIdHeaderCustomizer().customize(operation, null);

    assertThat(operation.getParameters()).hasSize(1);
  }

  @Test
  void swaggerUiSendsDefaultRequestIdWithoutOverwritingAnEditedValue() {
    var initializer = "const ui = SwaggerUIBundle({ url: '/openapi' });";

    var transformed =
        RequestIdSwaggerIndexTransformer.addRequestIdInterceptor(
            initializer, "tch_req_swagger_test_0001");

    assertThat(transformed)
        .contains("request.headers['X-Request-Id'] = 'tch_req_swagger_test_0001'")
        .contains(
            "request.headers['X-Request-Id'] || request.headers['x-request-id']");
    assertThat(
            RequestIdSwaggerIndexTransformer.addRequestIdInterceptor(
                transformed, "tch_req_swagger_test_0001"))
        .isEqualTo(transformed);
  }
}
