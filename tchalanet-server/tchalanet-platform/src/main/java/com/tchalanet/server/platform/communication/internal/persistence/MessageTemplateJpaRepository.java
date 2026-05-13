package com.tchalanet.server.platform.communication.internal.persistence;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageTemplateJpaRepository extends JpaRepository<MessageTemplateJpaEntity, UUID> {

  @Query(
      """
      select t from MessageTemplateJpaEntity t
       where t.deletedAt is null
         and t.active = true
         and t.templateKey = :templateKey
         and t.channel = :channel
         and t.locale = :locale
         and (t.tenantId = :tenantId or t.tenantId is null)
       order by case when t.tenantId = :tenantId then 0 else 1 end
       limit 1
      """)
  Optional<MessageTemplateJpaEntity> findBest(
      @Param("tenantId") UUID tenantId,
      @Param("templateKey") String templateKey,
      @Param("channel") CommunicationChannel channel,
      @Param("locale") String locale);
}
