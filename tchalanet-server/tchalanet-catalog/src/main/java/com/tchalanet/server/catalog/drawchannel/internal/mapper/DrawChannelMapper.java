package com.tchalanet.server.catalog.drawchannel.internal.mapper;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelSummaryView;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelEntity;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.CreateDrawChannelRequest;
import com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelRequest;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DrawChannelMapper {

  // Map entity -> view: fill label from name; other complex fields left null
  @Mapping(target = "label", expression = "java(e.getName())")
  @Mapping(target = "defaultSource", expression = "java((com.tchalanet.server.catalog.drawchannel.api.model.DrawSource) null)")
  DrawChannelView toView(DrawChannelEntity e);

  default DrawChannelView toDomain(DrawChannelEntity e) {
    return toView(e);
  }

  // Map entity -> summary view
  @Mapping(target = "channelCode", expression = "java(e.getCode())")
  @Mapping(target = "channelName", expression = "java(e.getName())")
  @Mapping(target = "cutoffTime", expression = "java(e.getDrawTime())")
  DrawChannelSummaryView toSummary(DrawChannelEntity e);

  List<DrawChannelSummaryView> toSummaries(List<DrawChannelEntity> list);

  // Map web request -> entity (ignore audit/id/flags as they are handled in service)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "flags", ignore = true)
  @Mapping(target = "timezone", expression = "java(req.timezone() == null ? null : req.timezone().toString())")
  @Mapping(target = "cutoffSec", expression = "java(req.cutoffSec() == null ? 120 : req.cutoffSec())")
  @Mapping(target = "daysOfWeek", expression = "java(com.tchalanet.server.common.time.DaysOfWeekFormatter.format(req.daysOfWeek()))")
  @Mapping(target = "sortOrder", expression = "java(req.sortOrder() == null ? 0 : req.sortOrder())")
  DrawChannelEntity toEntity(CreateDrawChannelRequest req);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "flags", ignore = true)
  @Mapping(target = "timezone", expression = "java(req.timezone() == null ? null : req.timezone().toString())")
  @Mapping(target = "cutoffSec", expression = "java(req.cutoffSec() == null ? 120 : req.cutoffSec())")
  @Mapping(target = "daysOfWeek", expression = "java(com.tchalanet.server.common.time.DaysOfWeekFormatter.format(req.daysOfWeek()))")
  @Mapping(target = "sortOrder", expression = "java(req.sortOrder() == null ? 0 : req.sortOrder())")
  DrawChannelEntity toEntity(UpdateDrawChannelRequest req);

  // For DrawMapper.toEntity
  @Mapping(target = "id", source = "view.id")
  @Mapping(target = "timezone", expression = "java(view.timezone() == null ? null : view.timezone().toString())")
  @Mapping(target = "daysOfWeek", expression = "java(com.tchalanet.server.common.time.DaysOfWeekFormatter.format(view.daysOfWeek()))")
  DrawChannelEntity toEntityDefault(DrawChannelView view);

  // Update existing entity in-place from request
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "flags", ignore = true)
  void updateEntityFromRequest(UpdateDrawChannelRequest req, @MappingTarget DrawChannelEntity entity);
}
