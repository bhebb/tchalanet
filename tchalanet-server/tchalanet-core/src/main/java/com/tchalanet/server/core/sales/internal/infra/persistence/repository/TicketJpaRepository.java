package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketJpaRepository extends TchJpaRepository<TicketJpaEntity, UUID> {
    @EntityGraph(attributePaths = "lines")
    Optional<TicketJpaEntity> findWithLinesById(UUID id);

    @EntityGraph(attributePaths = "lines")
    Optional<TicketJpaEntity> findWithLinesByTicketCode(String ticketCode);

    @EntityGraph(attributePaths = "lines")
    Optional<TicketJpaEntity> findWithLinesByPublicCode(String publicCode);

    @EntityGraph(attributePaths = "lines")
    Optional<TicketJpaEntity> findWithLinesByVerificationCode(String verificationCode);

    @EntityGraph(attributePaths = "lines")
    Optional<TicketJpaEntity> findWithLinesByOfflineSubmissionId(UUID offlineSubmissionId);

    boolean existsByOfflineSubmissionId(UUID offlineSubmissionId);

    @EntityGraph(attributePaths = "lines")
    List<TicketJpaEntity> findWithLinesByDrawId(UUID drawId);

    @EntityGraph(attributePaths = "charges")
    Optional<TicketJpaEntity> findWithChargesById(UUID id);

    @EntityGraph(attributePaths = "charges")
    List<TicketJpaEntity> findWithChargesByIdIn(List<UUID> ids);

    boolean existsByDrawIdAndSettlementStatusIn(UUID drawId, List<TicketSettlementStatus> statuses);

    long countByDrawIdAndSettlementStatusIn(UUID drawId, List<TicketSettlementStatus> statuses);

    /**
     * Returns just the @Version of an existing ticket.
     * Kept as a narrow persistence diagnostic API; updates must mutate managed entities
     * instead of transplanting versions onto rebuilt detached graphs.
     */
    @Query("select t.version from TicketJpaEntity t where t.id = :id")
    Optional<Long> findVersionById(@Param("id") UUID id);
}
