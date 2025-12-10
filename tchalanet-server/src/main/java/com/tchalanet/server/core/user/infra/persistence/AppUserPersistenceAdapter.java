package com.tchalanet.server.core.user.infra.persistence;

import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserStatus;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppUserPersistenceAdapter implements UserReaderPort, UserWriterPort {

  private final JpaAppUserRepository jpa;

  private AppUser toDomain(AppUserJpaEntity e) {
    if (e == null) return null;
    UUID id = e.getId();
    UUID keycloakId = e.getKeycloakId();
    UUID tenantId = e.getTenantId();
    String username = e.getUsername();
    String email = e.getEmail();
    String phone = e.getPhone();
    String firstName = e.getFirstName();
    String lastName = e.getLastName();
    String displayName = e.getDisplayName();
    String avatarUrl = e.getAvatarUrl();
    UserStatus status = UserStatus.ACTIVE;
    try {
      if (e.getStatus() != null) status = UserStatus.valueOf(e.getStatus());
    } catch (IllegalArgumentException ignored) {
      status = UserStatus.ACTIVE;
    }
    String locale = e.getLocale();
    String timeZone = e.getTimeZone();
    Instant lastLoginAt = e.getLastLoginAt();
    return new AppUser(id, keycloakId, tenantId, username, email, phone, firstName, lastName, displayName, avatarUrl, status, locale, timeZone, lastLoginAt);
  }

  @Override
  public Optional<AppUser> findById(UUID id) {
    return jpa.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<AppUser> findByKeycloakId(UUID keycloakId) {
    return jpa.findByKeycloakId(keycloakId).map(this::toDomain);
  }

  @Override
  public Optional<AppUser> findByEmail(String email) {
    return jpa.findByEmail(email).map(this::toDomain);
  }

  @Override
  public Optional<AppUser> findByEmailOrPhone(String email, String phone) {
    return jpa.findByEmailOrPhone(email, phone).map(this::toDomain);
  }

  @Override
  public List<AppUser> findAll() {
    return jpa.findAll().stream().map(this::toDomain).collect(Collectors.toList());
  }

  @Override
  public List<AppUser> findByTenantId(UUID tenantId) {
    return jpa.findAll().stream()
        .filter(e -> tenantId.equals(e.getTenantId()))
        .map(this::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<AppUser> findAllActiveUsers() {
    return jpa.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE.name()).stream().map(this::toDomain).collect(Collectors.toList());
  }

  @Override
  public List<AppUser> findAllActiveUsersByTenant(UUID tenantId) {
    return jpa.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, UserStatus.ACTIVE.name()).stream().map(this::toDomain).collect(Collectors.toList());
  }

  @Override
  public AppUser save(AppUser user) {
    AppUserJpaEntity e = new AppUserJpaEntity();
    if (user.id() != null) e.setId(user.id());
    e.setKeycloakId(user.keycloakId());
    e.setTenantId(user.tenantId());
    e.setUsername(user.username());
    e.setEmail(user.email());
    e.setPhone(user.phone());
    e.setFirstName(user.firstName());
    e.setLastName(user.lastName());
    e.setDisplayName(user.displayName());
    e.setAvatarUrl(user.avatarUrl());
    e.setLocale(user.locale());
    e.setTimeZone(user.timeZone());
    e.setStatus(user.status() != null ? user.status().name() : null);
    e.setLastLoginAt(user.lastLoginAt());
    AppUserJpaEntity saved = jpa.save(e);
    return toDomain(saved);
  }

  @Override
  public void softDelete(java.util.UUID userId, Instant when) {
    jpa.findById(userId).ifPresent(e -> {
      e.setDeletedAt(when);
      jpa.save(e);
    });
  }

  // --- Paged implementations ---
  @Override
  public Page<AppUser> findAll(Pageable pageable) {
    var page = jpa.findAll(pageable);
    return new PageImpl<>(page.getContent().stream().map(this::toDomain).collect(Collectors.toList()), pageable, page.getTotalElements());
  }

  @Override
  public Page<AppUser> findByTenantId(UUID tenantId, Pageable pageable) {
    var page = jpa.findByTenantId(tenantId, pageable);
    return new PageImpl<>(page.getContent().stream().map(this::toDomain).collect(Collectors.toList()), pageable, page.getTotalElements());
  }

  @Override
  public Page<AppUser> findAllActiveUsers(Pageable pageable) {
    var page = jpa.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE.name(), pageable);
    return new PageImpl<>(page.getContent().stream().map(this::toDomain).collect(Collectors.toList()), pageable, page.getTotalElements());
  }

  @Override
  public Page<AppUser> findAllActiveUsersByTenant(UUID tenantId, Pageable pageable) {
    var page = jpa.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, UserStatus.ACTIVE.name(), pageable);
    return new PageImpl<>(page.getContent().stream().map(this::toDomain).collect(Collectors.toList()), pageable, page.getTotalElements());
  }

  @Override
  public void updateStatus(UUID uuid, UserStatus userStatus) {
    jpa.findById(uuid).ifPresent(e -> {
      e.setStatus(userStatus != null ? userStatus.name() : null);
      jpa.save(e);
    });
  }
}
