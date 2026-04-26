package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.application.command.model.SettleDrawCommand;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface DrawWebMapper {

  default SettleDrawCommand toSettleCommand(DrawId drawId) {
    return new SettleDrawCommand(null, drawId);
  }

  @Mapping(target = "status", expression = "java(draw.status() != null ? draw.status().name() : null)")
  @Mapping(target = "drawTime", expression = "java(draw.scheduledAt() == null ? null : draw.scheduledAt().toOffsetDateTime())")
  @Mapping(target = "cutoffTime", expression = "java(draw.cutoffAt() == null ? null : draw.cutoffAt().toOffsetDateTime())")
  @Mapping(target = "isNext", constant = "false")
  @Mapping(target = "lastResult", expression = "java(java.util.List.of())")
  DrawSummaryResponse toDrawSummaryResponse(Draw draw);
}
