package com.tchalanet.server.core.theme.infra.persistence;

import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaThemeRepository
    extends JpaRepository<ThemeJpaEntity, UUID>, QuerydslPredicateExecutor<ThemeJpaEntity> {

  Optional<ThemeJpaEntity> findById(UUID id);

  List<ThemeJpaEntity> findByTenantId(UUID tenantId);

  Page<ThemeJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

  List<ThemeJpaEntity> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, ThemeStatus status);

  Page<ThemeJpaEntity> findByStatusAndDeletedAtIsNull(ThemeStatus status, Pageable pageable);
}
