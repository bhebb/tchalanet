package com.tchalanet.server.core.terminal.internal.infra.persistence.sellerterminal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SellerTerminalJpaRepository
    extends JpaRepository<SellerTerminalJpaEntity, UUID>,
            JpaSpecificationExecutor<SellerTerminalJpaEntity> {

    Optional<SellerTerminalJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

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
