package com.tchalanet.server.core.user.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.user.domain.model.AppUser;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserReaderPort {
  Optional<AppUser> findById(UserId id);

  Optional<AppUser> findByKeycloakId(UUID keycloakId);

  Optional<AppUser> findByEmail(String email);

  Optional<AppUser> findByEmailOrPhone(String email, String phone);

  // Paged versions
  Page<AppUser> findAll(Pageable pageable);

  Page<AppUser> findByTenantId(TenantId tenantId, Pageable pageable);

  Page<@NotNull AppUser> findAllActiveUsers(Pageable pageable);

  Page<AppUser> findAllActiveUsersByTenant(TenantId tenantId, Pageable pageable);
}
