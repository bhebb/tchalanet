package com.tchalanet.server.platform.notification.internal.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationTemplateJpaRepository
    extends JpaRepository<NotificationTemplateJpaEntity, UUID> {

  @Query(
      """
      select t from NotificationTemplateJpaEntity t
       where t.deletedAt is null
         and t.active = true
         and t.templateKey = :templateKey
         and t.locale = :locale
         and (t.tenantId = :tenantId or t.tenantId is null)
       order by case when t.tenantId = :tenantId then 0 else 1 end
       limit 1
      """)
  Optional<NotificationTemplateJpaEntity> findBest(
      @Param("tenantId") UUID tenantId,
      @Param("templateKey") String templateKey,
      @Param("locale") String locale);
}
