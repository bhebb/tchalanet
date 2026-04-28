package com.tchalanet.server.core.user.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.core.user.domain.model.AppUser;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserReaderPort {
  Optional<AppUser> findById(UserId id);

  Optional<AppUser> findByKeycloakSub(KeycloakUserSub keycloakSub);

  Optional<AppUser> findByEmail(String email);

  Optional<AppUser> findByEmailOrPhone(String email, String phone);

  // Paged versions
  Page<AppUser> findAll(Pageable pageable);

  // Returns only users who are active members of the given tenant (via tenant_user join)
  Page<AppUser> findByTenantId(TenantId tenantId, Pageable pageable);

  Page<@NotNull AppUser> findAllActiveUsers(Pageable pageable);

  // Search by criteria
  Page<AppUser> searchByCriteria(String nameLike, String status, Instant createdAfter, Instant createdBefore, Pageable pageable);
}
