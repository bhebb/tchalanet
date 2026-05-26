package com.tchalanet.server.core.seller.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SellerRepository extends JpaRepository<SellerJpaEntity, UUID> {
    Optional<SellerJpaEntity> findByUserId(UUID userId);
    List<SellerJpaEntity> findAllByOrderByCreatedAtDesc();
}
