package com.tchalanet.server.core.terminal.internal.infra.persistence.sellerterminal;

import com.tchalanet.server.core.terminal.api.query.SellerTerminalSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

public final class SellerTerminalSpecifications {

    private SellerTerminalSpecifications() {}

    public static Specification<SellerTerminalJpaEntity> matching(SellerTerminalSearchCriteria c) {
        return Specification.allOf(
            textMatches(c.q()),
            statusMatches(c.status()),
            outletMatches(c.outletId()));
    }

    private static Specification<SellerTerminalJpaEntity> textMatches(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            var pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("terminalCode")), pattern),
                cb.like(cb.lower(root.get("displayName")), pattern),
                cb.like(cb.lower(root.get("phoneNumber")), pattern));
        };
    }

    private static Specification<SellerTerminalJpaEntity> statusMatches(
        com.tchalanet.server.core.terminal.api.model.SellerTerminalStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    private static Specification<SellerTerminalJpaEntity> outletMatches(
        com.tchalanet.server.common.types.id.OutletId outletId) {
        return (root, query, cb) -> {
            if (outletId == null) return cb.conjunction();
            return cb.equal(root.get("outletId"), outletId.value());
        };
    }
}
