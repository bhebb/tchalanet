package com.tchalanet.server.core.agent.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.agent.api.command.UpdateAgentStatusCommand;
import com.tchalanet.server.core.agent.api.model.AgentView;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentReaderPort;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentWriterPort;
import com.tchalanet.server.core.agent.internal.application.service.AgentApplicationMapper;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateAgentStatusCommandHandler implements CommandHandler<UpdateAgentStatusCommand, AgentView> {
  private final AgentReaderPort reader;
  private final AgentWriterPort writer;
  private final Clock clock;
  private final AgentApplicationMapper mapper = new AgentApplicationMapper();

  @Override
  @TchTx
  public AgentView handle(UpdateAgentStatusCommand c) {
    var agent = reader.getAgentRequired(c.tenantId(), c.agentId());
    return mapper.toAgentView(writer.saveAgent(agent.withStatus(c.status(), Instant.now(clock))));
  }
}
