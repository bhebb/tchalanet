package com.tchalanet.server.app.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String REQUEST_ID_HEADER = "X-Request-Id";

  @Value("${app.base-path:/api}")
  private String basePath;

  @Value("${app.api-version:v1}")
  private String apiVersion;

  @Value("${app.api-base-url:http://localhost:8083}")
  private String apiBaseUrl;

  @Value("${tch.observability.request-id.swagger-default:tch_req_swagger_local_0001}")
  private String swaggerDefaultRequestId;

  @Bean
  public OpenAPI customOpenAPI() {
    var prefix = normalizePrefix(basePath);
    var serverUrl = normalizeUrl(apiBaseUrl + prefix + "/" + apiVersion);

    var bearerScheme =
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description(
                "Paste an ID token from the configured identity provider. "
                    + "Tchalanet resolves AppUser, roles, permissions, "
                    + "tenant context, and RLS server-side.");

    var components = new Components().addSecuritySchemes("bearerAuth", bearerScheme);
    var security = new SecurityRequirement().addList("bearerAuth");

    return new OpenAPI()
        .servers(List.of(new Server().url(serverUrl)))
        .components(components)
        .addSecurityItem(security);
  }

  @Bean
  OperationCustomizer requestIdHeaderCustomizer() {
    return (operation, handlerMethod) -> {
      var alreadyDeclared =
          operation.getParameters() != null
              && operation.getParameters().stream()
                  .anyMatch(parameter -> REQUEST_ID_HEADER.equalsIgnoreCase(parameter.getName()));
      if (!alreadyDeclared) {
        operation.addParametersItem(
            new Parameter()
                .in("header")
                .name(REQUEST_ID_HEADER)
                .required(true)
                .description("Required request correlation identifier. Editable before sending.")
                .schema(
                    new StringSchema()
                        .pattern("^[A-Za-z0-9._:\\-]{8,96}$")
                        ._default(swaggerDefaultRequestId))
                .example(swaggerDefaultRequestId));
      }
      return operation;
    };
  }

  @Bean
  SwaggerIndexTransformer requestIdSwaggerIndexTransformer(
      SwaggerUiConfigProperties swaggerUiConfig,
      SwaggerUiOAuthProperties swaggerUiOAuthProperties,
      SwaggerWelcomeCommon swaggerWelcomeCommon,
      ObjectMapperProvider objectMapperProvider) {
    return new RequestIdSwaggerIndexTransformer(
        swaggerUiConfig,
        swaggerUiOAuthProperties,
        swaggerWelcomeCommon,
        objectMapperProvider,
        swaggerDefaultRequestId);
  }

  private static String normalizePrefix(String basePath) {
    if (basePath == null || basePath.isBlank()) return "";
    var p = basePath.startsWith("/") ? basePath : "/" + basePath;
    return p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
  }

  private static String normalizeUrl(String url) {
    // normalize accidental double slashes (keep http(s)://)
    return url.replaceAll("(?<!https?:)/{2,}", "/");
  }
}
