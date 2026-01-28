package com.tchalanet.server.core.tenantuser.infra.persistence;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserReaderPort;
import com.tchalanet.server.core.tenantuser.application.query.model.TenantUserDetails;
import com.tchalanet.server.core.tenantuser.application.query.model.TenantUserRow;
import com.tchalanet.server.core.tenantuser.application.query.model.TenantUserSearchCriteria;
import com.tchalanet.server.core.tenantuser.domain.model.TenantUserMembership;
import com.tchalanet.server.core.user.infra.persistence.AppUserJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantUserQueryAdapter implements TenantUserReaderPort {

    private final EntityManager em;

    @Override
    public TchPage<TenantUserRow> pagedListByTenant(TenantId tenantId, TchPageRequest pageReq) {
        return searchInternal(tenantId, null, pageReq);
    }

    @Override
    public TchPage<TenantUserRow> searchByTenant(
        TenantId tenantId, TenantUserSearchCriteria criteria, TchPageRequest pageReq) {
        return searchInternal(tenantId, criteria, pageReq);
    }

    @Override
    public TenantUserDetails getDetails(TenantId tenantId, UserId userId) {
        var cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<TenantUserJpaEntity> tu = cq.from(TenantUserJpaEntity.class);
        Join<TenantUserJpaEntity, AppUserJpaEntity> u = tu.join("user", JoinType.INNER);

        cq.multiselect(
            tu.get("userId").alias("userId"),
            u.get("username").alias("username"),
            u.get("displayName").alias("displayName"),
            u.get("email").alias("email"),
            tu.get("status").alias("status"),
            tu.get("autonomyLevel").alias("autonomyLevel"),
            tu.get("roleId").alias("roleId"),
            tu.get("createdAt").alias("createdAt"));

        cq.where(
            cb.and(
                cb.equal(tu.get("tenantId"), tenantId.value()),
                cb.equal(tu.get("userId"), userId.value()),
                cb.isNull(tu.get("deletedAt")),
                cb.isNull(u.get("deletedAt"))));

        return em.createQuery(cq)
            .getResultStream()
            .findFirst()
            .map(this::toDetails)
            .orElse(null);
    }

    // ---------------------------------------------------------------------
    // Internal (shared) search impl: rows + count with identical predicates
    // ---------------------------------------------------------------------

    private TchPage<TenantUserRow> searchInternal(
        TenantId tenantId, TenantUserSearchCriteria criteria, TchPageRequest pageReq) {

        var pageable = pageReq.pageable();
        var cb = em.getCriteriaBuilder();

        // ---------- main query ----------
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<TenantUserJpaEntity> tu = cq.from(TenantUserJpaEntity.class);
        Join<TenantUserJpaEntity, AppUserJpaEntity> u = tu.join("user", JoinType.INNER);

        cq.multiselect(
            tu.get("userId").alias("userId"),
            u.get("username").alias("username"),
            u.get("displayName").alias("displayName"),
            u.get("email").alias("email"),
            tu.get("status").alias("status"),
            tu.get("autonomyLevel").alias("autonomyLevel"),
            tu.get("roleId").alias("roleId"),
            tu.get("createdAt").alias("createdAt"));

        var predicates = buildPredicates(cb, tu, u, tenantId, criteria);
        cq.where(predicates.toArray(new Predicate[0]));

        applySort(cb, cq, tu, u, pageable);

        TypedQuery<Tuple> q = em.createQuery(cq);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());

        List<TenantUserRow> rows = q.getResultStream().map(this::toRow).toList();

        // ---------- count query (same predicates!) ----------
        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<TenantUserJpaEntity> ctu = countQ.from(TenantUserJpaEntity.class);
        Join<TenantUserJpaEntity, AppUserJpaEntity> cu = ctu.join("user", JoinType.INNER);

        countQ.select(cb.count(ctu));
        var countPreds = buildPredicates(cb, ctu, cu, tenantId, criteria);
        countQ.where(countPreds.toArray(new Predicate[0]));

        long total = em.createQuery(countQ).getSingleResult();

        var pageImpl = new org.springframework.data.domain.PageImpl<>(rows, pageable, total);
        return TchPageMapper.map(pageImpl, r -> r);
    }

    private List<Predicate> buildPredicates(
        CriteriaBuilder cb,
        Root<TenantUserJpaEntity> tu,
        Join<TenantUserJpaEntity, AppUserJpaEntity> u,
        TenantId tenantId,
        TenantUserSearchCriteria criteria) {

        List<Predicate> preds = new ArrayList<>();
        preds.add(cb.equal(tu.get("tenantId"), tenantId.value()));
        preds.add(cb.isNull(tu.get("deletedAt")));
        preds.add(cb.isNull(u.get("deletedAt")));

        if (criteria == null) return preds;

        criteria.text()
            .filter(s -> !s.isBlank())
            .ifPresent(
                s -> {
                    String like = "%" + s.trim().toLowerCase() + "%";
                    preds.add(
                        cb.or(
                            cb.like(cb.lower(u.get("username")), like),
                            cb.like(cb.lower(u.get("displayName")), like),
                            cb.like(cb.lower(u.get("email")), like)));
                });

        criteria.roleId().ifPresent(r -> preds.add(cb.equal(tu.get("roleId"), r.value())));
        criteria.status().ifPresent(st -> preds.add(cb.equal(tu.get("status"), st)));
        criteria.autonomyLevel().ifPresent(al -> preds.add(cb.equal(tu.get("autonomyLevel"), al)));

        return preds;
    }

    private void applySort(
        CriteriaBuilder cb,
        CriteriaQuery<?> cq,
        Root<TenantUserJpaEntity> tu,
        Join<TenantUserJpaEntity, AppUserJpaEntity> u,
        org.springframework.data.domain.Pageable pageable) {

        if (!pageable.getSort().isSorted()) {
            cq.orderBy(cb.desc(tu.get("createdAt")));
            return;
        }

        List<Order> orders = new ArrayList<>();
        pageable
            .getSort()
            .forEach(
                s -> {
                    String prop = s.getProperty();
                    Path<?> path =
                        switch (prop) {
                            case "username", "displayName", "email" -> u.get(prop);
                            case "createdAt", "status", "autonomyLevel", "roleId" -> tu.get(prop);
                            default -> tu.get("createdAt");
                        };
                    orders.add(s.isAscending() ? cb.asc(path) : cb.desc(path));
                });

        cq.orderBy(orders);
    }

    private TenantUserRow toRow(Tuple t) {
        return new TenantUserRow(
            UserId.of((java.util.UUID) t.get("userId")),
            (String) t.get("username"),
            (String) t.get("displayName"),
            (String) t.get("email"),
            (com.tchalanet.server.common.types.enums.TenantUserStatus) t.get("status"),
            (com.tchalanet.server.common.types.enums.AutonomyLevel) t.get("autonomyLevel"),
            t.get("roleId") == null ? null : RoleId.of((java.util.UUID) t.get("roleId")),
            (java.time.Instant) t.get("createdAt"));
    }

    private TenantUserDetails toDetails(Tuple t) {
        return new TenantUserDetails(
            UserId.of((java.util.UUID) t.get("userId")),
            (String) t.get("username"),
            (String) t.get("displayName"),
            (String) t.get("email"),
            (com.tchalanet.server.common.types.enums.TenantUserStatus) t.get("status"),
            (com.tchalanet.server.common.types.enums.AutonomyLevel) t.get("autonomyLevel"),
            t.get("roleId") == null ? null : RoleId.of((java.util.UUID) t.get("roleId")),
            (java.time.Instant) t.get("createdAt"));
    }

    @Override
    public Optional<TenantUserMembership> findByTenantIdAndUserId(TenantId tenantId, UserId userId) {

        var cb = em.getCriteriaBuilder();
        CriteriaQuery<TenantUserJpaEntity> cq = cb.createQuery(TenantUserJpaEntity.class);
        Root<TenantUserJpaEntity> tu = cq.from(TenantUserJpaEntity.class);

        cq.select(tu);
        cq.where(
            cb.and(
                cb.equal(tu.get("tenantId"), tenantId.value()),
                cb.equal(tu.get("userId"), userId.value()),
                cb.isNull(tu.get("deletedAt"))
            )
        );

        TypedQuery<TenantUserJpaEntity> q = em.createQuery(cq);
        List<TenantUserJpaEntity> list = q.setMaxResults(1).getResultList();
        if (list.isEmpty()) return Optional.empty();

        TenantUserJpaEntity e = list.get(0);
        var m = TenantUserMembership.of(tenantId, userId);
        if (e.getRoleId() != null) m.assignRole(RoleId.of(e.getRoleId()));
        if (e.getAutonomyLevel() != null) m.changeAutonomy(e.getAutonomyLevel());
        if (e.getIsOwner() != null && e.getIsOwner()) m.markOwner(true);
        if (e.getStatus() != null && e.getStatus() == com.tchalanet.server.common.types.enums.TenantUserStatus.SUSPENDED) m.suspend();
        return Optional.of(m);
    }
}
