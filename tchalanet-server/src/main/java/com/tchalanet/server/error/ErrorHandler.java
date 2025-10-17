package com.tchalanet.server.error;

import static com.tchalanet.server.constants.AppConstants.APP_HEADER_ERROR_VERSION;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

  /** Exceptions “métier” levées via ProblemRestException (déjà porteuse d’un ProblemDetail). */
  @ExceptionHandler(ProblemRestException.class)
  public ProblemDetail handleProblemRest(ProblemRestException ex, HttpServletRequest req) {
    var pd = ex.getProblem();
    decorate(pd, req, ex, false);
    // Log au niveau approprié (4xx = warn, 5xx = error)
    if (pd.getStatus() >= 500) {
      log.error("[{}] {} {}", pd.getStatus(), req.getMethod(), req.getRequestURI(), ex);
    } else {
      log.warn(
          "[{}] {} {} – {}", pd.getStatus(), req.getMethod(), req.getRequestURI(), pd.getDetail());
    }
    return pd;
  }

  /** 404 “classique” JPA */
  @ExceptionHandler(EntityNotFoundException.class)
  public ProblemDetail handleNotFound(EntityNotFoundException ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setTitle("Resource not found");
    decorate(pd, req, ex, true);
    log.warn("[404] {} {} – {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
    return pd;
  }

  /** 422 règles métier simples */
  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleBusiness(IllegalStateException ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    pd.setTitle("Business rule violation");
    pd.setDetail(ex.getMessage());
    decorate(pd, req, ex, true);
    log.warn("[422] {} {} – {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
    return pd;
  }

  /** 400 – @Valid sur @RequestBody */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleBadRequest(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Validation failed");
    pd.setDetail(
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request"));
    decorate(pd, req, ex, true);
    log.warn("[400] {} {} – {}", req.getMethod(), req.getRequestURI(), pd.getDetail());
    return pd;
  }

  /** 400 – @Validated sur query/path params */
  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Constraint violation");
    pd.setDetail(ex.getMessage());
    decorate(pd, req, ex, true);
    log.warn("[400] {} {} – {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
    return pd;
  }

  /** Fallback 500 – toujours renvoyer ProblemDetail */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleAny(Exception ex, HttpServletRequest req) {
    var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    pd.setTitle("Unexpected error");
    pd.setDetail("An unexpected error occurred");
    decorate(pd, req, ex, false);
    log.error("[500] {} {}", req.getMethod(), req.getRequestURI(), ex);
    return pd;
  }

  /**
   * Ajoute des métadonnées utiles au ProblemDetail (trace, path, method, ts, version). Si
   * verbose=false, on évite d’exposer la cause/stack au client.
   */
  private static void decorate(
      ProblemDetail pd, HttpServletRequest req, Throwable ex, boolean verbose) {
    // type = URI pseudo-stable par status
    if (pd.getType() == null) {
      pd.setType(URI.create("about:blank"));
    }
    pd.setProperty("timestamp", Instant.now().toString());
    pd.setProperty("method", req.getMethod());
    pd.setProperty("path", req.getRequestURI());

    // Propager un traceId si présent ; sinon générer un errorId
    String traceId = headerOrNull(req, "X-Request-Id");
    if (traceId != null) pd.setProperty("traceId", traceId);
    pd.setProperty("errorId", UUID.randomUUID().toString());

    // Versionnement de payload d’erreur (si tu veux le garder)
    String ver = req.getHeader(APP_HEADER_ERROR_VERSION);
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
