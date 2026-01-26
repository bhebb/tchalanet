package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawChannelRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelResponse;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawChannelRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CommonIdMapper.class)
public interface DrawChannelWebMapper {
  DrawChannelResponse toResponse(
      com.tchalanet.server.core.draw.domain.model.DrawChannel drawChannel);

  DrawChannelSummaryResponse toSummaryResponse(DrawChannelSummary summary);

  CreateDrawChannelCommand toCreateCommand(CreateDrawChannelRequest request);

  UpdateDrawChannelCommand toUpdateCommand(UpdateDrawChannelRequest request);
}
