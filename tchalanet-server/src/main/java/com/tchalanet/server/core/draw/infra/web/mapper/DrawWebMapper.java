package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.command.model.FetchAndApplyExternalResultCommand;
import com.tchalanet.server.core.draw.application.command.model.SettleDrawCommand;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawWebMapper {

  default FetchAndApplyExternalResultCommand toFetchCommand(DrawId drawId) {
    return FetchAndApplyExternalResultCommand.normal(drawId, java.time.Instant.now());
  }

  default SettleDrawCommand toSettleCommand(DrawId drawId) {
    return new SettleDrawCommand(null, drawId);
  }

  // example mappings
  DrawSummaryResponse toDrawSummaryResponse(Draw draw);
}
