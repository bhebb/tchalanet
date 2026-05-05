package com.tchalanet.server.features.ticketdelivery;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.ticketdelivery.app.TicketDeliveryService;
import com.tchalanet.server.features.ticketdelivery.model.DeliverTicketRequest;
import com.tchalanet.server.features.ticketdelivery.model.DeliverTicketResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tenant • Tickets")
public class TicketDeliveryController {

  private final TicketDeliveryService service;

  @Operation(summary = "Send ticket receipt via email, SMS, or WhatsApp")
  @PostMapping("/{ticketId}/delivery")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Secured({"ROLE_CASHIER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"})
  public ApiResponse<DeliverTicketResponse> deliver(
      @PathVariable TicketId ticketId,
      @Valid @RequestBody DeliverTicketRequest request) {

    var result = service.deliver(ticketId, request);
    return ApiResponse.success(result);
  }
}
