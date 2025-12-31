package com.tchalanet.server.core.sales.infra.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.sales.application.query.model.VerifyPublicTicketQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PublicTicketController {

  private final QueryBus queryBus;

  @GetMapping("/public/tickets/verify/{publicCode}")
  public ResponseEntity<?> verify(@PathVariable String publicCode) {
    var q = new VerifyPublicTicketQuery(publicCode, java.time.Instant.now());
    var res = queryBus.send(q); // TicketVerificationResult

    if (res == null) {
      return ResponseEntity.status(404).build();
    }

    return ResponseEntity.ok()
        .header("X-Robots-Tag", "noindex, nofollow")
        .header("Cache-Control", "no-store")
        .body(res);
  }
}
