package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AppUserPersistenceAdapter implements UserReaderPort, UserWriterPort {

    private final JpaAppUserRepository jpa;

    @Override
    public Optional<AppUser> findById(UUID id) {
        return jpa.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<AppUser> findByKeycloakId(UUID keycloakId) {
        return jpa.findByKeycloakId(keycloakId).map(UserMapper::toDomain);
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
            entity = jpa.findById(user.getId()).orElseGet(AppUserJpaEntity::new);
            entity.setId(user.getId());
            UserMapper.merge(entity, user);
        } else {
            entity = UserMapper.toNewEntity(user);
        }
        AppUserJpaEntity saved = jpa.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    public void softDelete(UUID userId, Instant when) {
        jpa.findById(userId).ifPresent(e -> {
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
    public Page<AppUser> findByTenantId(UUID tenantId, Pageable pageable) {
        var page = jpa.findByTenantId(tenantId, pageable);
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
    public Page<AppUser> findAllActiveUsersByTenant(UUID tenantId, Pageable pageable) {
        var page = jpa.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, UserStatus.ACTIVE.name(), pageable);
        var content = page.getContent().stream().map(UserMapper::toDomain).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    public void updateStatus(UUID uuid, UserStatus userStatus) {
        jpa.findById(uuid).ifPresent(e -> {
            e.setStatus(userStatus);
            jpa.save(e);
        });
    }
}
