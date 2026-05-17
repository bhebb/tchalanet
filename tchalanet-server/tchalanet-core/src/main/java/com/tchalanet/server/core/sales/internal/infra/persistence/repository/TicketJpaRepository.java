package com.tchalanet.server.core.sales.internal.infra.persistence.repository;

import com.tchalanet.server.common.persistence.repository.TchJpaRepository;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;

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

    @EntityGraph(attributePaths = {"lines", "charges"})
    Optional<TicketJpaEntity> findWithLinesAndChargesById(UUID value);

    @EntityGraph(attributePaths = {"lines", "charges"})
    List<TicketJpaEntity> findWithLinesAndChargesByDrawId(UUID drawId);

    boolean existsByDrawIdAndSettlementStatusIn(UUID drawId, List<TicketSettlementStatus> statuses);

    long countByDrawIdAndSettlementStatusIn(UUID drawId, List<TicketSettlementStatus> statuses);
}
