package com.tchalanet.server.common.web.error;

import static com.tchalanet.server.common.http.TchHeaders.X_REQUEST_ID;
import static com.tchalanet.server.common.web.observability.RequiredRequestIdFilter.ATTR_REQUEST_ID;

import com.tchalanet.server.common.observability.TchTraceIds;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.experimental.UtilityClass;

/** Writes a redacted {@code application/problem+json} response for filter-chain failures. */
@UtilityClass
public class ProblemDetailResponseWriter {

  public static void write(
      HttpServletRequest request, HttpServletResponse response, ErrorDescriptor descriptor)
      throws IOException {
    var status = descriptor.expectedStatus();
    response.setStatus(status.value());
    response.setContentType("application/problem+json;charset=UTF-8");
    response.setCharacterEncoding("UTF-8");

    var requestId = request.getAttribute(ATTR_REQUEST_ID);
    if (requestId instanceof String value && !value.isBlank()) {
      response.setHeader(X_REQUEST_ID, value);
    }

    var body = new StringBuilder("{");
    append(body, "type", "about:blank");
    append(body, "title", titleFor(status.value()));
    append(body, "status", status.value());
    append(body, "detail", "Request could not be completed");
    append(body, "code", descriptor.code());
    append(body, "category", descriptor.category().wireValue());
    append(body, "retryPolicy", descriptor.retryPolicy().name());
    append(body, "retryable", descriptor.retryPolicy().retryable());
    append(body, "errorId", UUID.randomUUID().toString());
    if (requestId instanceof String value && !value.isBlank()) {
      append(body, "requestId", value);
    }
    appendOptional(body, "traceId", TchTraceIds.currentTraceId());
    appendOptional(body, "spanId", TchTraceIds.currentSpanId());
    body.append('}');

    response.getWriter().write(body.toString());
  }

  private static String titleFor(int status) {
    return switch (status) {
      case 401 -> "Authentication required";
      case 403 -> "Access denied";
      default -> "Request rejected";
    };
  }

  private static void appendOptional(StringBuilder body, String name, String value) {
    if (value != null && !value.isBlank()) {
      append(body, name, value);
    }
  }

  private static void append(StringBuilder body, String name, String value) {
    appendSeparator(body);
    body.append('"').append(name).append("\":\"").append(escape(value)).append('"');
  }

  private static void append(StringBuilder body, String name, int value) {
    appendSeparator(body);
    body.append('"').append(name).append("\":").append(value);
  }

  private static void append(StringBuilder body, String name, boolean value) {
    appendSeparator(body);
    body.append('"').append(name).append("\":").append(value);
  }

  private static void appendSeparator(StringBuilder body) {
    if (body.length() > 1) {
      body.append(',');
    }
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
