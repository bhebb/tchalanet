package com.tchalanet.server.common.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.Map;

public final class ProblemRest {

    private ProblemRest() {
    }

    public static ProblemRestException of(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setDetail(detail);
        return new ProblemRestException(pd);
    }

    public static ProblemRestException of(HttpStatus status, String detail, Map<String, Object> properties) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setDetail(detail);
        pd.setProperties(properties);
        return new ProblemRestException(pd);
    }

    public static ProblemRestException badRequest(String detail) {
        return of(HttpStatus.BAD_REQUEST, detail);
    }

    public static ProblemRestException unauthorized(String detail) {
        return of(HttpStatus.UNAUTHORIZED, detail);
    }

    public static ProblemRestException forbidden(String detail) {
        return of(HttpStatus.FORBIDDEN, detail);
    }

    public static ProblemRestException forbidden(String detail, Map<String, Object> properties) {
        return of(HttpStatus.FORBIDDEN, detail, properties);
    }

    public static ProblemRestException notFound(String detail) {
        return of(HttpStatus.NOT_FOUND, detail);
    }

    /**
     * Convenience overload — appends the id to the detail message.
     */
    public static ProblemRestException notFound(String detail, Object id) {
        return of(HttpStatus.NOT_FOUND, detail + ": " + id);
    }

    public static ProblemRestException conflict(String detail) {
        return of(HttpStatus.CONFLICT, detail);
    }

    public static ProblemRestException unprocessable(String detail) {
        return of(HttpStatus.UNPROCESSABLE_ENTITY, detail);
    }

    public static ProblemRestException internal(String detail) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, detail);
    }
}
