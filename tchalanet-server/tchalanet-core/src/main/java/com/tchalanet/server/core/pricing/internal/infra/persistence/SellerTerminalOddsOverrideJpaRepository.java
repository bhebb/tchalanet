package com.tchalanet.server.core.pricing.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerTerminalOddsOverrideJpaRepository
    extends JpaRepository<SellerTerminalOddsOverrideJpaEntity, UUID> {

    @Query("""
        SELECT e FROM SellerTerminalOddsOverrideJpaEntity e
        WHERE e.tenantId = :tenantId
          AND e.sellerTerminalId = :sellerTerminalId
          AND e.active = true
          AND e.deletedAt IS NULL
        ORDER BY e.gameCode, e.betType, e.betOption
        """)
    List<SellerTerminalOddsOverrideJpaEntity> findActiveBySellerTerminal(
        @Param("tenantId") UUID tenantId,
        @Param("sellerTerminalId") UUID sellerTerminalId
    );

    @Query("""
        SELECT e FROM SellerTerminalOddsOverrideJpaEntity e
        WHERE e.tenantId = :tenantId
          AND e.sellerTerminalId = :sellerTerminalId
          AND e.gameCode = :gameCode
          AND e.betType = :betType
          AND ((:betOption IS NULL AND e.betOption IS NULL) OR e.betOption = :betOption)
          AND e.active = true
          AND e.deletedAt IS NULL
        """)
    Optional<SellerTerminalOddsOverrideJpaEntity> findActiveByNaturalKey(
        @Param("tenantId") UUID tenantId,
        @Param("sellerTerminalId") UUID sellerTerminalId,
        @Param("gameCode") String gameCode,
        @Param("betType") String betType,
        @Param("betOption") Short betOption
    );
}
