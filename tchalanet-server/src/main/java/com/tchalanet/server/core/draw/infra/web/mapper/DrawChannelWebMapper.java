package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.core.draw.application.command.model.CreateDrawChannelCommand;
import com.tchalanet.server.core.draw.application.command.model.UpdateDrawChannelCommand;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawChannelRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelResponse;
import com.tchalanet.server.core.draw.infra.web.model.DrawChannelSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawChannelRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawChannelWebMapper {
  DrawChannelResponse toResponse(DrawChannel drawChannel);

  DrawChannelSummaryResponse toSummaryResponse(DrawChannelSummary summary);

  CreateDrawChannelCommand toCreateCommand(CreateDrawChannelRequest request);

  UpdateDrawChannelCommand toUpdateCommand(UpdateDrawChannelRequest request);
}
