package com.tchalanet.server.core.user.application.port.out;

import com.tchalanet.server.core.user.domain.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserReaderPort {
    Optional<AppUser> findById(UUID id);
    Optional<AppUser> findByKeycloakId(UUID keycloakId);
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByEmailOrPhone(String email, String phone);
        List<AppUser> findAll();
    List<AppUser> findByTenantId(UUID tenantId);
    List<AppUser> findAllActiveUsers();
    List<AppUser> findAllActiveUsersByTenant(UUID tenantId);

    // Paged versions
    Page<AppUser> findAll(Pageable pageable);
    Page<AppUser> findByTenantId(UUID tenantId, Pageable pageable);
    Page<AppUser> findAllActiveUsers(Pageable pageable);
    Page<AppUser> findAllActiveUsersByTenant(UUID tenantId, Pageable pageable);

}

