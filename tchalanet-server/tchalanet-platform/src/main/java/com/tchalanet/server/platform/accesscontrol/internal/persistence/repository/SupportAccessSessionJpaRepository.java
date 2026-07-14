package com.tchalanet.server.platform.accesscontrol.internal.persistence.repository;

import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.SupportAccessSessionJpaEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportAccessSessionJpaRepository
    extends JpaRepository<SupportAccessSessionJpaEntity, UUID> {

  Optional<SupportAccessSessionJpaEntity>
      findFirstBySuperAdminUserIdAndClearedAtIsNullAndExpiresAtAfterOrderByGrantedAtDesc(
          UUID superAdminUserId, Instant now);

  Optional<SupportAccessSessionJpaEntity>
      findFirstBySuperAdminUserIdAndClearedAtIsNullOrderByGrantedAtDesc(UUID superAdminUserId);

  @Modifying
  @Query(
      """
      update SupportAccessSessionJpaEntity s
      set s.clearedAt = :clearedAt
      where s.superAdminUserId = :superAdminUserId
        and s.clearedAt is null
      """)
  int clearCurrent(
      @Param("superAdminUserId") UUID superAdminUserId, @Param("clearedAt") Instant clearedAt);
}
