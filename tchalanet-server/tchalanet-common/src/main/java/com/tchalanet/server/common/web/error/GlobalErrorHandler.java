package com.tchalanet.server.common.web.error;

import com.tchalanet.server.common.exception.TchBusinessRuleException;
import com.tchalanet.server.common.exception.TchConflictException;
import com.tchalanet.server.common.exception.TchException;
import com.tchalanet.server.common.exception.TchForbiddenException;
import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.exception.TchValidationException;
import com.tchalanet.server.common.job.gate.BatchDisabledException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static com.tchalanet.server.common.http.TchHeaders.APP_ERROR_VERSION;
import static com.tchalanet.server.common.http.TchHeaders.X_REQUEST_ID;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalErrorHandler {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @ExceptionHandler(ProblemRestException.class)
    public ResponseEntity<ProblemDetail> handleProblemRest(
        ProblemRestException ex,
        HttpServletRequest req
    ) {
        var pd = ex.getProblem();
        decorate(pd, req, ex, false);

        var status = HttpStatus.resolve(pd.getStatus());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        logByStatus(status, req, pd.getDetail(), ex);
        return buildResponse(pd, req, status);
    }

    @ExceptionHandler(TchNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleTchNotFound(
        TchNotFoundException ex,
        HttpServletRequest req
    ) {
        var pd = problem(HttpStatus.NOT_FOUND, "Resource not found", ex);
        decorate(pd, req, ex, true);
        log.warn("[404] {} {} - code={} message={}",
            req.getMethod(), req.getRequestURI(), ex.code(), ex.getMessage());
        return buildResponse(pd, req, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TchValidationException.class)
    public ResponseEntity<ProblemDetail> handleTchValidation(
        TchValidationException ex,
        HttpServletRequest req
    ) {
        var pd = problem(HttpStatus.BAD_REQUEST, "Validation failed", ex);
        decorate(pd, req, ex, true);
        log.warn("[400] {} {} - code={} message={}",
            req.getMethod(), req.getRequestURI(), ex.code(), ex.getMessage());
        return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TchForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleTchForbidden(
        TchForbiddenException ex,
        HttpServletRequest req
    ) {
        var pd = problem(HttpStatus.FORBIDDEN, "Forbidden", ex);
        decorate(pd, req, ex, true);
        log.warn("[403] {} {} - code={} message={}",
            req.getMethod(), req.getRequestURI(), ex.code(), ex.getMessage());
        return buildResponse(pd, req, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(TchConflictException.class)
    public ResponseEntity<ProblemDetail> handleTchConflict(
        TchConflictException ex,
        HttpServletRequest req
    ) {
        var pd = problem(HttpStatus.CONFLICT, "Conflict", ex);
        decorate(pd, req, ex, true);
        log.warn("[409] {} {} - code={} message={}",
            req.getMethod(), req.getRequestURI(), ex.code(), ex.getMessage());
        return buildResponse(pd, req, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TchBusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleTchBusinessRule(
        TchBusinessRuleException ex,
        HttpServletRequest req
    ) {
        var pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation", ex);
        decorate(pd, req, ex, true);
        log.warn("[422] {} {} - code={} message={}",
            req.getMethod(), req.getRequestURI(), ex.code(), ex.getMessage());
        return buildResponse(pd, req, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(BatchDisabledException.class)
    public ResponseEntity<ProblemDetail> handleBatchDisabled(
        BatchDisabledException ex,
        HttpServletRequest req
    ) {
        var pd = problem(HttpStatus.LOCKED, "Batch job disabled", ex);
        pd.setProperty("jobKey", ex.jobKey());

        decorate(pd, req, ex, true);

        log.warn("[423] {} {} - code={} jobKey={}",
            req.getMethod(), req.getRequestURI(), ex.code(), ex.jobKey());

        return buildResponse(pd, req, HttpStatus.LOCKED);
    }

    /**
     * Fallback for other TchException subclasses not mapped explicitly.
     *
     * <p>Use explicit subclasses above when the HTTP status is known.
     */
    @ExceptionHandler(TchException.class)
    public ResponseEntity<ProblemDetail> handleGenericTchException(
        TchException ex,
        HttpServletRequest req
    ) {
        var pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Application error", ex);
        decorate(pd, req, ex, true);

        log.warn("[422] {} {} - code={} message={}",
            req.getMethod(), req.getRequestURI(), ex.code(), ex.getMessage());

        return buildResponse(pd, req, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Temporary legacy support.
     *
     * <p>Prefer throwing TchNotFoundException or ProblemRestException.notFound(...).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleJpaNotFound(
        EntityNotFoundException ex,
        HttpServletRequest req
    ) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource not found");
        decorate(pd, req, ex, true);

        log.warn("[404] {} {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage());

        return buildResponse(pd, req, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpServletRequest req
    ) {
        var detail = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request");

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Validation failed");
        pd.setProperty("code", "validation.failed");

        decorate(pd, req, ex, true);

        log.warn("[400] {} {} - {}", req.getMethod(), req.getRequestURI(), detail);

        return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
    }

    /**
     * Malformed or unparseable request body (Jackson deserialization failure, etc.).
     *
     * <p>Without this handler Spring's DefaultHandlerExceptionResolver short-circuits to the
     * BasicErrorController, leaking a bare {@code {"timestamp",...,"status":400}} response that
     * bypasses our problem+json contract. We surface the most specific cause message instead.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(
        HttpMessageNotReadableException ex,
        HttpServletRequest req
    ) {
        var cause = ex.getMostSpecificCause();
        var detail = cause != null ? cause.getMessage() : ex.getMessage();

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Malformed request body");
        pd.setProperty("code", "request.not_readable");

        decorate(pd, req, ex, true);

        log.warn("[400] {} {} - not readable: {}", req.getMethod(), req.getRequestURI(), detail);

        return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParam(
        MissingServletRequestParameterException ex,
        HttpServletRequest req
    ) {
        var detail = "Missing required request parameter: " + ex.getParameterName();

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Missing request parameter");
        pd.setProperty("code", "request.missing_parameter");

        decorate(pd, req, ex, true);

        log.warn("[400] {} {} - {}", req.getMethod(), req.getRequestURI(), detail);

        return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest req
    ) {
        var detail = "Invalid value for '" + ex.getName() + "'";
        var cause = ex.getMostSpecificCause();
        if (cause != null && cause.getMessage() != null) {
            detail += ": " + cause.getMessage();
        }

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Type mismatch");
        pd.setProperty("code", "request.type_mismatch");

        decorate(pd, req, ex, true);

        log.warn("[400] {} {} - {}", req.getMethod(), req.getRequestURI(), detail);

        return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest req
    ) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Constraint violation");
        pd.setProperty("code", "validation.constraint_violation");

        decorate(pd, req, ex, true);

        log.warn("[400] {} {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage());

        return buildResponse(pd, req, HttpStatus.BAD_REQUEST);
    }

    /**
     * Keep this only during migration.
     *
     * <p>IllegalStateException can be a bug, so long-term it should not be mapped blindly to 422.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleLegacyIllegalState(
        IllegalStateException ex,
        HttpServletRequest req
    ) {
        var pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,
            ex.getMessage()
        );
        pd.setTitle("Business rule violation");
        pd.setProperty("code", "business_rule.violation");

        decorate(pd, req, ex, true);

        log.warn("[422] {} {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage());

        return buildResponse(pd, req, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Method-security (@PreAuthorize) denials throw Spring Security's
     * {@link AccessDeniedException} (including its subclass
     * {@code AuthorizationDeniedException}) <em>inside</em> the dispatcher, so they
     * reach this advice instead of the filter-chain translator. Map them to a clean
     * 403 — otherwise the catch-all below turns every forbidden access into a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest req
    ) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        pd.setTitle("Forbidden");
        pd.setProperty("code", "access.denied");

        decorate(pd, req, ex, true);

        log.warn("[403] {} {} - access denied", req.getMethod(), req.getRequestURI());

        return buildResponse(pd, req, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAny(
        Exception ex,
        HttpServletRequest req
    ) {
        var pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        pd.setTitle("Unexpected error");
        pd.setProperty("code", "internal.unexpected");

        decorate(pd, req, ex, false);

        log.error("[500] {} {}", req.getMethod(), req.getRequestURI(), ex);

        return buildResponse(pd, req, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static ProblemDetail problem(
        HttpStatus status,
        String title,
        TchException ex
    ) {
        var pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle(title);
        pd.setProperty("code", ex.code());
        return pd;
    }

    private ResponseEntity<ProblemDetail> buildResponse(
        ProblemDetail pd,
        HttpServletRequest req,
        HttpStatus status
    ) {
        if (isEventStreamRequest(req)) {
            // SSE responses cannot serialize ProblemDetail (converter mismatch on text/event-stream).
            return ResponseEntity.status(status).build();
        }

        pd.setStatus(status.value());

        var headers = new HttpHeaders();
        headers.setContentType(PROBLEM_JSON);

        var traceId = headerOrNull(req, X_REQUEST_ID);
        if (traceId != null) {
            headers.add(X_REQUEST_ID, traceId);
        }

        return new ResponseEntity<>(pd, headers, status);
    }

    private static boolean isEventStreamRequest(HttpServletRequest req) {
        var accept = req.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private static void decorate(
        ProblemDetail pd,
        HttpServletRequest req,
        Throwable ex,
        boolean verbose
    ) {
        if (pd.getType() == null) {
            pd.setType(URI.create("about:blank"));
        }

        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("method", req.getMethod());
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("errorId", UUID.randomUUID().toString());

        var traceId = headerOrNull(req, X_REQUEST_ID);
        if (traceId != null) {
            pd.setProperty("traceId", traceId);
        }

        var version = req.getHeader(APP_ERROR_VERSION);
        if (version != null && !version.isBlank()) {
            pd.setProperty("version", version.trim());
        }

        if (verbose) {
            pd.setProperty("cause", ex.getClass().getSimpleName());
        }
    }

    private static void logByStatus(
        HttpStatus status,
        HttpServletRequest req,
        String detail,
        Throwable ex
    ) {
        if (status.is5xxServerError()) {
            log.error("[{}] {} {} - {}",
                status.value(), req.getMethod(), req.getRequestURI(), detail, ex);
            return;
        }

        log.warn("[{}] {} {} - {}",
            status.value(), req.getMethod(), req.getRequestURI(), detail);
    }

    private static String headerOrNull(HttpServletRequest req, String name) {
        var value = req.getHeader(name);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
