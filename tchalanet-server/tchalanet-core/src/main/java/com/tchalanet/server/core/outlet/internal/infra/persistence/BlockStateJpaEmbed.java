package com.tchalanet.server.core.outlet.internal.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA embeddable for BlockState. Column names are overridden per usage via @AttributeOverrides
 * in OutletJpaEntity.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class BlockStateJpaEmbed {

  @Column(name = "blocked", nullable = false)
  private boolean blocked = false;

  @Column(name = "reason")
  private String reason;

  @Column(name = "at")
  private Instant at;

  @Column(name = "by")
  private UUID by;
}
