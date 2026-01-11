package com.tchalanet.server.core.draw.infra.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Audited
@Entity
@Table(
    name = "draw_channel",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_draw_channel_tenant_code",
            columnNames = {"tenant_id", "code"}),
    indexes = {
      @Index(name = "ix_draw_channel_tenant_active", columnList = "tenant_id, active, sort_order"),
      @Index(name = "ix_draw_channel_provider", columnList = "external_provider")
    })
@Getter
@Setter
public class DrawChannelJpaEntity extends BaseTenantEntity {

  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "timezone", nullable = false, length = 64)
  private String timezone;

  @Column(name = "draw_time", nullable = false)
  private LocalTime drawTime;

  @Column(name = "cutoff_sec", nullable = false)
  private int cutoffSec = 120;

  @Column(name = "days_of_week", nullable = false, length = 32)
  private String daysOfWeek;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder = 0;

  @Column(name = "external_provider", nullable = false, length = 32)
  private String externalProvider; // NY/FL/GA/TN

  @Column(name = "flags", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode flags;

  @Column(name = "notes")
  private String notes;

  @Column(name = "result_slot_id", nullable = false)
  private UUID resultSlotId; // FK vers result_slot.id
}
