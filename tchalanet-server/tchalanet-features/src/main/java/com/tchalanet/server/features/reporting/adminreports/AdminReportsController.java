package com.tchalanet.server.features.reporting.adminreports;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.reporting.ReportPeriodResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reports")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Tenant Admin • Reports")
public class AdminReportsController {

  private static final int DEFAULT_DRAW_LIMIT = 100;
  private static final int DEFAULT_SELLER_TERMINAL_LIMIT = 100;
  private static final int MAX_LIMIT = 500;

  private final AdminReportsService service;
  private final ReportPeriodResolver periodResolver;

  @GetMapping("/overview")
  @Operation(summary = "Tenant report overview")
  public ApiResponse<AdminReportResponses.Overview> overview(
      @CurrentContext TchRequestContext ctx,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "100") int drawLimit,
      @RequestParam(defaultValue = "100") int sellerTerminalLimit,
      @RequestParam(required = false) List<UUID> drawIds,
      @RequestParam(required = false) List<UUID> sellerTerminalIds
  ) {
    return ApiResponse.success(service.overview(
        ctx.tenantIdRequired(),
        request(ctx, from, to, drawLimit, sellerTerminalLimit, drawIds, sellerTerminalIds)));
  }

  @GetMapping("/draws")
  @Operation(summary = "Tenant report rows grouped by draw")
  public ApiResponse<AdminReportResponses.Draws> draws(
      @CurrentContext TchRequestContext ctx,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "100") int limit,
      @RequestParam(required = false) List<UUID> drawIds
  ) {
    return ApiResponse.success(service.draws(
        ctx.tenantIdRequired(),
        request(ctx, from, to, limit, DEFAULT_SELLER_TERMINAL_LIMIT, drawIds, List.of())));
  }

  @GetMapping("/seller-terminals")
  @Operation(summary = "Tenant report rows grouped by seller terminal")
  public ApiResponse<AdminReportResponses.SellerTerminals> sellerTerminals(
      @CurrentContext TchRequestContext ctx,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "100") int limit,
      @RequestParam(required = false) List<UUID> sellerTerminalIds
  ) {
    return ApiResponse.success(service.sellerTerminals(
        ctx.tenantIdRequired(),
        request(ctx, from, to, DEFAULT_DRAW_LIMIT, limit, List.of(), sellerTerminalIds)));
  }

  private AdminReportPeriodRequest request(
      TchRequestContext ctx,
      LocalDate from,
      LocalDate to,
      int drawLimit,
      int sellerTerminalLimit,
      List<UUID> drawIds,
      List<UUID> sellerTerminalIds
  ) {
    var period = periodResolver.resolve(from, to, ctx.tenantZoneId());
    return new AdminReportPeriodRequest(
        period.from(),
        period.to(),
        ctx.tenantZoneId(),
        boundedLimit(drawLimit, DEFAULT_DRAW_LIMIT),
        boundedLimit(sellerTerminalLimit, DEFAULT_SELLER_TERMINAL_LIMIT),
        drawIds == null ? List.of() : drawIds,
        sellerTerminalIds == null ? List.of() : sellerTerminalIds);
  }

  private static int boundedLimit(int requested, int fallback) {
    if (requested <= 0) {
      return fallback;
    }
    return Math.min(requested, MAX_LIMIT);
  }
}
