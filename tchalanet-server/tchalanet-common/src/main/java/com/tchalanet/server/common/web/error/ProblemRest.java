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
   * <p>Legacy overloads remain during migration because many callers still pass a message as
   * {@code detail}. New code must use this overload (or a typed domain exception) so clients can
   * translate a stable {@code code} without parsing diagnostics.
   */
  public static ProblemRestException of(HttpStatus status, ErrorDescriptor descriptor) {
    return of(status, descriptor, null);
  }

  /** Code-first variant that preserves the root cause for server logging only. */
  public static ProblemRestException of(
      HttpStatus status, ErrorDescriptor descriptor, Throwable cause) {
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setDetail(descriptor.code());
    pd.setProperty("code", descriptor.code());
    pd.setProperty("category", descriptor.category().name());
    pd.setProperty("retryPolicy", descriptor.retryPolicy().name());
    return cause == null ? new ProblemRestException(pd) : new ProblemRestException(pd, cause);
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
