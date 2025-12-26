package com.tchalanet.server.core.draw.infra.persistence;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.application.port.out.TenantDrawCalendarQueryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TenantDrawCalendarJpaAdapter implements TenantDrawCalendarQueryPort {

  @PersistenceContext private EntityManager em;

  @Override
  public List<TenantId> listActiveTenantIdsForDrawCalendar() {
    String sql =
        """
                select distinct dc.tenant_id
                from draw_channel dc
                join tenant t on t.id = dc.tenant_id
                where dc.active = true
                  and dc.deleted_at is null
                  and t.status = 'ACTIVE'
                  and t.deleted_at is null
                order by dc.tenant_id
                """;
    return em.createNativeQuery(sql, UUID.class).getResultList().stream()
        .map(t -> TenantId.nullableOf((UUID) t))
        .toList();
  }
}
