package com.tchalanet.server.user.infra.persistence;

import com.tchalanet.server.user.domain.model.AppUser;
import com.tchalanet.server.user.domain.ports.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaAppUserRepositoryAdapter implements AppUserRepository {

  private final JpaAppUserRepository jpa;

  @Override
  public Optional<AppUser> findById(UUID id) {
    return jpa.findById(id).map(e -> toDomain(e));
  }

  @Override
  public AppUser save(AppUser user) {
    AppUserJpaEntity e = toEntity(user);
    AppUserJpaEntity saved = jpa.save(e);
    return toDomain(saved);
  }

  @Override
  public void deleteById(UUID id) {
    jpa.deleteById(id);
  }

  @Override
  public List<AppUser> findByTenantId(UUID tenantId) {
    return jpa.findAll().stream()
        .filter(e -> tenantId.equals(e.getTenantId()))
        .map(this::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<AppUser> findAll() {
    return jpa.findAll().stream().map(this::toDomain).collect(Collectors.toList());
  }

  @Override
  public void softDelete(UUID id, Instant deletedAt) {
    jpa.findById(id)
        .ifPresent(
            e -> {
              e.setDeletedAt(deletedAt);
              jpa.save(e);
            });
  }

  private AppUser toDomain(AppUserJpaEntity e) {
    return new AppUser(
        e.getId(),
        e.getTenantId(),
        e.getUsername(),
        e.getEmail(),
        e.getDisplayName(),
        e.getLocale(),
        e.getLastLoginAt());
  }

  private AppUserJpaEntity toEntity(AppUser u) {
    AppUserJpaEntity e = new AppUserJpaEntity();
    e.setId(u.id());
    e.setTenantId(u.tenantId());
    e.setUsername(u.username());
    e.setEmail(u.email());
    e.setDisplayName(u.displayName());
    e.setLocale(u.locale());
    e.setLastLoginAt(u.lastLoginAt());
    return e;
  }
}
