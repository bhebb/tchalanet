package com.tchalanet.server.core.draw.infra.persistence.mapper;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import com.tchalanet.server.core.draw.infra.persistence.DrawChannelJpaEntity;
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

  // provide an explicit mapping for toEntity to ensure slotKey fields are present
  default DrawChannelJpaEntity toEntityDefault(DrawChannel d) {
    if (d == null) return null;
    var e = new DrawChannelJpaEntity();
    // id
    if (d.id() != null) e.setId(d.id().value());
    // tenant
    if (d.tenantId() != null) e.setTenantId(d.tenantId().uuid());
    // basic fields
    e.setCode(d.code());
    e.setName(d.name());
    e.setTimezone(d.timezone() == null ? null : d.timezone().toString());
    e.setDrawTime(d.drawTime());
    e.setCutoffSec(d.cutoffSec());
    if (d.daysOfWeek() != null && !d.daysOfWeek().isEmpty()) {
      e.setDaysOfWeek(
          d.daysOfWeek().stream().map(java.time.DayOfWeek::name).collect(Collectors.joining(",")));
    }
    e.setActive(Boolean.valueOf(d.isActive()));
    e.setSortOrder(d.sortOrder());
    return e;
  }

  // default helpers to assist MapStruct when generating impl
  default DrawChannelId mapToDrawChannelId(UUID id) {
    return id == null ? null : new DrawChannelId(id);
  }

  default UUID mapFromDrawChannelId(DrawChannelId id) {
    return id == null ? null : id.value();
  }

  default TenantId mapToTenantId(UUID id) {
    return id == null ? null : TenantId.of(id);
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
    if (daysOfWeek == null || daysOfWeek.isBlank()) return List.of();

    var s = daysOfWeek.trim().toUpperCase();

    // specials
    if (s.equals("*") || s.equals("ALL") || s.equals("DAILY") || s.equals("MON-SUN")) {
      return Arrays.asList(DayOfWeek.values());
    }

    java.util.function.Function<String, DayOfWeek> resolve =
        token -> {
          if (token == null || token.isBlank()) return null;
          var up = token.trim().toUpperCase();

          // full enum name first (MONDAY...)
          try {
            return DayOfWeek.valueOf(up);
          } catch (IllegalArgumentException ignored) {
          }

          // common abbreviations
          return switch (up) {
            case "MON", "M" -> DayOfWeek.MONDAY;
            case "TUE", "T", "TUES" -> DayOfWeek.TUESDAY;
            case "WED", "W" -> DayOfWeek.WEDNESDAY;
            case "THU", "TH", "THURS" -> DayOfWeek.THURSDAY;
            case "FRI", "F" -> DayOfWeek.FRIDAY;
            case "SAT", "SA" -> DayOfWeek.SATURDAY; // évite "S" ambigu
            case "SUN", "SU" -> DayOfWeek.SUNDAY;
            default -> null;
          };
        };

    var out = new java.util.LinkedHashSet<DayOfWeek>(); // stable order, unique

    for (String rawToken : s.split(",")) {
      var token = rawToken.trim();
      if (token.isEmpty()) continue;

      if (token.contains("-")) {
        var parts = token.split("-", 2);
        var from = resolve.apply(parts[0]);
        var to = resolve.apply(parts[1]);
        if (from == null || to == null) {
          throw new IllegalArgumentException("Invalid days_of_week range token: " + token);
        }

        int cur = from.getValue(); // 1..7
        int end = to.getValue();
        while (true) {
          out.add(DayOfWeek.of(cur));
          if (cur == end) break;
          cur = cur % 7 + 1; // wrap
        }
      } else {
        var day = resolve.apply(token);
        if (day == null) {
          throw new IllegalArgumentException("Invalid days_of_week token: " + token);
        }
        out.add(day);
      }
    }

    return out.stream().toList();
  }

  default String mapFromDays(List<DayOfWeek> days) {
    if (days == null || days.isEmpty()) return null;
    return days.stream().map(DayOfWeek::name).collect(Collectors.joining(","));
  }
}
