package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawChannelRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelResponse;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawChannelRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CommonIdMapper.class)
public interface DrawChannelWebMapper {

  @Mapping(target = "gameCode", ignore = true) // Computed at BFF level or left empty
  @Mapping(target = "defaultSource", expression = "java(drawChannel.defaultSource() != null ? drawChannel.defaultSource().name() : null)")
  DrawChannelResponse toResponse(DrawChannelView drawChannel);

  DrawChannelSummaryResponse toSummaryResponse(DrawChannelSummary summary);
}
