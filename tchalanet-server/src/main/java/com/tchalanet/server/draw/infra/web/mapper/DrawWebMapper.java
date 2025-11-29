package com.tchalanet.server.draw.infra.web.mapper;

import com.tchalanet.server.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.draw.application.command.model.FetchAndApplyExternalResultCommand;
import com.tchalanet.server.draw.application.command.model.SettleDrawCommand;
import com.tchalanet.server.draw.domain.model.Draw;
import com.tchalanet.server.draw.infra.web.dto.DrawSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawWebMapper {

  FetchAndApplyExternalResultCommand toFetchCommand(java.util.UUID drawId);

  SettleDrawCommand toSettleCommand(java.util.UUID drawId);

  default CloseDueDrawsCommand toCloseDueCommand() {
    return new CloseDueDrawsCommand();
  }

  // example mappings
  DrawSummaryResponse toDrawSummaryResponse(Draw draw);

  // CreateDrawRequest to CreateDrawCommand mapping would be added when CreateDrawCommand exists
}
