package com.tchalanet.server.features.notifications.shared;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByTenantIdAndUserIdOrderByCreatedAtDesc(
        UUID tenantId,
        UUID userId,
        Pageable pageable
    );

    Page<NotificationEntity> findByTenantIdAndUserIdAndReadIsFalseOrderByCreatedAtDesc(
        UUID tenantId,
        UUID userId,
        Pageable pageable
    );
    long countByTenantIdAndUserIdAndReadIsFalse(UUID tenantId, UUID userId);

    @Modifying
    @Query("""
        update NotificationEntity n
           set n.read = true,
               n.readAt = :now
         where n.tenant_id = :tenantId
           and n.userId = :userId
           and n.read = false
        """)
    int markAllRead(@Param("tenantId") UUID tenantId,
                    @Param("userId") UUID userId,
                    @Param("now") Instant now);

    Optional<NotificationEntity> findByIdAndTenantIdAndUserId(
        UUID id,
        UUID tenantId,
        UUID userId
    );}

