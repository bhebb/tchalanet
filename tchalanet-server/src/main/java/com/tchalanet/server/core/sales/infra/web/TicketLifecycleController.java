package com.tchalanet.server.core.sales.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.sales.application.command.model.ApproveTicketSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.RejectTicketSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.SellTicketOutcome;
import com.tchalanet.server.core.sales.infra.web.mapper.TicketWebMapper;
import com.tchalanet.server.core.sales.infra.web.model.CancelSaleResponse;
import com.tchalanet.server.core.sales.infra.web.model.CancelTicketRequest;
import com.tchalanet.server.core.sales.infra.web.model.SellTicketRequest;
import com.tchalanet.server.core.sales.infra.web.model.TicketResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tenant • Ticket Sales")
public class TicketLifecycleController {

  private final CommandBus commandBus;
  private final TicketWebMapper mapper;

  @Operation(summary = "Sell a ticket")
  @PostMapping
  @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
  public ResponseEntity<ApiResponse<TicketResponse>> sell(
      @Valid @RequestBody SellTicketRequest request) {
    var cmd = mapper.toSellCommand(request);
    var result = commandBus.execute(cmd);

    if (SellTicketOutcome.PENDING_APPROVAL == result.outcome()) {
      var notice =
          new ApiNotice(
              "APPROVAL_REQUIRED",
              "Transaction requires approval",
              "sales",
              NoticeSeverity.WARN,
              Map.of("approvalRequestId", result.approvalRequestId().value()));
      return ResponseEntity.status(HttpStatus.ACCEPTED)
          .body(ApiResponse.pending(notice, mapper.toTicketResponse(result.ticket())));
    }

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(mapper.toTicketResponse(result.ticket())));
  }

  @Operation(summary = "Approve a pending ticket sale")
  @PostMapping("/{ticketId}/approve")
  @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<TicketResponse> approve(
      @PathVariable TicketId ticketId,
      @RequestParam UserId approvedBy,
      @RequestParam(required = false) String reason) {
    var cmd = new ApproveTicketSaleCommand(ticketId, approvedBy, reason);
    var result = commandBus.execute(cmd);
    return ApiResponse.success(mapper.toTicketResponse(result.ticket()));
  }

  @Operation(summary = "Reject a pending ticket sale")
  @PostMapping("/{ticketId}/reject")
  @Secured({"ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<TicketResponse> reject(
      @PathVariable TicketId ticketId,
      @RequestParam UserId rejectedBy,
      @RequestParam(required = false) String reason) {
    var cmd = new RejectTicketSaleCommand(ticketId, rejectedBy, reason);
    var result = commandBus.execute(cmd);
    return ApiResponse.success(mapper.toTicketResponse(result.ticket()));
  }

  @Operation(summary = "Cancel a ticket")
  @PatchMapping("/{ticketId}/cancel")
  @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<CancelSaleResponse> cancel(
      @PathVariable TicketId ticketId, @Valid @RequestBody CancelTicketRequest request) {
    var cmd = mapper.toCancelSaleCommand(ticketId, request);
    var result = commandBus.execute(cmd);
    return ApiResponse.success(mapper.toCancelSaleResponse(result));
  }
}
