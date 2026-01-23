package com.tchalanet.server.catalog.resultslot.internal.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Audited
@Entity
@Table(name = "result_slot")
@Getter
@Setter
public class ResultSlotJpaEntity extends BaseEntity {

  @Column(name = "slot_key", nullable = false, length = 32, unique = true)
  private String slotKey;

  @Column(name = "provider", nullable = false, length = 16)
  private String provider;

  @Column(name = "timezone", nullable = false, length = 64)
  private String timezone;

  @Column(name = "draw_time", nullable = false)
  private LocalTime drawTime;

  @Column(name = "days_of_week", nullable = false, length = 32)
  private String daysOfWeek;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder = 0;

  @Column(name = "source_cfg", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode sourceCfg;

  @Column(name = "projection_cfg", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode projectionCfg;

  @Column(name = "notes")
  private String notes;

  // Mapped to SQL column `label_key` (optional i18n key for slot label)
  @Column(name = "label_key", length = 256)
  private String labelKey;
}
