package com.tchalanet.server.catalog.drawchannel.internal.persistence;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import com.tchalanet.server.common.persistence.BaseTenantEntity;

@Entity
@Table(name = "draw_channel")
@Audited
@Getter
@Setter
public class DrawChannelEntity extends BaseTenantEntity {

  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "timezone", nullable = false, length = 64)
  private String timezone;

  @Column(name = "draw_time", nullable = false)
  private LocalTime drawTime;

  @Column(name = "sales_open_time")
  private LocalTime salesOpenTime;

  @Column(name = "cutoff_sec", nullable = false)
  private int cutoffSec = 120;

  @Column(name = "days_of_week", nullable = false, length = 32)
  private String daysOfWeek;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder = 0;

  @Column(name = "period", length = 32)
  private String period;

  @Column(name = "flags", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode flags;

  @Column(name = "notes")
  private String notes;

  @Column(name = "result_slot_id", nullable = false)
  private UUID resultSlotId;
}
