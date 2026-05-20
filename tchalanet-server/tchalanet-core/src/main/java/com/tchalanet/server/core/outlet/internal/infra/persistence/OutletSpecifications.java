package com.tchalanet.server.core.outlet.internal.infra.persistence;

import com.tchalanet.server.core.outlet.api.query.OutletSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

public final class OutletSpecifications {

    private OutletSpecifications() {}

    public static Specification<OutletJpaEntity> matching(OutletSearchCriteria criteria) {
        return Specification.allOf(
            textMatches(criteria.q()),
            activeMatches(criteria.active()),
            salesBlockedMatches(criteria.salesBlocked()),
            dayClosedMatches(criteria.dayClosed()));
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

    private static Specification<OutletJpaEntity> activeMatches(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }

            var effectivelyActive =
                cb.and(
                    cb.isFalse(root.get("salesBlocked")),
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
                ? cb.isTrue(root.get("salesBlocked"))
                : cb.isFalse(root.get("salesBlocked"));
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
}
