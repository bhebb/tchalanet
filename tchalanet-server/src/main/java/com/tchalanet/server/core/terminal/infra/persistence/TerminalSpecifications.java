package com.tchalanet.server.core.terminal.infra.persistence;


import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

public final class TerminalSpecifications {

    private TerminalSpecifications() {
    }

    public static Specification<TerminalJpaEntity> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<TerminalJpaEntity> byOutlet(OutletId outletId) {
        return (root, query, cb) -> {
            if (outletId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("outletId"), outletId.value());
        };
    }

    public static Specification<TerminalJpaEntity> matching(TerminalSearchCriteria criteria) {
        return Specification.allOf(
            textMatches(criteria.q()),
            byOutlet(criteria.outletId()),
            assignedUserMatches(criteria.assignedUserId()),
            kindMatches(criteria.kind()),
            stateMatches(criteria.state()),
            syncStateMatches(criteria.syncState()),
            autoSessionEnabledMatches(criteria.autoSessionEnabled()));
    }

    private static Specification<TerminalJpaEntity> textMatches(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }

            var pattern = "%" + q.trim().toLowerCase() + "%";

            return cb.or(
                cb.like(cb.lower(root.get("label")), pattern),
                cb.like(cb.lower(root.get("inventoryTag")), pattern));
        };
    }

    private static Specification<TerminalJpaEntity> assignedUserMatches(Object assignedUserId) {
        return (root, query, cb) -> {
            if (assignedUserId == null) {
                return cb.conjunction();
            }

            var id = ((com.tchalanet.server.common.types.id.UserId) assignedUserId).value();
            return cb.equal(root.get("assignedUserId"), id);
        };
    }

    private static Specification<TerminalJpaEntity> kindMatches(Object kind) {
        return (root, query, cb) -> {
            if (kind == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("kind"), kind.toString());
        };
    }

    private static Specification<TerminalJpaEntity> stateMatches(Object state) {
        return (root, query, cb) -> {
            if (state == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("state"), state.toString());
        };
    }

    private static Specification<TerminalJpaEntity> syncStateMatches(Object syncState) {
        return (root, query, cb) -> {
            if (syncState == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("syncState"), syncState.toString());
        };
    }

    private static Specification<TerminalJpaEntity> autoSessionEnabledMatches(Boolean value) {
        return (root, query, cb) -> {
            if (value == null) {
                return cb.conjunction();
            }

            return value
                ? cb.isTrue(root.get("autoSessionEnabled"))
                : cb.isFalse(root.get("autoSessionEnabled"));
        };
    }
}
