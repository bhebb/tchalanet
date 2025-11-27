package com.tchalanet.server.pos.web;

import com.tchalanet.server.pos.domain.ports.in.CloseSessionUseCase;
import com.tchalanet.server.pos.domain.ports.in.OpenSessionUseCase;
import com.tchalanet.server.pos.web.dto.CloseSessionRequest;
import com.tchalanet.server.pos.web.dto.OpenSessionRequest;
import com.tchalanet.server.pos.web.dto.PosSessionResponse;
import com.tchalanet.server.pos.web.mapper.PosSessionWebMapper;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/pos-sessions")
@RequiredArgsConstructor
public class PosSessionController {

  private final OpenSessionUseCase openSessionUseCase;
  private final CloseSessionUseCase closeSessionUseCase;
  private final PosSessionWebMapper mapper;

  @PostMapping("/open")
  public ResponseEntity<PosSessionResponse> openSession(
      @PathVariable UUID tenantId, @Valid @RequestBody OpenSessionRequest request) {
    var command = mapper.toOpenCommand(tenantId, request);
    var session = openSessionUseCase.openSession(command);
    return new ResponseEntity<>(mapper.toPosSessionResponse(session), HttpStatus.CREATED);
  }

  @PostMapping("/{sessionId}/close")
  public ResponseEntity<PosSessionResponse> closeSession(
      @PathVariable UUID tenantId,
      @PathVariable UUID sessionId,
      @Valid @RequestBody CloseSessionRequest request) {
    var command = mapper.toCloseCommand(tenantId, sessionId, request);
    var session = closeSessionUseCase.closeSession(command);
    return ResponseEntity.ok(mapper.toPosSessionResponse(session));
  }
}
