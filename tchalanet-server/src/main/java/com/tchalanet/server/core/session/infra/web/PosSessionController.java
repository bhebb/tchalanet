package com.tchalanet.server.core.session.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.id.*;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.session.application.command.model.CloseSessionCommand;
import com.tchalanet.server.core.session.application.command.model.OpenSessionCommand;
import com.tchalanet.server.core.session.application.query.model.GetCurrentSessionQuery;
import com.tchalanet.server.core.session.infra.web.model.PosSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant/sessions")
@RequiredArgsConstructor
@Tag(name = "Tenant • Sessions")
public class PosSessionController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final TchContextResolver contextResolver;

  @Operation(summary = "Open a POS session (tenant)")
  @PostMapping("/open")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<PosSessionResponse> open(
      @jakarta.validation.Valid @RequestBody OpenSessionRequest body) {
    var ctx = contextResolver.currentOrNull();
    var tenantId = TenantId.of(ctx.tenantUuid());
    var userId = ctx.userUuid() == null ? null : UserId.of(ctx.userUuid()); // source of truth

    var session =
        commandBus.send(
            new OpenSessionCommand(
                tenantId,
                OutletId.parse(body.outletId()), // MODIFIÉ: use parse
                TerminalId.parse(body.terminalId()), // MODIFIÉ: use parse
                userId,
                body.openingFloat()));

    return ApiResponse.<PosSessionResponse>created(PosSessionResponse.fromDomain(session));
  }

  @Operation(summary = "Close a POS session (tenant)")
  @PostMapping("/{sessionId}/close")
  public ResponseEntity<PosSessionResponse> close(
      @PathVariable SessionId sessionId,
      @jakarta.validation.Valid @RequestBody CloseSessionRequest body) {
    var ctx = contextResolver.currentOrNull();
    var tenantId = TenantId.of(ctx.tenantUuid());

    var session =
        commandBus.send(new CloseSessionCommand(tenantId, sessionId, body.closingAmount()));

    return ResponseEntity.ok(PosSessionResponse.fromDomain(session));
  }

  @Operation(summary = "Get current session for a terminal (tenant)")
  @GetMapping("/current")
  public ResponseEntity<PosSessionResponse> current(@RequestParam TerminalId terminalId) {
    var ctx = contextResolver.currentOrNull();
    var tenantId = TenantId.of(ctx.tenantUuid());

    var result = queryBus.send(new GetCurrentSessionQuery(tenantId, terminalId));

    return result
        .map(s -> ResponseEntity.ok(PosSessionResponse.fromDomain(s)))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  public record OpenSessionRequest(
      @NotNull String outletId,
      @NotNull String terminalId,
      @DecimalMin("0.00") BigDecimal openingFloat) {}

  public record CloseSessionRequest(@DecimalMin("0.00") BigDecimal closingAmount) {}
}
