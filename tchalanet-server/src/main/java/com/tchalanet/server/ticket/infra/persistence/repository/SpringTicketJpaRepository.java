package com.tchalanet.server.ticket.infra.persistence.repository;

import com.tchalanet.server.ticket.infra.persistence.entity.TicketEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringTicketJpaRepository
    extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {
  Optional<TicketEntity> findByPublicCode(String publicCode);

  @Modifying
  @Query(
      "UPDATE TicketEntity t SET t.deletedAt = :now WHERE t.tenantId = :tenantId AND t.createdAt < :cutoffDate AND t.deletedAt IS NULL")
  int archiveOldTickets(
      @Param("tenantId") UUID tenantId,
      @Param("cutoffDate") Instant cutoffDate,
      @Param("now") Instant now);

  List<TicketEntity> findByDrawId(UUID drawId); // New method for Stats domain
}
