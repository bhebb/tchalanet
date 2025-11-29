package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.core.draw.application.command.model.CreateDrawCommand;
import com.tchalanet.server.core.draw.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.core.draw.application.command.model.UpdateDrawCommand;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.OverrideDrawResultRequest;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawAdminWebMapper {
  CreateDrawCommand toCreateDrawCommand(CreateDrawRequest request);

  UpdateDrawCommand toUpdateDrawCommand(UpdateDrawRequest request);

  OverrideDrawResultCommand toOverrideDrawResultCommand(OverrideDrawResultRequest request);

  DrawSummaryResponse toDrawSummaryResponse(DrawSummary summary);
}
