package com.tchalanet.server.core.limitpolicy.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Audited
@Table(name = "limit_definition")
@Getter
@Setter
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
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode params;

  @Column(name = "applies_to", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode appliesTo;
}
