package com.tchalanet.server.features.cashier;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.apiresponse.ApiResponse;
import com.tchalanet.server.features.cashier.app.CashierService;
import com.tchalanet.server.features.cashier.model.CashierSellPrintRequest;
import com.tchalanet.server.features.cashier.model.CashierSellPrintResponse;
import com.tchalanet.server.features.cashier.model.CashierSendReceiptRequest;
import com.tchalanet.server.features.cashier.model.CashierSendReceiptResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/cashier")
@RequiredArgsConstructor
@Tag(name = "Tenant • Cashier")
@Validated
public class CashierController {

  private final CashierService service;

  @Operation(summary = "Sell a ticket and return a printable receipt artifact")
  @PostMapping("/sell")
  @ResponseStatus(HttpStatus.CREATED)
  @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
  public ApiResponse<CashierSellPrintResponse> sellAndPrint(
      @Valid @RequestBody CashierSellPrintRequest request) {
    return ApiResponse.created(service.sellAndPrint(request));
  }

  @Operation(summary = "Send a ticket receipt through an external communication channel")
  @PostMapping("/tickets/{ticketId}/send")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
  public ApiResponse<CashierSendReceiptResponse> sendReceipt(
      @PathVariable TicketId ticketId,
      @Valid @RequestBody CashierSendReceiptRequest request) {
    var sendReceiptResponse = service.sendReceipt(ticketId, request);
    return ApiResponse.accepted(sendReceiptResponse);
  }
}
