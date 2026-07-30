package com.tchalanet.server.features.ops.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorParamSpec;
import com.tchalanet.server.common.web.error.ErrorParamType;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable request failures owned by platform operational tools. */
@UtilityClass
public class OpsErrorCodes {

  private static final Set<ErrorAudience> AUDIENCES = Set.of(ErrorAudience.WEB_PLATFORM);

  public static final ErrorDescriptor SALES_SIMULATION_EMPTY_MIX =
      validation("ops.sales_simulation.empty_mix");
  public static final ErrorDescriptor SALES_SIMULATION_TOO_MANY_TICKETS =
      new ErrorDescriptor(
          "ops.sales_simulation.too_many_tickets",
          ErrorCategory.VALIDATION,
          HttpStatus.BAD_REQUEST,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          AUDIENCES,
          Set.of(
              new ErrorParamSpec("requestedTickets", ErrorParamType.INTEGER),
              new ErrorParamSpec("maxTickets", ErrorParamType.INTEGER)));
  public static final ErrorDescriptor SALES_SIMULATION_REASON_REQUIRED =
      validation("ops.sales_simulation.reason_required");
  public static final ErrorDescriptor SALES_SIMULATION_DRAW_NOT_IN_TENANT =
      validation("ops.sales_simulation.draw_not_in_tenant");
  public static final ErrorDescriptor DRAW_RESULT_FUTURE_DATE =
      validation("ops.draw_result.future_date");
  public static final ErrorDescriptor BATCH_UNKNOWN_TENANT_CODE =
      validation("ops.batch.unknown_tenant_code");
  public static final ErrorDescriptor BATCH_TOO_MANY_TENANTS =
      new ErrorDescriptor(
          "ops.batch.too_many_tenants",
          ErrorCategory.VALIDATION,
          HttpStatus.BAD_REQUEST,
          ErrorRetryPolicy.AFTER_USER_ACTION,
          AUDIENCES,
          Set.of(
              new ErrorParamSpec("requestedTenants", ErrorParamType.INTEGER),
              new ErrorParamSpec("maxTenants", ErrorParamType.INTEGER)));
  public static final ErrorDescriptor BATCH_JOB_KEY_INVALID =
      validation("ops.batch.job_key.invalid");
  public static final ErrorDescriptor BATCH_JOB_KEY_REQUIRED =
      validation("ops.batch.job_key.required");
  public static final ErrorDescriptor BATCH_JOB_NOT_FOUND = notFound("ops.batch.job.not_found");
  public static final ErrorDescriptor BATCH_EXECUTION_NOT_FOUND =
      notFound("ops.batch.execution.not_found");
  public static final ErrorDescriptor BATCH_SCOPE_INVALID =
      validation("ops.batch.scope.invalid");
  public static final ErrorDescriptor BATCH_TENANT_REQUIRED =
      validation("ops.batch.tenant.required");
  public static final ErrorDescriptor BATCH_TENANT_NOT_ALLOWED =
      validation("ops.batch.tenant.not_allowed");
  public static final ErrorDescriptor BATCH_TENANT_ID_INVALID =
      validation("ops.batch.tenant_id.invalid");
  public static final ErrorDescriptor BATCH_LIMIT_INVALID = validation("ops.batch.limit.invalid");
  public static final ErrorDescriptor BATCH_RETENTION_INVALID =
      validation("ops.batch.retention.invalid");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        SALES_SIMULATION_EMPTY_MIX,
        SALES_SIMULATION_TOO_MANY_TICKETS,
        SALES_SIMULATION_REASON_REQUIRED,
        SALES_SIMULATION_DRAW_NOT_IN_TENANT,
        DRAW_RESULT_FUTURE_DATE,
        BATCH_UNKNOWN_TENANT_CODE,
        BATCH_TOO_MANY_TENANTS,
        BATCH_JOB_KEY_INVALID,
        BATCH_JOB_KEY_REQUIRED,
        BATCH_JOB_NOT_FOUND,
        BATCH_EXECUTION_NOT_FOUND,
        BATCH_SCOPE_INVALID,
        BATCH_TENANT_REQUIRED,
        BATCH_TENANT_NOT_ALLOWED,
        BATCH_TENANT_ID_INVALID,
        BATCH_LIMIT_INVALID,
        BATCH_RETENTION_INVALID);
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.BAD_REQUEST,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor notFound(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.NEVER,
        AUDIENCES,
        Set.of());
  }
}
