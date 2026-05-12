package com.tchalanet.server.core.drawresult.internal.infra.persistence.repo;

import com.tchalanet.server.core.drawresult.infra.persistence.DrawResultJpaEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DrawResultJpaRepository extends JpaRepository<DrawResultJpaEntity, UUID> {

  /**
   * Marque un DrawResult comme OVERRIDDEN suite à une correction de résultat.
   *
   * @param id l'ID du DrawResult à marquer
   * @param reason la raison de l'override
   * @param overriddenAt timestamp de l'override
   * @return nombre de lignes mises à jour (0 ou 1)
   */
  @Modifying
  @Query("""
      UPDATE DrawResultJpaEntity dr
      SET dr.status = 'OVERRIDDEN',
          dr.overrideReason = :reason,
          dr.updatedAt = :overriddenAt
      WHERE dr.id = :id
        AND dr.deletedAt IS NULL
        AND dr.status != 'OVERRIDDEN'
      """)
  int markAsOverridden(
      @Param("id") UUID id,
      @Param("reason") String reason,
      @Param("overriddenAt") Instant overriddenAt
  );
}
