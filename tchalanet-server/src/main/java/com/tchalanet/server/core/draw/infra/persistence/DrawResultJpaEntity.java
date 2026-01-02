package com.tchalanet.server.core.draw.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.common.persistence.ListToJsonConverter;
import com.tchalanet.server.common.persistence.MapToJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "draw_result",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_draw_result_channel_date",
            columnNames = {"channel_code", "draw_date"}))
@Audited
@Getter
@Setter
public class DrawResultJpaEntity extends BaseEntity {

  @Column(name = "channel_code", nullable = false)
  private String channelCode;

  @Column(name = "draw_date", nullable = false)
  private LocalDate drawDate;

  @Column(name = "occurred_at")
  private Instant occurredAt;

  @Convert(converter = ListToJsonConverter.class)
  @Column(name = "numbers_main", columnDefinition = "jsonb", nullable = false)
  private List<String> numbersMain;

  @Convert(converter = ListToJsonConverter.class)
  @Column(name = "numbers_extra", columnDefinition = "jsonb")
  private List<String> numbersExtra;

  @Column(name = "quality", nullable = false)
  private String quality;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "source", nullable = false)
  private String source;

  @Column(name = "source_hash")
  private String sourceHash;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "raw_payload", columnDefinition = "jsonb")
  private Map<String, Object> rawPayload;

  @Column(name = "override_reason")
  private String overrideReason;

  @Column(name = "fetched_at")
  private Instant fetchedAt;
}
