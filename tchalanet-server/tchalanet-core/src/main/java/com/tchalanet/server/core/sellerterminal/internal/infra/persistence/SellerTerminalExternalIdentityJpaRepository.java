package com.tchalanet.server.core.sellerterminal.internal.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerTerminalExternalIdentityJpaRepository
    extends JpaRepository<SellerTerminalExternalIdentityJpaEntity, UUID> {

  Optional<SellerTerminalExternalIdentityJpaEntity> findBySellerTerminalId(UUID sellerTerminalId);

  boolean existsBySellerTerminalId(UUID sellerTerminalId);
}
