package com.tchalanet.server.draw.domain.model;

import com.tchalanet.server.tenant.domain.model.TenantId;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public final class DrawChannel {
  private final DrawChannelId id;
  private final String name;
  private final TenantId tenantId;
  private final String gameCode;
  private final String timezone;
  private final LocalTime drawTime;
  private final Integer cutoffSec;
  private final List<DayOfWeek> daysOfWeek;
  private final Boolean active;
  private final Integer sortOrder;

  public DrawChannel(DrawChannelId id, String name, TenantId tenantId) {
    this(id, name, tenantId, null, null, null, null, null, null, null);
  }

  public DrawChannel(
      DrawChannelId id,
      String name,
      TenantId tenantId,
      String gameCode,
      String timezone,
      LocalTime drawTime,
      Integer cutoffSec,
      List<DayOfWeek> daysOfWeek,
      Boolean active,
      Integer sortOrder) {
    this.id = id;
    this.name = name;
    this.tenantId = tenantId;
    this.gameCode = gameCode;
    this.timezone = timezone;
    this.drawTime = drawTime;
    this.cutoffSec = cutoffSec;
    this.daysOfWeek = daysOfWeek;
    this.active = active;
    this.sortOrder = sortOrder;
  }

  public DrawChannelId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public TenantId tenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public String getGameCode() {
    return gameCode;
  }

  public String getTimezone() {
    return timezone;
  }

  public LocalTime getDrawTime() {
    return drawTime;
  }

  public Integer getCutoffSec() {
    return cutoffSec;
  }

  public java.util.List<java.time.DayOfWeek> getDaysOfWeek() {
    return daysOfWeek;
  }

  public Boolean getActive() {
    return active;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public DrawChannelId getId() {
    return id;
  }

  public TenantId getTenantId() {
    return tenantId;
  }

  // Compatibility convenience methods expected by usecases
  public String timezone() {
    return this.timezone;
  }

  public LocalTime drawTime() {
    return this.drawTime;
  }

  public String code() {
    return this.id == null ? null : this.id.toString();
  }

  public String gameCode() {
    return this.gameCode;
  }

  public Integer cutoffSec() {
    return this.cutoffSec == null ? 0 : this.cutoffSec;
  }

  public boolean isScheduledOn(DayOfWeek dow) {
    return this.daysOfWeek != null && this.daysOfWeek.contains(dow);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DrawChannel that = (DrawChannel) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "DrawChannel{" + "id=" + id + ", name='" + name + '\'' + ", tenantId=" + tenantId + '}';
  }
}
