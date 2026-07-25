package com.tchalanet.server.common.web.error;

import static com.tchalanet.server.common.http.TchHeaders.APP_ERROR_VERSION;
import static com.tchalanet.server.common.http.TchHeaders.X_REQUEST_ID;
import static com.tchalanet.server.common.web.observability.RequiredRequestIdFilter.ATTR_REQUEST_ID;

import com.tchalanet.server.common.exception.TchBusinessRuleException;
import com.tchalanet.server.common.exception.TchConflictException;
import com.tchalanet.server.common.exception.TchException;
import com.tchalanet.server.common.exception.TchForbiddenException;
import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.exception.TchValidationException;
import com.tchalanet.server.common.job.gate.BatchDisabledException;
import com.tchalanet.server.common.observability.TchTraceIds;
import com.tchalanet.server.common.web.api.CommonErrorCodes;
import com.tchalanet.server.common.web.api.CommonErrorDescriptors;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalErrorHandler {

  private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;
  private static final Pattern JSON_MAPPING_PATH_SEGMENT =
      Pattern.compile("\\[\\\"([A-Za-z0-9_]+)\\\"\\]|\\[([0-9]+)]");

  @ExceptionHandler(ProblemRestException.class)
  public ResponseEntity<ProblemDetail> handleProblemRest(
      ProblemRestException ex, HttpServletRequest req) {
    var pd = ex.getProblem();

    var status = HttpStatus.resolve(pd.getStatus());
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    normalizeLegacyProblem(pd, status);
    decorate(pd, req, ex, false);

    logByStatus(status, req, pd);
    return buildResponse(pd, req, status);
  }

  @ExceptionHandler(TchNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleTchNotFound(
      TchNotFoundException ex, HttpServletRequest req) {
    var pd = problem(HttpStatus.NOT_FOUND, "Resource not found", ex);
    decorate(pd, req, ex, true);
    log.warn("[404] {} {} - code={}", req.getMethod(), req.getRequestURI(), ex.code());
    return buildResponse(pd, req, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(TchValidationException.class)
  public ResponseEntity<ProblemDetail> handleTchValidation(
      TchValidationException ex, HttpServletRequest req) {
    var pd = problem(HttpStatus.BAD_REQUEST, "Validation failed", ex);
    decorate(pd, req, ex, true);
    log.warn("[400] {} {} - code={}", req.getMethod(), req.getRequestURI(), ex.code());
    return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(TchForbiddenException.class)
  public ResponseEntity<ProblemDetail> handleTchForbidden(
      TchForbiddenException ex, HttpServletRequest req) {
    var pd = problem(HttpStatus.FORBIDDEN, "Forbidden", ex);
    decorate(pd, req, ex, true);
    log.warn("[403] {} {} - code={}", req.getMethod(), req.getRequestURI(), ex.code());
    return buildResponse(pd, req, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(TchConflictException.class)
  public ResponseEntity<ProblemDetail> handleTchConflict(
      TchConflictException ex, HttpServletRequest req) {
    var pd = problem(HttpStatus.CONFLICT, "Conflict", ex);
    decorate(pd, req, ex, true);
    log.warn("[409] {} {} - code={}", req.getMethod(), req.getRequestURI(), ex.code());
    return buildResponse(pd, req, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(TchBusinessRuleException.class)
  public ResponseEntity<ProblemDetail> handleTchBusinessRule(
      TchBusinessRuleException ex, HttpServletRequest req) {
    var pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation", ex);
    decorate(pd, req, ex, true);
    log.warn("[422] {} {} - code={}", req.getMethod(), req.getRequestURI(), ex.code());
    return buildResponse(pd, req, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(BatchDisabledException.class)
  public ResponseEntity<ProblemDetail> handleBatchDisabled(
      BatchDisabledException ex, HttpServletRequest req) {
    var pd = problem(HttpStatus.LOCKED, "Batch job disabled", ex);
    pd.setProperty("jobKey", ex.jobKey());

    decorate(pd, req, ex, true);

    log.warn("[423] {} {} - code={}", req.getMethod(), req.getRequestURI(), ex.code());

    return buildResponse(pd, req, HttpStatus.LOCKED);
  }

  /**
   * Fallback for other TchException subclasses not mapped explicitly.
   *
   * <p>Use explicit subclasses above when the HTTP status is known.
   */
  @ExceptionHandler(TchException.class)
  public ResponseEntity<ProblemDetail> handleGenericTchException(
      TchException ex, HttpServletRequest req) {
    var pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Application error", ex);
    decorate(pd, req, ex, true);

    log.warn("[422] {} {} - code={}", req.getMethod(), req.getRequestURI(), ex.code());

    return buildResponse(pd, req, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  /**
   * Temporary legacy support.
   *
   * <p>Prefer throwing TchNotFoundException or ProblemRestException.notFound(...).
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleJpaNotFound(
      EntityNotFoundException ex, HttpServletRequest req) {
    var pd = problem(CommonErrorDescriptors.RESOURCE_NOT_FOUND);
    decorate(pd, req, ex, false);

    log.warn(
        "[404] {} {} - code={}",
        req.getMethod(),
        req.getRequestURI(),
        CommonErrorCodes.RESOURCE_NOT_FOUND);

    return buildResponse(pd, req, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    var pd = problem(CommonErrorDescriptors.VALIDATION_FAILED);
    pd.setProperty("violations", fieldViolations(ex));

    decorate(pd, req, ex, true);

    log.warn(
        "[400] {} {} - validation fields={}",
        req.getMethod(),
        req.getRequestURI(),
        ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField())
            .toList());

    return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
  }

  /**
   * Malformed or unparseable request body (Jackson deserialization failure, etc.).
   *
   * <p>Without this handler Spring's DefaultHandlerExceptionResolver short-circuits to the
   * BasicErrorController, leaking a bare {@code {"timestamp",...,"status":400}} response that
   * bypasses our problem+json contract. The parsing failure remains in server logs only; arbitrary
   * request content or Jackson/provider prose is never returned as a client message.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest req) {
    var pd = problem(CommonErrorDescriptors.REQUEST_NOT_READABLE);

    decorate(pd, req, ex, true);

    log.warn(
        "[400] {} {} - not readable cause={} target={}",
        req.getMethod(),
        req.getRequestURI(),
        ex.getMostSpecificCause().getClass().getSimpleName(),
        safeMappingTarget(ex.getMostSpecificCause()));

    return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
  }

  /**
   * Keeps the log useful for contract debugging without logging a rejected value, request body, or
   * Jackson diagnostic prose.
   */
  private static String safeMappingTarget(Throwable cause) {
    if (!isJsonMappingException(cause)) {
      return "root";
    }

    final String pathReference;
    try {
      pathReference = String.valueOf(cause.getClass().getMethod("getPathReference").invoke(cause));
    } catch (ReflectiveOperationException ignored) {
      return "root";
    }

    var target = new StringBuilder();
    var matcher = JSON_MAPPING_PATH_SEGMENT.matcher(pathReference);
    while (matcher.find()) {
      var field = matcher.group(1);
      if (field != null) {
        if (target.length() > 0) {
          target.append('.');
        }
        target.append(field);
      } else {
        target.append('[').append(matcher.group(2)).append(']');
      }
    }
    return target.isEmpty() ? "root" : target.toString();
  }

  private static boolean isJsonMappingException(Throwable cause) {
    for (Class<?> type = cause.getClass(); type != null; type = type.getSuperclass()) {
      if ("com.fasterxml.jackson.databind.JsonMappingException".equals(type.getName())) {
        return true;
      }
    }
    return false;
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ProblemDetail> handleMissingParam(
      MissingServletRequestParameterException ex, HttpServletRequest req) {
    var pd = problem(CommonErrorDescriptors.REQUEST_MISSING_PARAMETER);
    pd.setProperty(
        "violations",
        List.of(
            Map.of(
                "code", CommonErrorCodes.VALIDATION_REQUIRED,
                "field", ex.getParameterName(),
                "target", ex.getParameterName())));

    decorate(pd, req, ex, true);

    log.warn(
        "[400] {} {} - missing parameter={}",
        req.getMethod(),
        req.getRequestURI(),
        ex.getParameterName());

    return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
    var pd = problem(CommonErrorDescriptors.REQUEST_TYPE_MISMATCH);
    pd.setProperty(
        "violations",
        List.of(
            Map.of(
                "code", CommonErrorCodes.VALIDATION_INVALID_FORMAT,
                "field", ex.getName(),
                "target", ex.getName())));

    decorate(pd, req, ex, true);

    log.warn(
        "[400] {} {} - type mismatch parameter={}",
        req.getMethod(),
        req.getRequestURI(),
        ex.getName());

    return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest req) {
    var pd = problem(CommonErrorDescriptors.VALIDATION_CONSTRAINT_VIOLATION);
    pd.setProperty("violations", constraintViolations(ex));

    decorate(pd, req, ex, true);

    log.warn(
        "[400] {} {} - constraint violations={}",
        req.getMethod(),
        req.getRequestURI(),
        ex.getConstraintViolations().size());

    return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
  }

  /**
   * Keep this only during migration.
   *
   * <p>IllegalStateException can be a bug, so long-term it should not be mapped blindly to 422.
   */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetail> handleLegacyIllegalState(
      IllegalStateException ex, HttpServletRequest req) {
    var pd = problem(CommonErrorDescriptors.INTERNAL_UNEXPECTED);

    decorate(pd, req, ex, false);

    logUnexpected(req, pd, ex);

    return buildResponse(pd, req, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Method-security (@PreAuthorize) denials throw Spring Security's {@link AccessDeniedException}
   * (including its subclass {@code AuthorizationDeniedException}) <em>inside</em> the dispatcher,
   * so they reach this advice instead of the filter-chain translator. Map them to a clean 403 —
   * otherwise the catch-all below turns every forbidden access into a 500.
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest req) {
    var pd = problem(CommonErrorDescriptors.ACCESS_DENIED);

    decorate(pd, req, ex, true);

    log.warn("[403] {} {} - access denied", req.getMethod(), req.getRequestURI());

    return buildResponse(pd, req, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleAny(Exception ex, HttpServletRequest req) {
    var pd = problem(CommonErrorDescriptors.INTERNAL_UNEXPECTED);

    decorate(pd, req, ex, false);

    logUnexpected(req, pd, ex);

    return buildResponse(pd, req, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private static ProblemDetail problem(HttpStatus status, String title, TchException ex) {
    var pd = ProblemDetail.forStatusAndDetail(status, "Request could not be completed");
    pd.setTitle(title);
    pd.setProperty("code", ex.code());
    pd.setProperty("category", categoryFor(status));
    pd.setProperty("retryPolicy", ErrorRetryPolicy.NEVER.name());
    pd.setProperty("retryable", false);
    return pd;
  }

  private static ProblemDetail problem(ErrorDescriptor descriptor) {
    var pd = ProblemDetail.forStatus(descriptor.expectedStatus());
    pd.setDetail("Request could not be completed");
    pd.setProperty("code", descriptor.code());
    pd.setProperty("category", descriptor.category().wireValue());
    pd.setProperty("retryPolicy", descriptor.retryPolicy().name());
    pd.setProperty("retryable", descriptor.retryPolicy().retryable());
    return pd;
  }

  /**
   * Migration firewall for message-first {@link ProblemRestException} producers.
   *
   * <p>Legacy producers used {@code detail} for either a stable code or arbitrary diagnostics. A
   * complete descriptor contract is retained; anything else is reduced to a stable code and neutral
   * detail before it crosses the HTTP boundary.
   */
  private static void normalizeLegacyProblem(ProblemDetail pd, HttpStatus status) {
    var properties = pd.getProperties();
    if (properties != null
        && properties.containsKey("code")
        && properties.containsKey("category")
        && properties.containsKey("retryPolicy")
        && properties.containsKey("retryable")) {
      return;
    }

    var existingCode = properties == null ? null : properties.get("code");
    var code =
        existingCode instanceof String value && ErrorDescriptor.isValidCode(value)
            ? value
            : ErrorDescriptor.isValidCode(pd.getDetail()) ? pd.getDetail() : fallbackCode(status);

    pd.setTitle(titleFor(status));
    pd.setDetail("Request could not be completed");
    pd.setProperties(new LinkedHashMap<>());
    pd.setProperty("code", code);
    pd.setProperty("category", categoryFor(status));
    pd.setProperty("retryPolicy", ErrorRetryPolicy.NEVER.name());
    pd.setProperty("retryable", false);
  }

  private static String fallbackCode(HttpStatus status) {
    return switch (status) {
      case NOT_FOUND -> CommonErrorCodes.RESOURCE_NOT_FOUND;
      case SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT, BAD_GATEWAY ->
          CommonErrorCodes.SERVICE_UNAVAILABLE;
      case INTERNAL_SERVER_ERROR -> CommonErrorCodes.INTERNAL_UNEXPECTED;
      default -> CommonErrorCodes.REQUEST_REJECTED;
    };
  }

  private static String titleFor(HttpStatus status) {
    return switch (status) {
      case NOT_FOUND -> "Resource not found";
      case FORBIDDEN -> "Access denied";
      case UNAUTHORIZED -> "Authentication required";
      case CONFLICT -> "Request conflict";
      case SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT, BAD_GATEWAY -> "Service unavailable";
      case INTERNAL_SERVER_ERROR -> "Unexpected error";
      default -> "Request rejected";
    };
  }

  private ResponseEntity<ProblemDetail> buildResponse(
      ProblemDetail pd, HttpServletRequest req, HttpStatus status) {
    if (isEventStreamRequest(req)) {
      // SSE responses cannot serialize ProblemDetail (converter mismatch on text/event-stream).
      return ResponseEntity.status(status).build();
    }

    pd.setStatus(status.value());

    var headers = new HttpHeaders();
    headers.setContentType(PROBLEM_JSON);

    // TraceResponseHeaderFilter handles response headers globally; this is a fallback
    // for error responses that may bypass normal filter flow.
    var requestId = (String) req.getAttribute(ATTR_REQUEST_ID);
    if (requestId == null) {
      requestId = headerOrNull(req, X_REQUEST_ID);
    }
    if (requestId != null) {
      headers.add(X_REQUEST_ID, requestId);
    }

    return new ResponseEntity<>(pd, headers, status);
  }

  private static boolean isEventStreamRequest(HttpServletRequest req) {
    var accept = req.getHeader(HttpHeaders.ACCEPT);
    return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
  }

  private static void decorate(
      ProblemDetail pd, HttpServletRequest req, Throwable ex, boolean verbose) {
    if (pd.getType() == null) {
      pd.setType(URI.create("about:blank"));
    }

    pd.setProperty("timestamp", Instant.now().toString());
    pd.setProperty("method", req.getMethod());
    pd.setProperty("path", req.getRequestURI());
    pd.setProperty("errorId", UUID.randomUUID().toString());

    // requestId: prefer the validated attribute set by RequiredRequestIdFilter
    var requestId = (String) req.getAttribute(ATTR_REQUEST_ID);
    if (requestId == null) {
      requestId = headerOrNull(req, X_REQUEST_ID);
    }
    if (requestId != null) {
      pd.setProperty("requestId", requestId);
    }

    // traceId/spanId from Micrometer Tracing (populated once B3 OTel bridge is active)
    var traceId = TchTraceIds.currentTraceId();
    if (traceId != null) {
      pd.setProperty("traceId", traceId);
    }
    var spanId = TchTraceIds.currentSpanId();
    if (spanId != null) {
      pd.setProperty("spanId", spanId);
    }

    var version = req.getHeader(APP_ERROR_VERSION);
    if (version != null && !version.isBlank()) {
      pd.setProperty("version", version.trim());
    }

    var properties = pd.getProperties();
    if (properties == null || !properties.containsKey("category")) {
      pd.setProperty("category", categoryFor(HttpStatus.valueOf(pd.getStatus())));
    }
    if (properties == null || !properties.containsKey("retryPolicy")) {
      pd.setProperty("retryPolicy", ErrorRetryPolicy.NEVER.name());
    }
    if (properties == null || !properties.containsKey("retryable")) {
      pd.setProperty("retryable", false);
    }
  }

  private static String categoryFor(HttpStatus status) {
    return switch (status) {
      case UNAUTHORIZED -> ErrorCategory.AUTHENTICATION.wireValue();
      case FORBIDDEN -> ErrorCategory.AUTHORIZATION.wireValue();
      case NOT_FOUND -> ErrorCategory.NOT_FOUND.wireValue();
      case CONFLICT -> ErrorCategory.CONFLICT.wireValue();
      case TOO_MANY_REQUESTS -> ErrorCategory.RATE_LIMITED.wireValue();
      case BAD_REQUEST, UNPROCESSABLE_ENTITY -> ErrorCategory.VALIDATION.wireValue();
      case SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> ErrorCategory.SERVICE_UNAVAILABLE.wireValue();
      default -> ErrorCategory.UNEXPECTED.wireValue();
    };
  }

  private static void logByStatus(HttpStatus status, HttpServletRequest req, ProblemDetail pd) {
    var code = errorCode(pd);
    if (status.is5xxServerError()) {
      log.error("[{}] {} {} - code={}", status.value(), req.getMethod(), req.getRequestURI(), code);
      return;
    }

    log.warn("[{}] {} {} - code={}", status.value(), req.getMethod(), req.getRequestURI(), code);
  }

  private static String errorCode(ProblemDetail pd) {
    var properties = pd.getProperties();
    if (properties != null && properties.get("code") instanceof String code) {
      return code;
    }
    return CommonErrorCodes.INTERNAL_UNEXPECTED;
  }

  private static String errorId(ProblemDetail pd) {
    var properties = pd.getProperties();
    if (properties != null && properties.get("errorId") instanceof String errorId) {
      return errorId;
    }
    return "unknown";
  }

  /**
   * Logs a debuggable stack without putting exception messages, rejected values, or payload
   * fragments into application logs. The {@code errorId} ties this diagnostic to the client-safe
   * ProblemDetail.
   */
  private static void logUnexpected(HttpServletRequest req, ProblemDetail pd, Throwable ex) {
    log.error(
        "[500] {} {} - errorId={} code={} exception={} stack={}",
        req.getMethod(),
        req.getRequestURI(),
        errorId(pd),
        CommonErrorCodes.INTERNAL_UNEXPECTED,
        ex.getClass().getName(),
        safeStackTrace(ex));
  }

  private static String safeStackTrace(Throwable throwable) {
    var stack = new StringBuilder();
    var current = throwable;
    for (int depth = 0; current != null && depth < 4; depth++, current = current.getCause()) {
      if (depth > 0) {
        stack.append(" caused-by=");
      }
      stack.append(current.getClass().getName()).append('[');
      var frames = current.getStackTrace();
      for (int index = 0; index < frames.length && index < 32; index++) {
        if (index > 0) {
          stack.append(" <- ");
        }
        stack.append(frames[index]);
      }
      stack.append(']');
    }
    return stack.toString();
  }

  private static String headerOrNull(HttpServletRequest req, String name) {
    var value = req.getHeader(name);
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static List<Map<String, String>> fieldViolations(MethodArgumentNotValidException ex) {
    return ex.getBindingResult().getFieldErrors().stream()
        .map(
            fieldError ->
                Map.of(
                    "code", validationCode(fieldError.getCode()),
                    "field", fieldError.getField(),
                    "target", fieldError.getField()))
        .toList();
  }

  private static List<Map<String, String>> constraintViolations(ConstraintViolationException ex) {
    return ex.getConstraintViolations().stream()
        .map(
            violation -> {
              var target = violation.getPropertyPath().toString();
              return Map.of(
                  "code",
                      validationCode(
                          violation
                              .getConstraintDescriptor()
                              .getAnnotation()
                              .annotationType()
                              .getSimpleName()),
                  "field", target,
                  "target", target);
            })
        .toList();
  }

  private static String validationCode(String constraintCode) {
    if (constraintCode == null) {
      return CommonErrorCodes.VALIDATION_FAILED;
    }

    return switch (constraintCode) {
      case "NotBlank", "NotEmpty", "NotNull" -> CommonErrorCodes.VALIDATION_REQUIRED;
      case "Email", "Pattern" -> CommonErrorCodes.VALIDATION_INVALID_FORMAT;
      case "DecimalMax", "DecimalMin", "Max", "Min", "Size" ->
          CommonErrorCodes.VALIDATION_OUT_OF_RANGE;
      default -> CommonErrorCodes.VALIDATION_FAILED;
    };
  }
}
