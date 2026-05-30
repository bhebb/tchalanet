package com.tchalanet.server.common.web.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Exception applicative qui porte un {@link ProblemDetail} HTTP. Fournit des helpers statiques pour
 * les cas courants (404, 400, 422, 500...).
 */
public class ProblemRestException extends RuntimeException {
  private final ProblemDetail problem;

  public ProblemRestException(ProblemDetail problem) {
    super(problem.getDetail());
    this.problem = problem;
  }

  public ProblemDetail getProblem() {
    return problem;
  }

  // ---- Helpers statiques utilisés dans les services ----

  public static ProblemRestException notFound(String message) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Resource not found");
    pd.setDetail(message);
    return new ProblemRestException(pd);
  }

  public static ProblemRestException badRequest(String message) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Bad request");
    pd.setDetail(message);
    return new ProblemRestException(pd);
  }

  public static ProblemRestException unprocessable(String message) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
    pd.setTitle("Unprocessable entity");
    pd.setDetail(message);
    return new ProblemRestException(pd);
  }

  public static ProblemRestException conflict(String message) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Conflict");
    pd.setDetail(message);
    return new ProblemRestException(pd);
  }

  public static ProblemRestException internal(String message) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    pd.setTitle("Internal error");
    pd.setDetail(message);
    return new ProblemRestException(pd);
  }
}
