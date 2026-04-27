package com.tchalanet.server.core.draw.infra.web.mapper;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelResponse;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CommonIdMapper.class)
public interface DrawChannelWebMapper {

  @Mapping(target = "gameCode", ignore = true)
  @Mapping(target = "defaultSource", expression = "java(drawChannel.defaultSource() != null ? drawChannel.defaultSource().name() : null)")
  @Mapping(target = "flags", expression = "java(drawChannel.flags())")
  DrawChannelResponse toResponse(DrawChannelView drawChannel);

  DrawChannelSummaryResponse toSummaryResponse(DrawChannelSummary summary);
}
