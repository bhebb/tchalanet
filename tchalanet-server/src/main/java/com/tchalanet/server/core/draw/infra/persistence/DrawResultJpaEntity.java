package com.tchalanet.server.core.draw.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.persistence.ListToJsonConverter;
import com.tchalanet.server.common.persistence.MapToJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(
    name = "draw_result",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_draw_result_tenant_draw",
            columnNames = {"tenant_id", "draw_id"}))
@Audited
@Getter
@Setter
public class DrawResultJpaEntity extends BaseTenantEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "draw_id", nullable = false)
  private DrawJpaEntity draw;

  @Column(name = "source", nullable = false)
  private String source;

  @Column(name = "status", nullable = false)
  private String status;

  @Convert(converter = ListToJsonConverter.class)
  @Column(name = "numbers_main", columnDefinition = "jsonb", nullable = false)
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
  private UUID overriddenBy;

  @Column(name = "override_reason")
  private String overrideReason;
}
