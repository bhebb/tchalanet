package com.tchalanet.server.core.drawresult.internal.infra.persistence.repo;

import com.tchalanet.server.core.drawresult.internal.infra.persistence.DrawResultJpaEntity;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface DrawResultJpaRepository extends JpaRepository<DrawResultJpaEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<DrawResultJpaEntity> findByResultSlotIdAndResultDateAndDeletedAtIsNull(
      UUID resultSlotId,
      LocalDate resultDate);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<DrawResultJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
}
