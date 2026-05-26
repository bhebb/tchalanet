package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.core.outlet.api.query.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletKind;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class OutletSpecifications {

    private OutletSpecifications() {}

    public static Specification<OutletJpaEntity> matching(OutletSearchCriteria criteria) {
        return Specification.allOf(
            textMatches(criteria.q()),
            activeMatches(criteria.active()),
            salesBlockedMatches(criteria.salesBlocked()),
            dayClosedMatches(criteria.dayClosed()),
            outletBlockedMatches(criteria.outletBlocked()),
            kindMatches(criteria.kind()),
            zoneMatches(criteria.zoneId() != null ? criteria.zoneId().value() : null));
    }

    private static Specification<OutletJpaEntity> textMatches(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }
            var pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("slug")), pattern));
        };
    }

    /**
     * "Active" means operationally ready for sales:
     * status == ACTIVE && !outletBlock.blocked && !dayClosed.
     */
    private static Specification<OutletJpaEntity> activeMatches(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }
            var effectivelyActive = cb.and(
                cb.equal(root.get("status"), OutletStatus.ACTIVE),
                cb.isFalse(root.get("outletBlock").get("blocked")),
                cb.isFalse(root.get("dayClosed")));
            return active ? effectivelyActive : cb.not(effectivelyActive);
        };
    }

    private static Specification<OutletJpaEntity> salesBlockedMatches(Boolean salesBlocked) {
        return (root, query, cb) -> {
            if (salesBlocked == null) {
                return cb.conjunction();
            }
            return salesBlocked
                ? cb.isTrue(root.get("salesBlock").get("blocked"))
                : cb.isFalse(root.get("salesBlock").get("blocked"));
        };
    }

    private static Specification<OutletJpaEntity> dayClosedMatches(Boolean dayClosed) {
        return (root, query, cb) -> {
            if (dayClosed == null) {
                return cb.conjunction();
            }
            return dayClosed
                ? cb.isTrue(root.get("dayClosed"))
                : cb.isFalse(root.get("dayClosed"));
        };
    }

    private static Specification<OutletJpaEntity> outletBlockedMatches(Boolean outletBlocked) {
        return (root, query, cb) -> {
            if (outletBlocked == null) {
                return cb.conjunction();
            }
            return outletBlocked
                ? cb.isTrue(root.get("outletBlock").get("blocked"))
                : cb.isFalse(root.get("outletBlock").get("blocked"));
        };
    }

    private static Specification<OutletJpaEntity> kindMatches(OutletKind kind) {
        return (root, query, cb) -> {
            if (kind == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("kind"), kind);
        };
    }

    private static Specification<OutletJpaEntity> zoneMatches(UUID zoneId) {
        return (root, query, cb) -> {
            if (zoneId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("zoneId"), zoneId);
        };
    }
}
