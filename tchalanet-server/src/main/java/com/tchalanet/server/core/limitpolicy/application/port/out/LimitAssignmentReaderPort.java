package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitAssignment;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitTarget;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Reader port for assignments. List reads remain RLS-first; methods that require explicit tenant accept TenantId.
 */
public interface LimitAssignmentReaderPort {
    Optional<LimitAssignment> findById(LimitAssignmentId id);

    List<LimitAssignment> listByTarget(LimitTarget target);

    Optional<LimitAssignment> findByNaturalKey(LimitTarget target, LimitDefinitionId definitionId);

    List<LimitAssignment> listActiveForTargets(List<LimitTarget> targets, Instant now);

    /** Convenience: list active assignments for tenant (placeholder implementation may be more specific). */
    default List<LimitAssignment> listActive(TenantId tenantId) {
        return listActiveForTargets(List.of(), Instant.now());
    }
}
