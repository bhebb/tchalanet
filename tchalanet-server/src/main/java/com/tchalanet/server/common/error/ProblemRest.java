package com.tchalanet.server.common.error;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.core.sales.application.command.model.LimitNotice;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemRest {

  private ProblemRest() {}

  public static ProblemRestException of(HttpStatus status, String detail) {
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setDetail(detail);
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

  public static ProblemRestException notFound(String detail) {
    return of(HttpStatus.NOT_FOUND, detail);
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

  public static ProblemRestException limitBlocked(
      String detail,
      OperationType operationType,
      List<LimitNotice> limitReasons,
      boolean approvalRequired,
      ApprovalRole requiredRole) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Limit blocked");
    pd.setDetail(detail);
    pd.setProperty("operationType", operationType);
    pd.setProperty("limitOutcome", "BLOCK");
    pd.setProperty("limitReasons", limitReasons);
    pd.setProperty("approvalRequired", approvalRequired);
    pd.setProperty("requiredRole", requiredRole);
    return new ProblemRestException(pd);
  }
}
