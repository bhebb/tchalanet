package com.tchalanet.server.common.error;

import static com.tchalanet.server.common.constant.TchHeaders.APP_ERROR_VERSION;
import static com.tchalanet.server.common.constant.TchHeaders.X_REQUEST_ID;

import com.tchalanet.server.core.accesscontrol.domain.exception.PermissionsDeniedException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ErrorHandler {

  private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

  /** Toujours RFC7807. Même si le client demande HAL, on renvoie problem+json. */
  private ResponseEntity<ProblemDetail> buildResponse(
      ProblemDetail pd, HttpServletRequest req, HttpStatus status) {

    // sécurité : status cohérent
    pd.setStatus(status.value());

    var headers = new HttpHeaders();
    headers.setContentType(PROBLEM_JSON);

    // utile: exposer traceId dans un header aussi (optionnel)
    var traceId = headerOrNull(req, X_REQUEST_ID);
    if (traceId != null) {
      headers.add(X_REQUEST_ID, traceId);
    }

    return new ResponseEntity<>(pd, headers, status);
  }

  @ExceptionHandler(ProblemRestException.class)
  public ResponseEntity<ProblemDetail> handleProblemRest(
      ProblemRestException ex, HttpServletRequest req) {
    var pd = ex.getProblem();
    decorate(pd, req, ex, false);

    var status = HttpStatus.resolve(pd.getStatus());
    if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

    if (status.is5xxServerError()) {
      log.error(
          "[{}] {} {} - {}",
          status.value(),
          req.getMethod(),
          req.getRequestURI(),
          ex.getMessage(),
          ex);
    } else {
      log.warn(
          "[{}] {} {} - {}", status.value(), req.getMethod(), req.getRequestURI(), pd.getDetail());
    }
    return buildResponse(pd, req, status);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(
      EntityNotFoundException ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setTitle("Resource not found");
    decorate(pd, req, ex, true);
    log.warn("[404] {} {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
    return buildResponse(pd, req, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetail> handleBusiness(
      IllegalStateException ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    pd.setTitle("Business rule violation");
    pd.setDetail(ex.getMessage());
    decorate(pd, req, ex, true);
    log.warn("[422] {} {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
    return buildResponse(pd, req, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleBadRequest(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Validation failed");
    pd.setDetail(
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request"));
    decorate(pd, req, ex, true);
    log.warn("[400] {} {} - {}", req.getMethod(), req.getRequestURI(), pd.getDetail());
    return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Constraint violation");
    pd.setDetail(ex.getMessage());
    decorate(pd, req, ex, true);
    log.warn("[400] {} {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
    return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(PermissionsDeniedException.class)
  public ResponseEntity<ProblemDetail> handlePermissionsDenied(
      PermissionsDeniedException ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    pd.setTitle("Forbidden");
    pd.setDetail("You are not allowed to perform this action.");
    decorate(pd, req, ex, true);

    pd.setProperty("missingPermissions", ex.getMissingPermissions());
    pd.setProperty("tenantId", ex.getTenantId() != null ? ex.getTenantId().toString() : null);
    pd.setProperty("userId", ex.getUserId() != null ? ex.getUserId().toString() : null);

    log.warn(
        "[403] {} {} - missingPermissions={} tenantId={} userId={}",
        req.getMethod(),
        req.getRequestURI(),
        ex.getMissingPermissions(),
        ex.getTenantId(),
        ex.getUserId());

    return buildResponse(pd, req, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleAny(Exception ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    pd.setTitle("Unexpected error");
    pd.setDetail("An unexpected error occurred");
    decorate(pd, req, ex, false);
    log.error("[500] {} {}", req.getMethod(), req.getRequestURI(), ex);
    return buildResponse(pd, req, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private static void decorate(
      ProblemDetail pd, HttpServletRequest req, Throwable ex, boolean verbose) {
    if (pd.getType() == null) {
      pd.setType(URI.create("about:blank"));
    }
    pd.setProperty("timestamp", Instant.now().toString());
    pd.setProperty("method", req.getMethod());
    pd.setProperty("path", req.getRequestURI());

    String traceId = headerOrNull(req, X_REQUEST_ID);
    if (traceId != null) pd.setProperty("traceId", traceId);
    pd.setProperty("errorId", UUID.randomUUID().toString());

    String ver = req.getHeader(APP_ERROR_VERSION);
    if (ver != null) pd.setProperty("version", ver);

    if (verbose) {
      pd.setProperty("cause", ex.getClass().getSimpleName());
    }
  }

  private static String headerOrNull(HttpServletRequest req, String name) {
    var v = req.getHeader(name);
    return (v == null || v.isBlank()) ? null : v;
  }
}
