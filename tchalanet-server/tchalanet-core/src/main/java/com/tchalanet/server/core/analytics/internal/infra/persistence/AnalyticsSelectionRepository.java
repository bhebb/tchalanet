package com.tchalanet.server.core.analytics.internal.infra.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AnalyticsSelectionRepository
    extends JpaRepository<AnalyticsSelectionEntity, UUID>, AnalyticsSelectionUpsertRepository {

  List<AnalyticsSelectionEntity> findByTenantIdAndRefDateBetweenOrderByRefDate(
      UUID tenantId, LocalDate from, LocalDate to);

  @Modifying
  @Query(
      """
      DELETE FROM AnalyticsSelectionEntity a
       WHERE a.tenantId = :tenantId
         AND a.refDate BETWEEN :from AND :to
      """)
  int deleteTenantRows(
      @Param("tenantId") UUID tenantId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
}

interface AnalyticsSelectionUpsertRepository {

  @Transactional
  void upsertAndIncrement(
      UUID tenantId,
      LocalDate refDate,
      UUID drawChannelId,
      String gameCode,
      String betType,
      Short betOption,
      String selectionKey,
      long ticketsDelta,
      long stakeDelta,
      long winningsDelta);
}

@Repository
class AnalyticsSelectionUpsertRepositoryImpl implements AnalyticsSelectionUpsertRepository {

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void upsertAndIncrement(
      UUID tenantId,
      LocalDate refDate,
      UUID drawChannelId,
      String gameCode,
      String betType,
      Short betOption,
      String selectionKey,
      long ticketsDelta,
      long stakeDelta,
      long winningsDelta) {

    em.createNativeQuery(
            "SELECT public.upsert_analytics_selection("
                + ":tid, :rd, :dcid, :gc, :bt, :bo, :sk, :td, :sd, :wd)")
        .setParameter("tid", tenantId)
        .setParameter("rd", refDate)
        .setParameter("dcid", drawChannelId)
        .setParameter("gc", gameCode)
        .setParameter("bt", betType)
        .setParameter("bo", betOption)
        .setParameter("sk", selectionKey)
        .setParameter("td", ticketsDelta)
        .setParameter("sd", stakeDelta)
        .setParameter("wd", winningsDelta)
        .getSingleResult();
  }
}
