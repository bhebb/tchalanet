package com.tchalanet.server.platform.identity.internal.persistence.adapter;

import com.tchalanet.server.common.types.enums.UserStatus;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.mapper.IdentityPersistenceMapper;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserJpaRepository;
import com.tchalanet.server.platform.identity.internal.service.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppUserJpaAdapter {

  private final AppUserJpaRepository repository;
  private final EntityManager entityManager;

  public Optional<AppUser> findById(UserId id) {
    return repository.findById(id.value()).map(IdentityPersistenceMapper::toUser);
  }

  public Optional<AppUser> findByKeycloakSub(KeycloakUserSub keycloakSub) {
    return keycloakSub == null
        ? Optional.empty()
        : repository.findByKeycloakSub(keycloakSub.value()).map(IdentityPersistenceMapper::toUser);
  }

  public Optional<AppUser> findByEmailOrPhone(String email, String phone) {
    return repository.findByEmailOrPhone(email, phone).map(IdentityPersistenceMapper::toUser);
  }

  public AppUser save(AppUser user) {
    var entity =
        user.id() == null
            ? new AppUserJpaEntity()
            : repository.findById(user.id().value()).orElseGet(AppUserJpaEntity::new);
    IdentityPersistenceMapper.merge(entity, user);
    return IdentityPersistenceMapper.toUser(repository.save(entity));
  }

  public void softDelete(UserId userId, Instant when) {
    repository
        .findById(userId.value())
        .ifPresent(
            entity -> {
              entity.setDeletedAt(when);
              repository.save(entity);
            });
  }

  public Page<AppUser> findAll(Pageable pageable) {
    var page = repository.findAll(pageable);
    return new PageImpl<>(
        page.getContent().stream().map(IdentityPersistenceMapper::toUser).toList(),
        pageable,
        page.getTotalElements());
  }

  public Page<AppUser> findByTenantId(TenantId tenantId, Pageable pageable) {
    var page = repository.findByTenantMembership(tenantId.value(), pageable);
    return new PageImpl<>(
        page.getContent().stream().map(IdentityPersistenceMapper::toUser).toList(),
        pageable,
        page.getTotalElements());
  }

  public Page<AppUser> search(
      String nameLike, String status, Instant createdAfter, Instant createdBefore, Pageable pageable) {
    var cb = entityManager.getCriteriaBuilder();
    var query = cb.createQuery(AppUserJpaEntity.class);
    var root = query.from(AppUserJpaEntity.class);
    var predicates = new ArrayList<Predicate>();
    predicates.add(cb.isNull(root.get("deletedAt")));

    if (nameLike != null && !nameLike.isBlank()) {
      var like = "%" + nameLike.trim().toLowerCase() + "%";
      predicates.add(
          cb.or(
              cb.like(cb.lower(root.get("firstName")), like),
              cb.like(cb.lower(root.get("lastName")), like),
              cb.like(cb.lower(root.get("displayName")), like)));
    }
    if (status != null && !status.isBlank()) {
      predicates.add(cb.equal(root.get("status"), UserStatus.valueOf(status)));
    }
    if (createdAfter != null) {
      predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
    }
    if (createdBefore != null) {
      predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
    }

    query.where(predicates.toArray(new Predicate[0]));
    var typed = entityManager.createQuery(query);
    typed.setFirstResult((int) pageable.getOffset());
    typed.setMaxResults(pageable.getPageSize());

    var countQuery = cb.createQuery(Long.class);
    var countRoot = countQuery.from(AppUserJpaEntity.class);
    var countPredicates = new ArrayList<Predicate>();
    countPredicates.add(cb.isNull(countRoot.get("deletedAt")));
    countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));

    var rows = typed.getResultList().stream().map(IdentityPersistenceMapper::toUser).toList();
    return new PageImpl<>(rows, pageable, entityManager.createQuery(countQuery).getSingleResult());
  }
}
