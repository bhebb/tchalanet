package com.tchalanet.server.features.stats.outletdaily;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.apiresponse.ApiResponse;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletDailySummaryQuery;
import com.tchalanet.server.core.outlet.application.query.model.OutletDailySummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/stats/outlet")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name = "Tenant • Outlet Daily Stats")
public class OutletDailyStatsController {

  private final QueryBus queryBus;

  @Operation(summary = "Get outlet daily summary")
  @GetMapping("/{id}/daily")
  public ApiResponse<OutletDailySummary> dailySummary(
      @CurrentContext TchRequestContext ctx,
      @PathVariable OutletId id,
      @RequestParam("date") String date) {
    try {
      var summary =
          queryBus.ask(new GetOutletDailySummaryQuery(ctx.tenantIdSafe(), id, LocalDate.parse(date)));
      return ApiResponse.success(summary);
    } catch (DateTimeParseException e) {
      throw ProblemRest.badRequest("invalid date format: " + date);
    }
  }
}
