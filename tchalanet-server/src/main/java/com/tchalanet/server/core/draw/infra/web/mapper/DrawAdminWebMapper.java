package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawCommand;
import com.tchalanet.server.core.draw.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.core.draw.application.command.model.UpdateDrawCommand;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.OverrideDrawResultRequest;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawRequest;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface DrawAdminWebMapper {
  CreateDrawCommand toCreateDrawCommand(CreateDrawRequest request);

  UpdateDrawCommand toUpdateDrawCommand(UpdateDrawRequest request);

  OverrideDrawResultCommand toOverrideDrawResultCommand(OverrideDrawResultRequest request);

  DrawSummaryResponse toDrawSummaryResponse(DrawSummary summary);

  default DrawId mapDrawId(UUID value) {
    return value == null ? null : DrawId.of(value);
  }

  default TenantId mapTenantId(UUID value) {
    return value == null ? null : TenantId.of(value);
  }

  @Mappings({
    @Mapping(target = "channelCode", source = "request.channelCode"),
    @Mapping(target = "channelName", constant = ""),
    @Mapping(target = "status", constant = "SCHEDULED"),
    @Mapping(target = "drawTime", expression = "java(request.scheduledDate() == null ? null : request.scheduledDate().atStartOfDay().toOffsetDateTime())"),
    @Mapping(target = "cutoffTime", ignore = true),
    @Mapping(target = "isNext", constant = "false"),
    @Mapping(target = "active", constant = "true"),
    @Mapping(target = "lastResult", expression = "java(java.util.List.of())")
  })
  DrawSummaryResponse toDrawSummaryResponseFallback(CreateDrawRequest request);

  @Mappings({
    @Mapping(target = "channelCode", source = "request.code"),
    @Mapping(target = "channelName", source = "request.name"),
    @Mapping(target = "status", constant = "SCHEDULED"),
    @Mapping(target = "drawTime", expression = "java(request.scheduledDate() == null ? null : request.scheduledDate().atStartOfDay().toOffsetDateTime())"),
    @Mapping(target = "cutoffTime", ignore = true),
    @Mapping(target = "isNext", constant = "false"),
    @Mapping(target = "active", constant = "true"),
    @Mapping(target = "lastResult", expression = "java(java.util.List.of())")
  })
  DrawSummaryResponse toDrawSummaryResponseFallback(UpdateDrawRequest request);
}
