package com.tchalanet.server.core.draw.infra.web.mapper;

import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.core.draw.application.command.model.CreateDrawCommand;
import com.tchalanet.server.core.draw.application.command.model.UpdateDrawCommand;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import com.tchalanet.server.core.draw.infra.web.model.CreateDrawRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.infra.web.model.UpdateDrawRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface DrawAdminWebMapper {
  CreateDrawCommand toCreateDrawCommand(CreateDrawRequest request);

  @Mapping(target = "scheduledDate", source = "scheduledDate")
  UpdateDrawCommand toUpdateDrawCommand(UpdateDrawRequest request);


  @Mapping(target = "channelCode", source = "summary.channelCode")
  @Mapping(target = "channelName", source = "summary.channelName")
  @Mapping(target = "status", source = "summary.status")
  @Mapping(
      target = "drawTime",
      expression =
          "java(summary.scheduledAt() == null ? null : summary.scheduledAt().toOffsetDateTime())")
  @Mapping(
      target = "cutoffTime",
      expression =
          "java(summary.cutoffTime() == null ? null : summary.cutoffTime().toOffsetDateTime())")
  @Mapping(target = "isNext", source = "summary.isNext")
  @Mapping(target = "active", source = "summary.active")
  @Mapping(target = "lastResult", source = "summary.lastResult")
  DrawSummaryResponse toDrawSummaryResponse(DrawSummary summary);

  @Mappings({
    @Mapping(target = "channelCode", source = "request.channelCode"),
    @Mapping(target = "channelName", constant = ""),
    @Mapping(target = "status", constant = "SCHEDULED"),
    @Mapping(
        target = "drawTime",
        expression =
            "java(request.scheduledDate() == null ? null : request.scheduledDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC))"),
    @Mapping(target = "cutoffTime", ignore = true),
    @Mapping(target = "isNext", constant = "false"),
    @Mapping(target = "active", constant = "true"),
    @Mapping(target = "lastResult", expression = "java(java.util.List.of())")
  })
  DrawSummaryResponse toDrawSummaryResponseFallback(CreateDrawRequest request);

  @Mappings({
    @Mapping(target = "channelCode", constant = ""),
    @Mapping(target = "channelName", constant = ""),
    @Mapping(target = "status", constant = "SCHEDULED"),
    @Mapping(
        target = "drawTime",
        expression =
            "java(request.scheduledDate() == null ? null : request.scheduledDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC))"),
    @Mapping(target = "cutoffTime", ignore = true),
    @Mapping(target = "isNext", constant = "false"),
    @Mapping(target = "active", constant = "true"),
    @Mapping(target = "lastResult", expression = "java(java.util.List.of())")
  })
  DrawSummaryResponse toDrawSummaryResponseFallback(UpdateDrawRequest request);
}
