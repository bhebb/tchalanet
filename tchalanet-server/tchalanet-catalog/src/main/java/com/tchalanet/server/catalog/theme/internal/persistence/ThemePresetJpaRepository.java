package com.tchalanet.server.catalog.theme.internal.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThemePresetJpaRepository extends JpaRepository<ThemePresetJpaEntity, UUID> {

  Optional<ThemePresetJpaEntity> findFirstByCodeIgnoreCaseAndDeletedAtIsNull(String code);
}
