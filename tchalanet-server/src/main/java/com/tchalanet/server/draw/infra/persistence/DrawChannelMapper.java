package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import com.tchalanet.server.tenant.domain.model.TenantId;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class DrawChannelMapper {
  public static DrawChannel toDomain(DrawChannelJpaEntity e) {
    if (e == null) return null;
    List<DayOfWeek> days = null;
    if (e.getDaysOfWeek() != null && !e.getDaysOfWeek().isEmpty()) {
      days =
          Arrays.stream(e.getDaysOfWeek().split(","))
              .map(String::trim)
              .map(String::toUpperCase)
              .map(DayOfWeek::valueOf)
              .collect(Collectors.toList());
    }
    LocalTime dt = e.getDrawTime();

    return new DrawChannel(
        e.getId() == null ? null : new DrawChannelId(e.getId()),
        e.getName(),
        e.getTenantId() == null ? null : new TenantId(e.getTenantId()),
        null, // gameCode not stored here
        e.getTimezone(),
        dt,
        e.getCutoffSec(),
        days,
        e.getActive(),
        e.getSortOrder());
  }

  public static DrawChannelJpaEntity toEntity(DrawChannel d) {
    if (d == null) return null;
    DrawChannelJpaEntity e = new DrawChannelJpaEntity();
    if (d.getId() != null && d.getId().value() != null) e.setId(d.getId().value());
    if (d.getTenantId() != null && d.getTenantId().value() != null)
      e.setTenantId(d.getTenantId().value());
    e.setCode(d.getId() == null ? null : d.getId().toString());
    e.setName(d.getName());
    e.setGameId(null);
    e.setTimezone(d.getTimezone());
    if (d.getDrawTime() != null) e.setDrawTime(d.getDrawTime());
    e.setCutoffSec(d.getCutoffSec());
    if (d.getDaysOfWeek() != null && !d.getDaysOfWeek().isEmpty()) {
      String days =
          d.getDaysOfWeek().stream().map(DayOfWeek::name).collect(Collectors.joining(","));
      e.setDaysOfWeek(days);
    }
    e.setActive(d.getActive() == null ? Boolean.TRUE : d.getActive());
    e.setSortOrder(d.getSortOrder() == null ? 0 : d.getSortOrder());
    return e;
  }
}
