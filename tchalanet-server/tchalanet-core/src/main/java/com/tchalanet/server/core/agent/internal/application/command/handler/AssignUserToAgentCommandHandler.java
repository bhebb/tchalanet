package com.tchalanet.server.core.agent.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.agent.api.command.AssignUserToAgentCommand;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentReaderPort;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AssignUserToAgentCommandHandler implements CommandHandler<AssignUserToAgentCommand, Void> {
  private final AgentReaderPort reader;
  private final AgentWriterPort writer;

  @Override
  @TchTx
  public Void handle(AssignUserToAgentCommand c) {
    var agent = reader.getAgentRequired(c.tenantId(), c.agentId());
    if (!agent.activeForSale()) throw new IllegalArgumentException("agent.not_active");
    if (!writer.userAssignmentExists(c.tenantId(), c.agentId(), c.userId(), c.relation())) {
      writer.assignUser(c.tenantId(), c.agentId(), c.userId(), c.relation().trim().toUpperCase());
    }
    return null;
  }
}
