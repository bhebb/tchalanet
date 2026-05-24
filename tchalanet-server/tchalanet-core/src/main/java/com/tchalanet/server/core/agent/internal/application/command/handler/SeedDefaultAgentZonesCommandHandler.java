package com.tchalanet.server.core.agent.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.agent.api.command.CreateAgentZoneCommand;
import com.tchalanet.server.core.agent.api.command.SeedDefaultAgentZonesCommand;
import com.tchalanet.server.core.agent.api.model.AgentZoneView;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SeedDefaultAgentZonesCommandHandler implements CommandHandler<SeedDefaultAgentZonesCommand, List<AgentZoneView>> {
  private final CreateAgentZoneCommandHandler createZone;

  @Override
  @TchTx
  public List<AgentZoneView> handle(SeedDefaultAgentZonesCommand c) {
    var seed = List.of(
        new String[]{"ZONE_METROPOLITAINE", "Zone Métropolitaine"},
        new String[]{"OUEST", "Ouest"},
        new String[]{"CENTRE", "Centre"},
        new String[]{"ARTIBONITE", "Artibonite"},
        new String[]{"NORD", "Nord"},
        new String[]{"NORD_EST", "Nord-Est"},
        new String[]{"NORD_OUEST", "Nord-Ouest"},
        new String[]{"SUD", "Sud"},
        new String[]{"SUD_EST", "Sud-Est"},
        new String[]{"GRAND_ANSE", "Grand'Anse"},
        new String[]{"NIPPES", "Nippes"}
    );
    var out = new ArrayList<AgentZoneView>();
    for (var row : seed) {
      try { out.add(createZone.handle(new CreateAgentZoneCommand(c.tenantId(), null, row[0], row[1], "REGION"))); }
      catch (IllegalArgumentException ignoredIfAlreadyExists) { }
    }
    return out;
  }
}
