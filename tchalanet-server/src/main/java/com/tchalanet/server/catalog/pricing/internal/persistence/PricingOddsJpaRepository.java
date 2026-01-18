package com.tchalanet.server.catalog.pricing.internal.persistence;

import com.tchalanet.server.common.types.enums.BetType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "pricing-odds", collectionResourceRel = "pricing-odds")
public interface PricingOddsJpaRepository extends JpaRepository<PricingOddsEntity, UUID> {

  Optional<PricingOddsEntity> findFirstByTenantIdAndGameCodeAndBetTypeAndBetOptionAndActiveIsTrueAndDeletedAtIsNull(
      UUID tenantId, String gameCode, BetType betType, Short betOption);
}

