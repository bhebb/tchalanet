package com.tchalanet.server.platform.communication.internal.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageDeliveryAttemptJpaRepository
    extends JpaRepository<MessageDeliveryAttemptJpaEntity, UUID> {

  @Query(
      """
      select a from MessageDeliveryAttemptJpaEntity a
       where a.deletedAt is null
         and a.messageId in :messageIds
       order by a.attemptedAt desc
      """)
  List<MessageDeliveryAttemptJpaEntity> findRecentForMessages(
      @Param("messageIds") Collection<UUID> messageIds,
      Pageable pageable);
}
