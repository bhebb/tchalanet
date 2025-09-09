package com.tchalanet.server.error;

import org.springframework.http.ProblemDetail;

public class ProblemRestException extends RuntimeException {
  private final ProblemDetail problem;

  public ProblemRestException(ProblemDetail problem) {
    super(problem.getDetail());
    this.problem = problem;
  }

  public ProblemDetail getProblem() {
    return problem;
  }
}
