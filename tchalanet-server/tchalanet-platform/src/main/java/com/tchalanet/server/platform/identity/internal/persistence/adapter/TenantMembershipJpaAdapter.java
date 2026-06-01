package com.tchalanet.server.platform.identity.internal.persistence.adapter;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.accesscontrol.internal.service.TenantUserRoleWriterPort;
import com.tchalanet.server.platform.identity.api.model.TenantUserStatus;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.entity.TenantUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.mapper.IdentityPersistenceMapper;
import com.tchalanet.server.platform.identity.internal.persistence.repository.TenantUserJpaRepository;
import com.tchalanet.server.platform.identity.internal.service.TenantMembership;
import com.tchalanet.server.platform.identity.internal.service.TenantUserRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantMembershipJpaAdapter implements TenantUserRoleWriterPort {

  private final TenantUserJpaRepository repository;
  private final EntityManager entityManager;

  public Optional<TenantMembership> findByTenantAndUser(TenantId tenantId, UserId userId) {
    return repository
        .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId.value(), userId.value())
        .map(IdentityPersistenceMapper::toMembership);
  }

  public Optional<Instant> findCreatedAt(TenantId tenantId, UserId userId) {
    return repository
        .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId.value(), userId.value())
        .map(TenantUserJpaEntity::getCreatedAt);
  }

  public TenantMembership upsert(TenantMembership membership) {
    var entity =
        repository
            .findByTenantIdAndUserIdAndDeletedAtIsNull(
                membership.tenantId().value(), membership.userId().value())
            .orElseGet(TenantUserJpaEntity::new);
    entity.setTenantId(membership.tenantId().value());
    entity.setUserId(membership.userId().value());
    entity.setRoleId(membership.roleId() == null ? null : membership.roleId().value());
    entity.setOutletId(membership.outletId() == null ? null : membership.outletId().value());
    entity.setTerminalId(membership.terminalId() == null ? null : membership.terminalId().value());
    entity.setStatus(membership.status());
    entity.setIsOwner(membership.owner());
    return IdentityPersistenceMapper.toMembership(repository.save(entity));
  }

  @Override
  public void setUserRole(TenantId tenantId, UserId userId, RoleId roleId) {
    repository.findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId.value(), userId.value())
        .ifPresent(entity -> {
          entity.setRoleId(roleId == null ? null : roleId.value());
          repository.save(entity);
        });
  }

  public void softDelete(TenantId tenantId, UserId userId, Instant when) {
    repository
        .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId.value(), userId.value())
        .ifPresent(
            entity -> {
              entity.setDeletedAt(when);
              repository.save(entity);
            });
  }

  public TchPage<TenantUserRow> listByTenant(TenantId tenantId, TchPageRequest pageRequest) {
    var pageable = pageRequest.pageable();
    var cb = entityManager.getCriteriaBuilder();
    var query = cb.createTupleQuery();
    var membership = query.from(TenantUserJpaEntity.class);
    var user = query.from(AppUserJpaEntity.class);
    query.multiselect(
        membership.get("userId").alias("userId"),
        user.get("username").alias("username"),
        user.get("displayName").alias("displayName"),
        user.get("email").alias("email"),
        membership.get("status").alias("status"),
        membership.get("roleId").alias("roleId"),
        membership.get("createdAt").alias("createdAt"));
    query.where(
        cb.and(
            cb.equal(membership.get("tenantId"), tenantId.value()),
            cb.equal(membership.get("userId"), user.get("id")),
            cb.isNull(membership.get("deletedAt")),
            cb.isNull(user.get("deletedAt"))));

    var typed = entityManager.createQuery(query);
    typed.setFirstResult((int) pageable.getOffset());
    typed.setMaxResults(pageable.getPageSize());
    var rows = typed.getResultStream().map(this::toRow).toList();

    var countQuery = cb.createQuery(Long.class);
    var countRoot = countQuery.from(TenantUserJpaEntity.class);
    countQuery
        .select(cb.count(countRoot))
        .where(cb.equal(countRoot.get("tenantId"), tenantId.value()), cb.isNull(countRoot.get("deletedAt")));
    var page = new PageImpl<>(rows, pageable, entityManager.createQuery(countQuery).getSingleResult());
    return TchPageMapper.map(page, row -> row);
  }

  private TenantUserRow toRow(Tuple tuple) {
    return new TenantUserRow(
        UserId.of((java.util.UUID) tuple.get("userId")),
        (String) tuple.get("username"),
        (String) tuple.get("displayName"),
        (String) tuple.get("email"),
        (TenantUserStatus) tuple.get("status"),
        tuple.get("roleId") == null
            ? null
            : com.tchalanet.server.common.types.id.RoleId.of((java.util.UUID) tuple.get("roleId")),
        (java.time.Instant) tuple.get("createdAt"));
  }
}
