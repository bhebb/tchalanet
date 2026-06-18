package com.tchalanet.server.core.sellerterminal.internal.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerTerminalExternalIdentityJpaRepository
    extends JpaRepository<SellerTerminalExternalIdentityJpaEntity, UUID> {

    Optional<SellerTerminalExternalIdentityJpaEntity> findBySellerTerminalId(UUID sellerTerminalId);

    boolean existsBySellerTerminalId(UUID sellerTerminalId);
}
