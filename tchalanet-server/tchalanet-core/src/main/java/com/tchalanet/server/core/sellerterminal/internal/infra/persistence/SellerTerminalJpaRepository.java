package com.tchalanet.server.core.sellerterminal.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface SellerTerminalJpaRepository
    extends JpaRepository<SellerTerminalJpaEntity, UUID>,
            JpaSpecificationExecutor<SellerTerminalJpaEntity> {

    Optional<SellerTerminalJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("""
        SELECT
            COUNT(e),
            SUM(CASE WHEN e.commissionRate = :defaultRate THEN 1L ELSE 0L END),
            MIN(e.commissionRate),
            MAX(e.commissionRate),
            AVG(e.commissionRate)
        FROM SellerTerminalJpaEntity e
        WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL
        """)
    Object[] commissionStats(
        @Param("tenantId") UUID tenantId,
        @Param("defaultRate") BigDecimal defaultRate
    );

    @Query("""
        SELECT COUNT(e), MIN(e.commissionRate), MAX(e.commissionRate), AVG(e.commissionRate)
        FROM SellerTerminalJpaEntity e
        WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL
        """)
    Object[] commissionStatsNoDefault(@Param("tenantId") UUID tenantId);

    @Query("""
        SELECT t FROM SellerTerminalJpaEntity t
        JOIN SellerTerminalExternalIdentityJpaEntity e ON e.sellerTerminalId = t.id
        WHERE e.provider = :provider AND e.issuer = :issuer AND e.externalSubject = :subject
          AND t.deletedAt IS NULL AND e.deletedAt IS NULL
        """)
    Optional<SellerTerminalJpaEntity> findByExternalSubject(
        @Param("provider") String provider,
        @Param("issuer") String issuer,
        @Param("subject") String externalSubject);
}
