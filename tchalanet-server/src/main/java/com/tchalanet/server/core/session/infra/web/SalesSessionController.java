package com.tchalanet.server.core.session.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.session.application.command.model.CloseSalesSessionCommand;
import com.tchalanet.server.core.session.application.command.model.OpenSalesSessionCommand;
import com.tchalanet.server.core.session.application.query.model.GetCurrentSalesSessionQuery;
import com.tchalanet.server.core.session.infra.web.model.CloseSalesSessionResponse;
import com.tchalanet.server.core.session.infra.web.model.OpenSalesSessionResponse;
import com.tchalanet.server.core.session.infra.web.model.SalesSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;


@RestController
@RequestMapping("/tenant/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions • Tenant Admin")
@Validated
public class SalesSessionController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Operation(summary = "Open a POS session (tenant)")
    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpenSalesSessionResponse> open(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody OpenSessionRequest body) {
        var session =
            commandBus.execute(
                new OpenSalesSessionCommand(ctx.effectiveTenantIdRequired(), body.outletId(), body.terminalId(),
                    ctx.userId(), body.openingFloat().longValue()));

        return ApiResponse.created(OpenSalesSessionResponse.fromDomain(session));
    }

    @Operation(summary = "Close a POS session (tenant)")
    @PostMapping("/{sessionId}/close")
    public ApiResponse<CloseSalesSessionResponse> close(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SalesSessionId sessionId,
        @Valid @RequestBody CloseSessionRequest body) {

        var session = commandBus.execute(new CloseSalesSessionCommand(
            ctx.effectiveTenantIdRequired(),
            sessionId,
            body.declaringClosingAmount.longValue(),
            ctx.userId(),
            body.reason()));

        return ApiResponse.success(CloseSalesSessionResponse.fromDomain(session));
    }

    @Operation(summary = "Get current session for a terminal (tenant)")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SalesSessionResponse>> current(
        @CurrentContext TchRequestContext ctx,
        @RequestParam TerminalId terminalId) {

        var result =
            queryBus.ask(new GetCurrentSalesSessionQuery(ctx.effectiveTenantIdRequired(), terminalId));

        return result
            .map(s -> ResponseEntity.ok(ApiResponse.success(SalesSessionResponse.fromDomain(s))))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    public record OpenSessionRequest(
        @NotNull OutletId outletId,
        @NotNull TerminalId terminalId,
        @DecimalMin("0.00") BigDecimal openingFloat) {
    }

    public record CloseSessionRequest(
        @DecimalMin("0.00") BigDecimal declaringClosingAmount,
        @NotNull String reason) {
    }
}
