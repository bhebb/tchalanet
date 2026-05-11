package com.tchalanet.server.core.limitpolicy.application.port.out.assignment;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LimitAssignmentReaderPort {

    Optional<LimitAssignment> findById(LimitAssignmentId id);

    Optional<LimitAssignment> findByNaturalKey(
        LimitScopeRef scope,
        RuleKey ruleKey
    );

    List<LimitAssignment> listActiveForTargets(
        List<LimitScopeRef> scopes,
        Instant now
    );

    List<LimitAssignment> listByTarget(LimitScopeRef scope);
}
