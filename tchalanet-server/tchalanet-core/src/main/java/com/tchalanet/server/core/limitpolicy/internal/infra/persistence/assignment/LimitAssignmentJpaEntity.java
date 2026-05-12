package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.assignment;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.enums.ScopeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "limit_assignment",
    indexes = {
        @Index(
            name = "idx_limit_assignment_scope",
            columnList = "scope_type, scope_id"
        ),
        @Index(
            name = "idx_limit_assignment_rule",
            columnList = "rule_key"
        )
    }
)
@Getter
@Setter
@Audited
public class LimitAssignmentJpaEntity extends BaseTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_key", nullable = false, length = 80)
    private RuleKey ruleKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private ScopeType scopeType;

    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "on_breach", nullable = false, length = 32)
    private BreachOutcome onBreach = BreachOutcome.BLOCK;

    @Column(name = "params", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode params;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;
}
