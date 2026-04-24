package com.tchalanet.server.core.autonomy.domain.model;

import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyPolicyRuleId;
import com.tchalanet.server.core.autonomy.domain.ids.AutonomyTargetId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AutonomyPolicyRule {

    private AutonomyPolicyRuleId id;
    private AutonomyTargetType targetType;
    private AutonomyTargetId targetId;

    private AutonomyLevel level;
    private boolean requireApprovalOnBlock;
    private ApprovalRole approvalRole;

    private boolean enabled;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;

    private Long version; // optimistic locking / version
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private boolean deleted; // soft-delete flag (infra info surfaced for overview)

    // Explicit getter to ensure static analyses (without Lombok processing) see this method
    public Long getVersion() {
        return this.version;
    }
}
