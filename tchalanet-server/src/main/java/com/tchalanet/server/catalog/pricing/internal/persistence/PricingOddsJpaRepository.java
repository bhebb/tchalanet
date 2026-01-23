package com.tchalanet.server.catalog.pricing.internal.persistence;

import com.tchalanet.server.common.types.enums.BetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PricingOddsJpaRepository extends JpaRepository<PricingOddsEntity, UUID> {

    Optional<PricingOddsEntity> findFirstByTenantIdAndGameCodeAndBetTypeAndBetOptionAndActiveIsTrueAndDeletedAtIsNull(
        UUID tenantId, String gameCode, BetType betType, Short betOption);
}

