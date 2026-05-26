package com.tchalanet.server.core.seller.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface SellerCommissionPolicyRepository extends JpaRepository<SellerCommissionPolicyJpaEntity, UUID> {

    @Query("SELECT p FROM SellerCommissionPolicyJpaEntity p WHERE p.sellerId = :sellerId AND p.status = 'ACTIVE' AND p.endsAt IS NULL ORDER BY p.startsAt DESC")
    Optional<SellerCommissionPolicyJpaEntity> findActivePolicy(@Param("sellerId") UUID sellerId);

    @Query("SELECT p FROM SellerCommissionPolicyJpaEntity p WHERE p.sellerId = :sellerId AND p.startsAt <= :at AND (p.endsAt IS NULL OR p.endsAt > :at) ORDER BY p.startsAt DESC")
    Optional<SellerCommissionPolicyJpaEntity> findPolicyAt(@Param("sellerId") UUID sellerId, @Param("at") Instant at);
}
