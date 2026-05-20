package com.tchalanet.server.catalog.pricing.internal.persistence;

import com.tchalanet.server.catalog.game.api.model.BetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PricingOddsJpaRepository extends JpaRepository<PricingOddsEntity, UUID> {

    Optional<PricingOddsEntity> findFirstByTenantIdAndGameCodeAndBetTypeAndBetOptionAndActiveIsTrue(
        UUID tenantId, String gameCode, BetType betType, Short betOption);

    // Tenant-agnostic (read-side) - rely on RLS to scope by current tenant
    Optional<PricingOddsEntity> findFirstByGameCodeAndBetTypeAndBetOptionAndActiveIsTrue(
        String gameCode, BetType betType, Short betOption);
}
