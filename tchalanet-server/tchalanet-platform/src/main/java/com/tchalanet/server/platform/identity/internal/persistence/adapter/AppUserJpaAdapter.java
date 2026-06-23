package com.tchalanet.server.platform.identity.internal.persistence.adapter;

import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserExternalIdentityJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.entity.AppUserJpaEntity;
import com.tchalanet.server.platform.identity.internal.persistence.mapper.IdentityPersistenceMapper;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserExternalIdentityJpaRepository;
import com.tchalanet.server.platform.identity.internal.persistence.repository.AppUserJpaRepository;
import com.tchalanet.server.platform.identity.internal.model.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AppUserJpaAdapter {

    private final AppUserJpaRepository repository;
    private final AppUserExternalIdentityJpaRepository externalIdentities;
    private final EntityManager entityManager;

    public Optional<AppUser> findById(UserId id) {
        return repository.findById(id.value()).map(this::toUser);
    }

    public Optional<AppUser> findByKeycloakSub(KeycloakUserSub keycloakSub) {
        return keycloakSub == null
            ? Optional.empty()
            : externalIdentities
                .findFirstByProviderAndExternalSubject(
                    IdentityProviderType.KEYCLOAK, keycloakSub.value().toString())
                .flatMap(identity -> repository.findById(identity.getAppUserId()))
                .map(this::toUser);
    }

    public Optional<AppUser> findByEmailOrPhone(String email, String phone) {
        // Dispatch to single-field finders: Spring Data translates a null argument to "= null"
        // into "IS NULL", so findByEmailOrPhone(email, null) becomes "email = ? OR phone IS NULL"
        // and matches every phone-less row, yielding a NonUniqueResultException. Only query the
        // fields actually provided.
        var hasEmail = email != null && !email.isBlank();
        var hasPhone = phone != null && !phone.isBlank();
        if (hasEmail) {
            var byEmail = repository.findByEmail(email).map(this::toUser);
            if (byEmail.isPresent() || !hasPhone) {
                return byEmail;
            }
        }
        if (hasPhone) {
            return repository.findByPhone(phone).map(this::toUser);
        }
        return Optional.empty();
    }

    public AppUser save(AppUser user) {
        var entity =
            user.id() == null
                ? new AppUserJpaEntity()
                : repository.findById(user.id().value()).orElseGet(AppUserJpaEntity::new);
        IdentityPersistenceMapper.merge(entity, user);
        var saved = repository.save(entity);
        persistKeycloakIdentity(saved, user.keycloakSub());
        return toUser(saved);
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
            page.getContent().stream().map(this::toUser).toList(),
            pageable,
            page.getTotalElements());
    }

    public Page<AppUser> findByTenantId(TenantId tenantId, Pageable pageable) {
        var page = repository.findByTenantMembership(tenantId.value(), pageable);
        return new PageImpl<>(
            page.getContent().stream().map(this::toUser).toList(),
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

        var rows = typed.getResultList().stream().map(this::toUser).toList();
        return new PageImpl<>(rows, pageable, entityManager.createQuery(countQuery).getSingleResult());
    }

    public List<AppUser> findUnassigned(String q, int page, int size) {
        String nameLike = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        int offset = page * size;
        return repository.findUnassigned(nameLike, size, offset)
            .stream().map(this::toUser).toList();
    }

    public Optional<String> findExternalSubject(UserId userId, IdentityProviderType provider) {
        return externalIdentities
            .findFirstByAppUserIdAndProvider(userId.value(), provider)
            .map(AppUserExternalIdentityJpaEntity::getExternalSubject);
    }

    public KeycloakUserSub findKeycloakSub(UserId userId) {
        return externalIdentities
            .findFirstByAppUserIdAndProvider(userId.value(), IdentityProviderType.KEYCLOAK)
            .map(AppUserExternalIdentityJpaEntity::getExternalSubject)
            .map(KeycloakUserSub::parse)
            .orElse(null);
    }

    private AppUser toUser(AppUserJpaEntity entity) {
        return IdentityPersistenceMapper.toUser(
            entity, findKeycloakSub(UserId.of(entity.getId())));
    }

    private void persistKeycloakIdentity(AppUserJpaEntity appUser, KeycloakUserSub keycloakSub) {
        if (keycloakSub == null) {
            return;
        }
        var existing =
            externalIdentities.findFirstByAppUserIdAndProvider(
                appUser.getId(), IdentityProviderType.KEYCLOAK);
        var identity = existing.orElseGet(AppUserExternalIdentityJpaEntity::new);
        if (existing.isEmpty()) {
            identity.setAppUserId(appUser.getId());
            identity.setProvider(IdentityProviderType.KEYCLOAK);
            identity.setIssuer("legacy:keycloak");
            identity.setExternalSubject(keycloakSub.value().toString());
        }
        identity.setEmailSnapshot(appUser.getEmail());
        externalIdentities.save(identity);
    }
}
