package com.tchalanet.server.core.limitpolicy.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.persistence.MapToJsonConverter;
import com.tchalanet.server.core.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.RuleKey;
import jakarta.persistence.*;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "limit_definition")
public class LimitDefinitionJpaEntity extends BaseTenantEntity {

  @Column(name = "rule_key", nullable = false)
  @Enumerated(EnumType.STRING)
  private RuleKey ruleKey;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "on_breach", nullable = false)
  @Enumerated(EnumType.STRING)
  private BreachOutcome onBreach = BreachOutcome.BLOCK;

  @Column(name = "params", nullable = false, columnDefinition = "jsonb")
  @Convert(converter = MapToJsonConverter.class)
  private Map<String, Object> params = Map.of();

  @Column(name = "applies_to", nullable = false, columnDefinition = "jsonb")
  @Convert(converter = MapToJsonConverter.class)
  private Map<String, Object> appliesTo = Map.of();

  // Getters and setters
  public RuleKey getRuleKey() {
    return ruleKey;
  }

  public void setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public BreachOutcome getOnBreach() {
    return onBreach;
  }

  public void setOnBreach(BreachOutcome onBreach) {
    this.onBreach = onBreach;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params;
  }

  public Map<String, Object> getAppliesTo() {
    return appliesTo;
  }

  public void setAppliesTo(Map<String, Object> appliesTo) {
    this.appliesTo = appliesTo;
  }
}
