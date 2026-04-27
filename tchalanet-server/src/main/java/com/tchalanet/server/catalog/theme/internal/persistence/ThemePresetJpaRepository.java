package com.tchalanet.server.catalog.theme.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThemePresetJpaRepository extends JpaRepository<ThemePresetJpaEntity, UUID> {

    Optional<ThemePresetJpaEntity> findFirstByCodeIgnoreCaseAndDeletedAtIsNull(String code);

}
