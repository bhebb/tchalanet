package com.tchalanet.server.features.ticketverify;

import com.tchalanet.server.common.apiresponse.ApiResponse;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyResponse;
import com.tchalanet.server.features.ticketverify.model.TicketVerifyStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/tickets")
@RequiredArgsConstructor
@Tag(name = "Public • Tickets")
public class TicketVerifyController {

  private final TicketVerifyService service;

  @Operation(summary = "Verify a public ticket code")
  @GetMapping("/verify/{code}")
  public ResponseEntity<ApiResponse<TicketVerifyResponse>> verify(@PathVariable String code) {
    var result = service.verify(code);

    if (result.status() == TicketVerifyStatus.INVALID_CODE || result.status() == TicketVerifyStatus.NOT_FOUND) {
      return ResponseEntity.status(404)
          .header("X-Robots-Tag", "noindex, nofollow")
          .header("Cache-Control", "no-store")
          .body(ApiResponse.notFound(result.status().name()));
    }

    return ResponseEntity.ok()
        .header("X-Robots-Tag", "noindex, nofollow")
        .header("Cache-Control", "no-store")
        .body(ApiResponse.success(result));
  }
}
