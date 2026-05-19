package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfflineSubmissionLineJpaRepository
    extends JpaRepository<OfflineSubmissionLineJpaEntity, UUID> {

    List<OfflineSubmissionLineJpaEntity> findAllBySubmissionIdOrderByLineNoAsc(UUID submissionId);
}
