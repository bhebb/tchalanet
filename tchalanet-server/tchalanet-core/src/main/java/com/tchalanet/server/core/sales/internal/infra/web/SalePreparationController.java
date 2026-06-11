package com.tchalanet.server.core.sales.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.sales.api.command.preparation.ConfirmPreparedSaleCommand;
import com.tchalanet.server.core.sales.api.command.preparation.PrepareSaleCommand;
import com.tchalanet.server.core.sales.api.command.preparation.RegenerateSalePreparationPromotionLineCommand;
import com.tchalanet.server.core.sales.api.model.preparation.ConfirmPreparedSaleResult;
import com.tchalanet.server.core.sales.api.model.preparation.SalePreparationView;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketLineRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketRequest;
import com.tchalanet.server.core.terminal.api.query.TerminalDeviceProofGate;
import com.tchalanet.server.core.terminal.api.query.TerminalProofPurpose;
import com.tchalanet.server.platform.idempotence.api.RequireIdempotency;
import com.tchalanet.server.platform.idempotence.api.model.IdempotencyScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sale preparation flow (maryaj-gratis-auto-selection-v1):
 * prepare -> [regenerate]* -> confirm. The confirmed ticket carries exactly
 * the previewed lines; confirm never receives lines from the client.
 */
@RestController
@RequestMapping("/tenant/sales/preparations")
@RequiredArgsConstructor
@Tag(name = "Sales • Preparation", description = "Prepared sale flow: preview with generated promotion lines, regenerate, confirm")
@PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
public class SalePreparationController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Operation(operationId = "prepareSale", summary = "Prepare a sale",
        description = "Runs the full sale pipeline, generates promotion lines, persists a 10-minute preparation and returns its id with the final lines.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SalePreparationView> prepare(
        @CurrentContext TchRequestContext ctx,
        @RequestHeader(TerminalDeviceProofGate.HEADER_TERMINAL_ID) String terminalId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_BINDING_ID)  String bindingId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_NONCE)       String nonce,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNED_AT)   String signedAt,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNATURE)   String signature,
        @Valid @RequestBody SellTicketRequest body
    ) {
        TerminalDeviceProofGate.verify(queryBus, ctx.effectiveTenantIdRequired(),
            terminalId, bindingId, TerminalProofPurpose.SELL_TICKET,
            "POST", "/tenant/sales/preparations", null,
            ctx.operationalContext(), nonce, signedAt, signature);

        var result = commandBus.execute(new PrepareSaleCommand(
            body.drawId(), body.drawChannelId(), body.currency(),
            body.lines().stream().map(SellTicketLineRequest::toLine).toList(),
            body.serviceOptions()));
        return ApiResponse.success(result);
    }

    @Operation(operationId = "regeneratePreparationPromotionLine",
        summary = "Regenerate a generated promotion line",
        description = "Before confirm only; capped by maxRegenerationsBeforeConfirm; audited.")
    @PostMapping("/{preparationId}/promotion-lines/{lineRef}/regenerate")
    public ApiResponse<SalePreparationView> regenerate(
        @CurrentContext TchRequestContext ctx,
        @PathVariable UUID preparationId,
        @PathVariable String lineRef
    ) {
        var result = commandBus.execute(
            new RegenerateSalePreparationPromotionLineCommand(preparationId, lineRef));
        return ApiResponse.success(result);
    }

    @Operation(operationId = "confirmPreparedSale", summary = "Confirm a prepared sale",
        description = "Payload = preparationId + idempotency key only. Persists exactly the prepared lines; double confirm with the same key returns the same ticket.")
    @PostMapping("/{preparationId}/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireIdempotency(scope = IdempotencyScope.SALES_SELL_TICKET)
    public ApiResponse<ConfirmPreparedSaleResult> confirm(
        @CurrentContext TchRequestContext ctx,
        @RequestHeader(TerminalDeviceProofGate.HEADER_TERMINAL_ID) String terminalId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_BINDING_ID)  String bindingId,
        @RequestHeader(TerminalDeviceProofGate.HEADER_NONCE)       String nonce,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNED_AT)   String signedAt,
        @RequestHeader(TerminalDeviceProofGate.HEADER_SIGNATURE)   String signature,
        @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
        @PathVariable UUID preparationId
    ) {
        TerminalDeviceProofGate.verify(queryBus, ctx.effectiveTenantIdRequired(),
            terminalId, bindingId, TerminalProofPurpose.SELL_TICKET,
            "POST", "/tenant/sales/preparations/" + preparationId + "/confirm", null,
            ctx.operationalContext(), nonce, signedAt, signature);

        var result = commandBus.execute(
            new ConfirmPreparedSaleCommand(preparationId, idempotencyKey));
        return ApiResponse.success(result);
    }
}
