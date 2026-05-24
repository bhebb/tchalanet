package com.tchalanet.server.core.agent.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.AgentId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.agent.api.command.CreateAgentCommand;
import com.tchalanet.server.core.agent.api.model.AgentStatus;
import com.tchalanet.server.core.agent.api.model.AgentUserRole;
import com.tchalanet.server.core.agent.api.model.AgentView;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentReaderPort;
import com.tchalanet.server.core.agent.internal.application.port.out.AgentWriterPort;
import com.tchalanet.server.core.agent.internal.application.service.AgentApplicationMapper;
import com.tchalanet.server.core.agent.internal.domain.model.Agent;
import com.tchalanet.server.core.agent.internal.domain.model.AgentZoneMandate;
import com.tchalanet.server.core.agent.internal.domain.service.AgentHierarchyPolicy;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@UseCase
@RequiredArgsConstructor
public class CreateAgentCommandHandler implements CommandHandler<CreateAgentCommand, AgentView> {

    private final AgentReaderPort reader;
    private final AgentWriterPort writer;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final AgentApplicationMapper mapper;

    @Override
    @TchTx
    public AgentView handle(CreateAgentCommand c) {
        var primaryZone = reader.getZoneRequired(c.tenantId(), c.primaryZoneId());
        requireActiveZone(primaryZone.active(), "agent.primary_zone_inactive");

        var parent = c.parentAgentId() == null
            ? null
            : reader.getAgentRequired(c.tenantId(), c.parentAgentId());

        var depth = AgentHierarchyPolicy.childDepth(parent);
        AgentHierarchyPolicy.requireTypeAllowedAtDepth(c.type(), depth);
        AgentHierarchyPolicy.requireCanCreateChild(parent, c.primaryZoneId(), c.commercialAllowedZoneIds());

        var now = Instant.now(clock);
        var agentId = AgentId.of(idGenerator.newUuid());

        var mandates = buildMandates(c, agentId, depth);

        var agent = Agent.create(
            agentId,
            c.tenantId(),
            c.parentAgentId(),
            c.displayName().trim(),
            c.type(),
            AgentStatus.ACTIVE,
            c.primaryZoneId(),
            c.ownerUserId(),
            depth,
            mandates,
            now
        );

        var saved = writer.saveAgent(agent);

        if (c.ownerUserId() != null) {
            writer.assignUser(c.tenantId(), saved.id(), c.ownerUserId(), AgentUserRole.OWNER.name());
        }

        return mapper.toAgentView(saved);
    }

    private List<AgentZoneMandate> buildMandates(CreateAgentCommand c, AgentId agentId, int depth) {
        return c.commercialAllowedZoneIds().stream()
            .map(zoneId -> {
                var zone = reader.getZoneRequired(c.tenantId(), zoneId);
                requireActiveZone(zone.active(), "agent.allowed_zone_inactive");

                return AgentZoneMandate.defaultCommercialMandate(
                    agentId,
                    zoneId,
                    depth,
                    Agent.MAX_V1_DEPTH
                );
            })
            .toList();
    }

    private static void requireActiveZone(boolean active, String code) {
        if (!active) {
            throw ProblemRest.badRequest(code);
        }
    }
}

