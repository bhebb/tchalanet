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

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        SALES_SIMULATION_EMPTY_MIX,
        SALES_SIMULATION_TOO_MANY_TICKETS,
        SALES_SIMULATION_REASON_REQUIRED,
        SALES_SIMULATION_DRAW_NOT_IN_TENANT);
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
}
