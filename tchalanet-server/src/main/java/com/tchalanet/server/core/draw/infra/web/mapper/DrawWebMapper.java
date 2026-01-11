package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.command.model.SettleDrawCommand;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawWebMapper {

  default SettleDrawCommand toSettleCommand(DrawId drawId) {
    return new SettleDrawCommand(null, drawId);
  }

  // example mappings
  DrawSummaryResponse toDrawSummaryResponse(Draw draw);
}
