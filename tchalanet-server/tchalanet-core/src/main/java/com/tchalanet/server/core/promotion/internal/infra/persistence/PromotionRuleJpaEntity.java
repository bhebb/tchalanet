package com.tchalanet.server.core.promotion.internal.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotion_rule")
public class PromotionRuleJpaEntity extends BaseTenantEntity {

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  private boolean active;

  @Column(name = "rule_type", nullable = false)
  private String ruleType;

  @Column(name = "engine_type", nullable = false)
  private String engineType;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @Column(name = "rule_version", nullable = false)
  private int ruleVersion;

  private int priority;
  private boolean stackable;

  @Column(name = "exclusive_group")
  private String exclusiveGroup;

  @Column(name = "starts_at")
  private Instant startsAt;

  @Column(name = "ends_at")
  private Instant endsAt;

  private String timezone;

  @Column(name = "condition_json", columnDefinition = "jsonb")
  private String conditionJson;

  @Column(name = "effect_json", columnDefinition = "jsonb")
  private String effectJson;

  @Column(name = "offline_allowed")
  private boolean offlineAllowed;

  @Column(name = "archived_at")
  private Instant archivedAt;
}
