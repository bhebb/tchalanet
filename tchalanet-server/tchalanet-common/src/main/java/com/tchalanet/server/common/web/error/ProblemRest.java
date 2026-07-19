package com.tchalanet.server.common.web.error;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemRest {

  private ProblemRest() {}

  public static ProblemRestException of(HttpStatus status, String detail) {
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setDetail(detail);
    return new ProblemRestException(pd);
  }

  /**
   * Code-first problem factory for new API errors.
   *
   * <p>Legacy overloads remain during migration because many callers still pass a message as {@code
   * detail}. New code must use this overload (or a typed domain exception) so clients can translate
   * a stable {@code code} without parsing diagnostics.
   */
  public static ProblemRestException of(HttpStatus status, ErrorDescriptor descriptor) {
    return of(status, descriptor, Map.of(), null);
  }

  /** Code-first variant that preserves the root cause for server logging only. */
  public static ProblemRestException of(
      HttpStatus status, ErrorDescriptor descriptor, Throwable cause) {
    return of(status, descriptor, Map.of(), cause);
  }

  /** Preferred code-first factory for new producers. */
  public static ProblemRestException of(ErrorDescriptor descriptor) {
    return of(descriptor.expectedStatus(), descriptor, Map.of(), null);
  }

  /** Preferred code-first factory for new producers with descriptor-approved public parameters. */
  public static ProblemRestException of(
      ErrorDescriptor descriptor, Map<String, ?> params, Throwable cause) {
    return of(descriptor.expectedStatus(), descriptor, params, cause);
  }

  private static ProblemRestException of(
      HttpStatus status, ErrorDescriptor descriptor, Map<String, ?> params, Throwable cause) {
    if (status != descriptor.expectedStatus()) {
      throw new IllegalArgumentException(
          "Descriptor status does not match the status requested by the producer");
    }
    var safeParams = approvedParams(descriptor, params);
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setDetail("Request could not be completed");
    pd.setProperty("code", descriptor.code());
    pd.setProperty("category", descriptor.category().wireValue());
    pd.setProperty("retryPolicy", descriptor.retryPolicy().name());
    pd.setProperty("retryable", descriptor.retryPolicy().retryable());
    if (!safeParams.isEmpty()) {
      pd.setProperty("params", safeParams);
    }
    return cause == null ? new ProblemRestException(pd) : new ProblemRestException(pd, cause);
  }

  private static Map<String, Object> approvedParams(
      ErrorDescriptor descriptor, Map<String, ?> supplied) {
    if (supplied == null || supplied.isEmpty()) {
      return Map.of();
    }
    var approved = new LinkedHashMap<String, Object>();
    for (var entry : supplied.entrySet()) {
      var parameter =
          descriptor.publicParams().stream()
              .filter(spec -> spec.name().equals(entry.getKey()))
              .findFirst()
              .orElse(null);
      if (parameter == null) {
        throw new IllegalArgumentException("Error parameter is not declared by the descriptor");
      }
      if (!parameter.accepts(entry.getValue())) {
        throw new IllegalArgumentException("Error parameter does not match its declared type");
      }
      approved.put(entry.getKey(), entry.getValue());
    }
    return Map.copyOf(approved);
  }

  public static ProblemRestException of(
      HttpStatus status, String detail, Map<String, Object> properties) {
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setDetail(detail);
    pd.setProperties(new LinkedHashMap<>(properties));
    return new ProblemRestException(pd);
  }

  public static ProblemRestException badRequest(String detail) {
    return of(HttpStatus.BAD_REQUEST, detail);
  }

  public static ProblemRestException badRequest(ErrorDescriptor descriptor) {
    return of(HttpStatus.BAD_REQUEST, descriptor);
  }

  public static ProblemRestException badRequest(ErrorDescriptor descriptor, Throwable cause) {
    return of(HttpStatus.BAD_REQUEST, descriptor, cause);
  }

  public static ProblemRestException badRequest(String detail, Throwable cause) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setDetail(detail);
    return new ProblemRestException(pd, cause);
  }

  public static ProblemRestException unauthorized(String detail) {
    return of(HttpStatus.UNAUTHORIZED, detail);
  }

  public static ProblemRestException forbidden(String detail) {
    return of(HttpStatus.FORBIDDEN, detail);
  }

  public static ProblemRestException forbidden(ErrorDescriptor descriptor) {
    return of(HttpStatus.FORBIDDEN, descriptor);
  }

  public static ProblemRestException forbidden(String detail, Map<String, Object> properties) {
    return of(HttpStatus.FORBIDDEN, detail, properties);
  }

  public static ProblemRestException notFound(String detail) {
    return of(HttpStatus.NOT_FOUND, detail);
  }

  public static ProblemRestException notFound(ErrorDescriptor descriptor) {
    return of(HttpStatus.NOT_FOUND, descriptor);
  }

  /** Convenience overload — appends the id to the detail message. */
  public static ProblemRestException notFound(String detail, Object id) {
    return of(HttpStatus.NOT_FOUND, detail + ": " + id);
  }

  public static ProblemRestException conflict(String detail) {
    return of(HttpStatus.CONFLICT, detail);
  }

  public static ProblemRestException conflict(ErrorDescriptor descriptor) {
    return of(HttpStatus.CONFLICT, descriptor);
  }

  public static ProblemRestException unprocessable(String detail) {
    return of(HttpStatus.UNPROCESSABLE_ENTITY, detail);
  }

  public static ProblemRestException unprocessable(ErrorDescriptor descriptor) {
    return of(HttpStatus.UNPROCESSABLE_ENTITY, descriptor);
  }

  public static ProblemRestException internal(String detail) {
    return of(HttpStatus.INTERNAL_SERVER_ERROR, detail);
  }

  public static ProblemRestException internal(ErrorDescriptor descriptor) {
    return of(HttpStatus.INTERNAL_SERVER_ERROR, descriptor);
  }

  public static ProblemRestException internal(String detail, Throwable cause) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    pd.setDetail(detail);
    return new ProblemRestException(pd, cause);
  }
}
