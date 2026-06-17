package com.tchalanet.server.app.config.openapi;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

final class RequestIdSwaggerIndexTransformer extends SwaggerIndexPageTransformer {

  private static final String SWAGGER_BUNDLE_MARKER = "SwaggerUIBundle({";

  private final String defaultRequestId;

  RequestIdSwaggerIndexTransformer(
      SwaggerUiConfigProperties swaggerUiConfig,
      SwaggerUiOAuthProperties swaggerUiOAuthProperties,
      SwaggerWelcomeCommon swaggerWelcomeCommon,
      ObjectMapperProvider objectMapperProvider,
      String defaultRequestId) {
    super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    this.defaultRequestId = defaultRequestId;
  }

  @Override
  public Resource transform(
      HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
      throws IOException {
    var transformed = super.transform(request, resource, transformerChain);
    var content =
        new String(transformed.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    var withInterceptor = addRequestIdInterceptor(content, defaultRequestId);
    if (content.equals(withInterceptor)) {
      return transformed;
    }
    return new TransformedResource(
        transformed, withInterceptor.getBytes(StandardCharsets.UTF_8));
  }

  static String addRequestIdInterceptor(String content, String defaultRequestId) {
    if (!content.contains(SWAGGER_BUNDLE_MARKER) || content.contains("requestInterceptor:")) {
      return content;
    }
    var escapedDefault =
        defaultRequestId.replace("\\", "\\\\").replace("'", "\\'");
    var interceptor =
        """
        SwaggerUIBundle({
          requestInterceptor: (request) => {
            request.headers = request.headers || {};
            const requestId = request.headers['X-Request-Id'] || request.headers['x-request-id'];
            if (!requestId) {
              request.headers['X-Request-Id'] = '%s';
            }
            return request;
          },
        """
            .formatted(escapedDefault);
    return content.replaceFirst(
        java.util.regex.Pattern.quote(SWAGGER_BUNDLE_MARKER),
        java.util.regex.Matcher.quoteReplacement(interceptor));
  }
}
