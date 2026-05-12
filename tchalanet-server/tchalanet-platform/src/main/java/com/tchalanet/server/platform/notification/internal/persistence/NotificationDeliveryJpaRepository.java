package com.tchalanet.server.platform.notification.internal.persistence;

import com.tchalanet.server.core.notification.domain.model.NotificationDeliveryStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDeliveryJpaRepository
    extends JpaRepository<NotificationDeliveryJpaEntity, UUID> {

  @Query(
      """
      select d from NotificationDeliveryJpaEntity d
       where d.deletedAt is null
         and (:notificationId is null or d.notificationId = :notificationId)
         and (:status is null or d.status = :status)
       order by d.createdAt desc
      """)
  Page<NotificationDeliveryJpaEntity> search(
      @Param("notificationId") UUID notificationId,
      @Param("status") NotificationDeliveryStatus status,
      Pageable pageable);
}
