package com.tchalanet.server.core.sales.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.sales.api.command.payout.AdjustTicketPayoutPaidAmountCommand;
import com.tchalanet.server.core.sales.internal.application.command.model.CancelTicketCommand;
import com.tchalanet.server.core.sales.internal.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.internal.infra.web.model.AdjustTicketPayoutPaidAmountRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.AdjustTicketPayoutPaidAmountResponse;
import com.tchalanet.server.core.sales.internal.infra.web.model.CancelTicketRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketResponse;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tenant • Ticket Sales", description = "Ticket lifecycle operations: sell and cancel")
public class TicketLifecycleController {

  private final CommandBus commandBus;
  private final TicketWebMapper mapper;

  @Operation(
      operationId = "cancelTicketSale",
      summary = "Cancel a ticket",
      description = "Cancels an existing ticket sale with a mandatory cancellation payload.")
  @io.swagger.v3.oas.annotations.responses.ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Ticket cancelled"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Invalid request"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Unauthorized"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "Forbidden"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Ticket not found")
  })
  @PatchMapping("/{ticketId}/cancel")
  @PreAuthorize("hasPermission(null, 'ticket.cancel') or hasPermission(null, 'ticket.cancel-own')")
  @AuditLog(
      action = AuditAction.CANCEL_TICKET,
      entity = AuditEntityType.TICKET,
      idExpression = "#ticketId",
      detailsExpression = "{ 'reason': #request.reason() }")
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<TicketResponse> cancel(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TicketId ticketId,
      @Valid @RequestBody CancelTicketRequest request) {
    var cmd = new CancelTicketCommand(ctx.tenantId(), ticketId, ctx.userId(), request.reason());
    var result = commandBus.execute(cmd);
    return ApiResponse.success(mapper.toTicketResponse(result.ticket()));
  }

  @Operation(
      operationId = "adjustTicketPayoutPaidAmount",
      summary = "Adjust the actual paid amount for a paid ticket",
      description =
          "Audits and projects a correction to the amount actually paid. The calculated winning"
              + " amount stored on the ticket is not modified.")
  @PatchMapping("/{ticketId}/payout-paid-amount")
  @PreAuthorize("hasPermission(null, 'ticket.payout.adjust')")
  @AuditLog(
      action = AuditAction.UPDATE,
      entity = AuditEntityType.PAYOUT,
      idExpression = "#ticketId",
      detailsExpression = "{ 'paidAmount': #request.paidAmount(), 'reason': #request.reason() }")
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<AdjustTicketPayoutPaidAmountResponse> adjustPayoutPaidAmount(
      @CurrentContext TchRequestContext ctx,
      @PathVariable TicketId ticketId,
      @Valid @RequestBody AdjustTicketPayoutPaidAmountRequest request) {
    var result =
        commandBus.execute(
            new AdjustTicketPayoutPaidAmountCommand(
                ctx.tenantId(),
                ticketId,
                request.paidAmount(),
                request.reason(),
                ctx.userId(),
                java.time.Instant.now()));
    return ApiResponse.success(
        new AdjustTicketPayoutPaidAmountResponse(
            result.ticketId(),
            result.calculatedAmountCents(),
            result.previousPaidAmountCents(),
            result.adjustedPaidAmountCents(),
            result.deltaAmountCents()));
  }
}
