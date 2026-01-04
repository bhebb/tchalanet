package com.tchalanet.server.core.draw.domain.model;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * DrawChannel (tenant-scoped)
 *
 * <p>Représente un canal de tirage (ex: US_NY_NUM3_MID) avec son calendrier et le mapping vers un
 * provider externe. Définit quand les draws doivent exister et leurs règles simples (cutoff,
 * timezone, jours activés).
 */
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name", "tenantId"})
@AllArgsConstructor
public final class DrawChannel {
  private DrawChannelId id;
  private String name;
  private String label; // pour UI back-office
  private TenantId tenantId;
  private String code;
  private ZoneId timezone;
  private LocalTime drawTime;
  private Integer cutoffSec;
  private List<DayOfWeek> daysOfWeek;
  private Boolean active;
  private Integer sortOrder;
  private DrawSource defaultSource; // EXTERNAL_NY, MANUAL_HT, etc.

  public DrawChannelId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public TenantId tenantId() {
    return tenantId;
  }

  public String code() {
    return code;
  }

  public String label() {
    return label;
  }

  public LocalTime drawTime() {
    return drawTime;
  }

  public Integer cutoffSec() {
    return cutoffSec;
  }

  public List<DayOfWeek> daysOfWeek() {
    return daysOfWeek;
  }

  public Integer sortOrder() {
    return sortOrder;
  }

  public ZoneId timezone() {
    return timezone;
  }

  public boolean isActive() {
    return active;
  }

  public DrawSource defaultSource() {
    return defaultSource;
  }

  public void rename(String newLabel) {
    this.label = Objects.requireNonNull(newLabel);
  }

  public void changeTimezone(ZoneId newZone) {
    this.timezone = Objects.requireNonNull(newZone);
  }

  public void activate() {
    this.active = true;
  }

  public void deactivate() {
    this.active = false;
  }

  public void changeDefaultSource(DrawSource newSource) {
    this.defaultSource = Objects.requireNonNull(newSource);
  }
}
