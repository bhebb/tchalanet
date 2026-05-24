package com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment;

import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;

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
