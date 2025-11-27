package com.tchalanet.server.session.web;

import com.tchalanet.server.session.domain.model.PosSession;
import com.tchalanet.server.session.domain.ports.in.CloseSessionUseCase;
import com.tchalanet.server.session.domain.ports.in.GetCurrentSessionQuery;
import com.tchalanet.server.session.domain.ports.in.OpenSessionUseCase;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class PosSessionController {

  private final OpenSessionUseCase openSessionUseCase;
  private final CloseSessionUseCase closeSessionUseCase;
  private final GetCurrentSessionQuery getCurrentSessionQuery;

  public PosSessionController(
      OpenSessionUseCase openSessionUseCase,
      CloseSessionUseCase closeSessionUseCase,
      GetCurrentSessionQuery getCurrentSessionQuery) {
    this.openSessionUseCase = openSessionUseCase;
    this.closeSessionUseCase = closeSessionUseCase;
    this.getCurrentSessionQuery = getCurrentSessionQuery;
  }

  @PostMapping("/open")
  public ResponseEntity<PosSession> open(@RequestBody OpenSessionRequest body) {
    PosSession session =
        openSessionUseCase.open(
            new OpenSessionUseCase.Command(
                body.tenantId(),
                body.outletId(),
                body.terminalId(),
                body.userId(),
                body.openingFloat()));
    return ResponseEntity.ok(session);
  }

  @PostMapping("/{sessionId}/close")
  public ResponseEntity<PosSession> close(
      @PathVariable UUID sessionId, @RequestBody CloseSessionRequest body) {
    PosSession session =
        closeSessionUseCase.close(new CloseSessionUseCase.Command(sessionId, body.closingAmount()));
    return ResponseEntity.ok(session);
  }

  @GetMapping("/current")
  public ResponseEntity<PosSession> current(
      @RequestParam UUID tenantId, @RequestParam UUID terminalId) {
    Optional<PosSession> current = getCurrentSessionQuery.get(tenantId, terminalId);
    return current.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
  }

  public record OpenSessionRequest(
      UUID tenantId, UUID outletId, UUID terminalId, UUID userId, BigDecimal openingFloat) {}

  public record CloseSessionRequest(BigDecimal closingAmount) {}
}
