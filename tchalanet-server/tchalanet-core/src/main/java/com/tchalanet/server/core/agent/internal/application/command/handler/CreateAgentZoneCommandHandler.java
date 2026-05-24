package com.tchalanet.server.core.agent.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.AgentZoneId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.agent.api.command.CreateAgentZoneCommand;
import com.tchalanet.server.core.agent.api.model.AgentZoneStatus;
import com.tchalanet.server.core.agent.api.model.AgentZoneView;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentReaderPort;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentWriterPort;
import com.tchalanet.server.core.agent.internal.application.service.AgentApplicationMapper;
import com.tchalanet.server.core.agent.internal.domain.model.AgentZone;
import com.tchalanet.server.core.agent.internal.domain.service.AgentZonePolicy;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateAgentZoneCommandHandler implements CommandHandler<CreateAgentZoneCommand, AgentZoneView> {
  private final AgentReaderPort reader;
  private final AgentWriterPort writer;
  private final IdGenerator idGenerator;
  private final Clock clock;
  private final AgentApplicationMapper mapper;
  private final AgentZonePolicy policy = new AgentZonePolicy();

  @Override
  @TchTx
  public AgentZoneView handle(CreateAgentZoneCommand c) {
    if (writer.zoneCodeExists(c.tenantId(), c.code())) throw new IllegalArgumentException("agent_zone.code_already_exists");
    var parent = c.parentZoneId() == null ? null : reader.getZoneRequired(c.tenantId(), c.parentZoneId());
    if (parent != null) policy.requireActive(parent);
    var now = Instant.now(clock);
    var zone = new AgentZone(AgentZoneId.of(idGenerator.newUuid()), c.tenantId(), c.parentZoneId(), c.code().trim().toUpperCase(), c.name().trim(), c.zoneType().trim().toUpperCase(), AgentZoneStatus.ACTIVE, policy.computeChildDepth(parent), now, now);
    return mapper.toZoneView(writer.saveZone(zone));
  }
}
