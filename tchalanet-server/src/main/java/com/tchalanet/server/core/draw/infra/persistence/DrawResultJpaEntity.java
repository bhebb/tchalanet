package com.tchalanet.server.core.draw.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.persistence.ListToJsonConverter;
import com.tchalanet.server.common.persistence.MapToJsonConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "draw_result")
@Getter
@Setter
@NoArgsConstructor
public class DrawResultJpaEntity extends BaseTenantEntity {

  // relation vers draw
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "draw_id", nullable = false)
  private DrawJpaEntity draw;

  @Column(name = "source", nullable = false)
  private String source;

  @Column(name = "status", nullable = false)
  private String status;

  // store numbers arrays as jsonb list for simplicity
  @Convert(converter = ListToJsonConverter.class)
  @Column(name = "numbers_main", columnDefinition = "jsonb")
  private List<String> numbersMain;

  @Convert(converter = ListToJsonConverter.class)
  @Column(name = "numbers_extra", columnDefinition = "jsonb")
  private List<String> numbersExtra;

  @Convert(converter = MapToJsonConverter.class)
  @Column(name = "raw_payload", columnDefinition = "jsonb")
  private Map<String, Object> rawPayload;

  @Column(name = "overridden_at")
  private Instant overriddenAt;

  @Column(name = "overridden_by")
  private java.util.UUID overriddenBy;

  @Column(name = "override_reason")
  private String overrideReason;
}
