package com.tchalanet.server.core.pos.infra.web.mapper;

import com.tchalanet.server.core.pos.domain.model.PosSession;
import com.tchalanet.server.core.pos.domain.ports.in.CloseSessionUseCase;
import com.tchalanet.server.core.pos.domain.ports.in.OpenSessionUseCase;
import com.tchalanet.server.core.pos.web.dto.CloseSessionRequest;
import com.tchalanet.server.core.pos.web.dto.OpenSessionRequest;
import com.tchalanet.server.core.pos.web.dto.PosSessionResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PosSessionWebMapper {

  public OpenSessionUseCase.OpenSessionCommand toOpenCommand(
      UUID tenantId, OpenSessionRequest request) {
    return new OpenSessionUseCase.OpenSessionCommand(
        tenantId, request.terminalId(), request.userId(), request.openingFloat());
  }

  public CloseSessionUseCase.CloseSessionCommand toCloseCommand(
      UUID tenantId, UUID sessionId, CloseSessionRequest request) {
    return new CloseSessionUseCase.CloseSessionCommand(
        tenantId, sessionId, request.userId(), request.closingAmount());
  }

  public PosSessionResponse toPosSessionResponse(PosSession session) {
    return new PosSessionResponse(
        session.getId(),
        session.getTenantId(),
        session.getTerminalId(),
        session.getUserId(),
        session.getStatus(),
        session.getOpenedAt(),
        session.getClosedAt(),
        session.getLastActivityAt(),
        session.getOpeningFloat(),
        session.getClosingAmount(),
        session.getTotalTicketsAmount(),
        session.getTotalPayoutAmount(),
        session.getGrossMargin());
  }
}
