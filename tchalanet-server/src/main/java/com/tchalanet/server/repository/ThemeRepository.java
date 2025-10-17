package com.tchalanet.server.repository;

import com.tchalanet.server.constants.ThemeStatus;
import com.tchalanet.server.model.Theme;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, UUID> {
  Optional<Theme> findFirstByTenantIdAndStatusOrderByUpdatedAtDesc(
      UUID tenantId, ThemeStatus status);

  List<Theme> findByTenantId(UUID tenantId);

  List<Theme> findByTenantIdIsNull();
}
