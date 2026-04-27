package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.common.types.enums.UserStatus;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class AppUserPersistenceAdapter implements UserReaderPort, UserWriterPort {

  private final JpaAppUserRepository jpa;
  private final EntityManager em;

  @Override
  public Optional<AppUser> findById(UserId id) {
    return jpa.findById(id.value()).map(UserMapper::toDomain);
  }

  @Override
  public Optional<AppUser> findByKeycloakSub(KeycloakUserSub keycloakSub) {
    if (keycloakSub == null) return Optional.empty();
    return jpa.findByKeycloakSub(keycloakSub.value()).map(UserMapper::toDomain);
  }

  @Override
  public Optional<AppUser> findByEmail(String email) {
    return jpa.findByEmail(email).map(UserMapper::toDomain);
  }

  @Override
  public Optional<AppUser> findByEmailOrPhone(String email, String phone) {
    return jpa.findByEmailOrPhone(email, phone).map(UserMapper::toDomain);
  }

  @Override
  public AppUser save(AppUser user) {
    AppUserJpaEntity entity;
    if (user.getId() != null) {
      entity = jpa.findById(user.getId().value()).orElseGet(AppUserJpaEntity::new);
      entity.setId(user.getId().value());
      UserMapper.merge(entity, user);
    } else {
      entity = UserMapper.toNewEntity(user);
    }
    var saved = jpa.save(entity);
    return UserMapper.toDomain(saved);
  }

  @Override
  public void softDelete(UserId userId, Instant when) {
    jpa.findById(userId.value())
        .ifPresent(
            e -> {
              e.setDeletedAt(when);
              jpa.save(e);
            });
  }

  @Override
  public Page<AppUser> findAll(Pageable pageable) {
    var page = jpa.findAll(pageable);
    var content = page.getContent().stream().map(UserMapper::toDomain).collect(Collectors.toList());
    return new PageImpl<>(content, pageable, page.getTotalElements());
  }

  @Override
  public Page<AppUser> findByTenantId(TenantId tenantId, Pageable pageable) {
    var page = jpa.findAll(pageable);
    var content = page.getContent().stream().map(UserMapper::toDomain).collect(Collectors.toList());
    return new PageImpl<>(content, pageable, page.getTotalElements());
  }

  @Override
  public Page<@NotNull AppUser> findAllActiveUsers(Pageable pageable) {
    var page = jpa.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE.name(), pageable);
    var content = page.getContent().stream().map(UserMapper::toDomain).collect(Collectors.toList());
    return new PageImpl<>(content, pageable, page.getTotalElements());
  }

  @Override
  public Page<AppUser> findAllActiveUsersByTenant(TenantId tenantId, Pageable pageable) {
    var page = jpa.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE.name(), pageable);
    var content = page.getContent().stream().map(UserMapper::toDomain).collect(Collectors.toList());
    return new PageImpl<>(content, pageable, page.getTotalElements());
  }

  @Override
  public void updateStatus(UserId userId, UserStatus userStatus) {
    jpa.findById(userId.value())
        .ifPresent(
            e -> {
              e.setStatus(userStatus);
              jpa.save(e);
            });
  }

  @Override
  public Page<AppUser> searchByCriteria(String nameLike, String status, Instant createdAfter, Instant createdBefore, Pageable pageable) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<AppUserJpaEntity> cq = cb.createQuery(AppUserJpaEntity.class);
    Root<AppUserJpaEntity> root = cq.from(AppUserJpaEntity.class);

    List<Predicate> preds = new ArrayList<>();
    // ignore deleted
    preds.add(cb.isNull(root.get("deletedAt")));

    if (nameLike != null && !nameLike.isBlank()) {
      String like = "%" + nameLike.trim().toLowerCase() + "%";
      preds.add(
          cb.or(
              cb.like(cb.lower(root.get("firstName")), like), cb.like(cb.lower(root.get("lastName")), like), cb.like(cb.lower(root.get("displayName")), like)));
    }

    if (status != null && !status.isBlank()) {
      preds.add(cb.equal(root.get("status"), status));
    }

    if (createdAfter != null) {
      preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
    }
    if (createdBefore != null) {
      preds.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
    }

    cq.where(preds.toArray(new Predicate[0]));

    // order
    if (pageable.getSort().isSorted()) {
      List<Order> orders = new ArrayList<>();
      pageable.getSort().forEach(o -> orders.add(o.isAscending() ? cb.asc(root.get(o.getProperty())) : cb.desc(root.get(o.getProperty()))));
      cq.orderBy(orders);
    }

    TypedQuery<AppUserJpaEntity> q = em.createQuery(cq.select(root));
    q.setFirstResult((int) pageable.getOffset());
    q.setMaxResults(pageable.getPageSize());
    List<AppUserJpaEntity> result = q.getResultList();

    // count
    CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
    Root<AppUserJpaEntity> countRoot = countQ.from(AppUserJpaEntity.class);
    countQ.select(cb.count(countRoot)).where(preds.toArray(new Predicate[0]));
    Long total = em.createQuery(countQ).getSingleResult();

    var content = result.stream().map(UserMapper::toDomain).collect(Collectors.toList());
    return new PageImpl<>(content, pageable, total);
  }
}
