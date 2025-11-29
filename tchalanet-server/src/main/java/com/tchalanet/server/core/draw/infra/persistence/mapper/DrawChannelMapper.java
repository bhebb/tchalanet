package com.tchalanet.server.core.draw.infra.persistence.mapper;

import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.domain.model.DrawChannelId;
import com.tchalanet.server.core.draw.infra.persistence.DrawChannelJpaEntity;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrawChannelMapper {

  DrawChannel toDomain(DrawChannelJpaEntity e);

  DrawChannelJpaEntity toEntity(DrawChannel d);

  // default helpers to assist MapStruct when generating impl
  default DrawChannelId mapToDrawChannelId(UUID id) {
    return id == null ? null : new DrawChannelId(id);
  }

  default UUID mapFromDrawChannelId(DrawChannelId id) {
    return id == null ? null : id.value();
  }

  default TenantId mapToTenantId(UUID id) {
    return id == null ? null : new TenantId(id);
  }

  default UUID mapFromTenantId(TenantId id) {
    return id == null ? null : id.value();
  }

  // timezone conversion
  default ZoneId mapToZoneId(String zone) {
    return zone == null ? null : ZoneId.of(zone);
  }

  default String mapFromZoneId(ZoneId zone) {
    return zone == null ? null : zone.toString();
  }

  // days mapping
  default List<DayOfWeek> mapToDays(String daysOfWeek) {
    if (daysOfWeek == null || daysOfWeek.isEmpty()) return null;
    return Arrays.stream(daysOfWeek.split(","))
        .map(String::trim)
        .map(String::toUpperCase)
        .map(DayOfWeek::valueOf)
        .collect(Collectors.toList());
  }

  default String mapFromDays(List<DayOfWeek> days) {
    if (days == null || days.isEmpty()) return null;
    return days.stream().map(DayOfWeek::name).collect(Collectors.joining(","));
  }
}
