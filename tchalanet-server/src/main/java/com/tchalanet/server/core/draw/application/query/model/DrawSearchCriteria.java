package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.Objects;

public final class DrawSearchCriteria {

  private final TenantId tenantId;
  private final String channelCode;
  private final LocalDate from;
  private final LocalDate to;
  private final int days;

  public DrawSearchCriteria(
      TenantId tenantId, String channelCode, LocalDate from, LocalDate to, int days) {
    this.tenantId = tenantId;
    this.channelCode = channelCode;
    this.from = from;
    this.to = to;
    this.days = days;
  }

  public TenantId tenantId() {
    return tenantId;
  }

  public String channelCode() {
    return channelCode;
  }

  public LocalDate from() {
    return from;
  }

  public LocalDate to() {
    return to;
  }

  public int days() {
    return days;
  }

  public static DrawSearchCriteria today(TenantId tenantId, String channelCode) {
    LocalDate today = LocalDate.now();
    return new DrawSearchCriteria(tenantId, channelCode, today, today, 1);
  }

  public static DrawSearchCriteria lastDays(TenantId tenantId, String channelCode, int days) {
    LocalDate to = LocalDate.now();
    LocalDate from = to.minusDays(days);
    return new DrawSearchCriteria(tenantId, channelCode, from, to, days);
  }

  public static DrawSearchCriteria of(
      TenantId tenantId, String channelCode, LocalDate from, LocalDate to) {
    // Protect against null dates; use today as default
    LocalDate now = LocalDate.now();
    LocalDate f = from == null ? now : from;
    LocalDate t = to == null ? now : to;
    // normalize so from <= to
    if (f.isAfter(t)) {
      LocalDate tmp = f;
      f = t;
      t = tmp;
    }
    // compute inclusive days (same-day => 1)
    int computedDays = (int) (t.toEpochDay() - f.toEpochDay()) + 1;
    return new DrawSearchCriteria(tenantId, channelCode, f, t, computedDays);
  }

  @Override
  public String toString() {
    return "DrawSearchCriteria{"
        + "tenantId="
        + tenantId
        + ", drawChannelCode='"
        + channelCode
        + '\''
        + ", from="
        + from
        + ", to="
        + to
        + ", days="
        + days
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DrawSearchCriteria that = (DrawSearchCriteria) o;
    return days == that.days
        && Objects.equals(tenantId, that.tenantId)
        && Objects.equals(channelCode, that.channelCode)
        && Objects.equals(from, that.from)
        && Objects.equals(to, that.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, channelCode, from, to, days);
  }
}
