package com.tchalanet.server.core.user.domain.ports;

import com.tchalanet.server.core.user.domain.model.AppUser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository {
  Optional<AppUser> findById(UUID id);

  AppUser save(AppUser user);

  void deleteById(UUID id);

  List<AppUser> findByTenantId(UUID tenantId);

  List<AppUser> findAll();

  void softDelete(UUID id, Instant deletedAt);
}
