package com.tchalanet.server.features.receipt;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.features.receipt.app.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tenant • Receipts")
public class ReceiptController {

  private final ReceiptService service;

  @Operation(summary = "Get ESC/POS printable bytes for a ticket")
  @GetMapping(value = "/{ticketId}/print.escpos", produces = "application/octet-stream")
  @PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
  public byte[] printEscpos(@PathVariable TicketId ticketId, HttpServletResponse res) {
    res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ticket-" + ticketId + ".bin");
    return service.renderEscPos(ticketId);
  }

  @Operation(summary = "Get PDF printable for a ticket")
  @GetMapping(value = "/{ticketId}/print.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
  public byte[] printPdf(@PathVariable TicketId ticketId, HttpServletResponse res) {
    res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    res.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=ticket-" + ticketId + ".pdf");
    return service.renderPdf(ticketId);
  }

  @Operation(summary = "Get QR PNG for a ticket")
  @GetMapping(value = "/{ticketId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
  @PreAuthorize("hasAnyAuthority('CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<byte[]> qrPng(
      @PathVariable TicketId ticketId,
      @RequestParam(name = "size", defaultValue = "280") int size) {
    byte[] png = service.renderQrPng(ticketId, size);
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .contentType(MediaType.IMAGE_PNG)
        .body(png);
  }
}
