package com.tchalanet.server.core.sales.infra.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.sales.application.query.model.GetTicketQrPngByPublicCodeQuery;
import com.tchalanet.server.core.sales.application.query.model.VerifyPublicTicketQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Public • Tickets")
@RequestMapping("/public/tickets")
public class PublicTicketController {

  private final QueryBus queryBus;

  @Operation(summary = "Verify a public ticket code (public)")
  @GetMapping("/verify/{publicCode}")
  public ResponseEntity<?> verify(@PathVariable String publicCode) {
    var q = new VerifyPublicTicketQuery(publicCode, java.time.Instant.now());
    var res = queryBus.send(q);

    if (res == null) {
      return ResponseEntity.status(404).build();
    }

    return ResponseEntity.ok()
        .header("X-Robots-Tag", "noindex, nofollow")
        .header("Cache-Control", "no-store")
        .body(res);
  }

  @GetMapping(value = "/qr/{publicCode}.png", produces = MediaType.IMAGE_PNG_VALUE)
  public byte[] qrPng(
      @PathVariable String publicCode,
      @RequestParam(name = "size", defaultValue = "280") int size,
      HttpServletResponse res) {
    res.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");

    return queryBus.send(new GetTicketQrPngByPublicCodeQuery(publicCode, size));
  }
}
