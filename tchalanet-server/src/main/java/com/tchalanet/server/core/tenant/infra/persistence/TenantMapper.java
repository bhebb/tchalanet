package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenant.domain.model.Tenant;

final class TenantMapper {

  private TenantMapper() {}

  static Tenant toDomain(TenantJpaEntity e) {
    return Tenant.restore(
        TenantId.of(e.getId()),
        e.getCode(),
        e.getName(),
        e.getType(),
        e.getTimezone(),
        e.getCurrency(),
        e.getStatus(),
        e.getActiveThemeId(),
        e.getAddressId(),
        e.getVersion());
  }

  static TenantJpaEntity toNewEntity(Tenant t) {
    TenantJpaEntity e = new TenantJpaEntity();
    e.setId(t.id().value());
    apply(e, t);
    return e;
  }

  static TenantJpaEntity merge(TenantJpaEntity e, Tenant t) {
    apply(e, t);
    return e;
  }

  private static void apply(TenantJpaEntity e, Tenant t) {
    e.setCode(t.code());
    e.setName(t.name());
    e.setType(t.type());
    e.setTimezone(t.timezone());
    e.setCurrency(t.currency());
    e.setStatus(t.status());
    e.setActiveThemeId(t.activeThemeId());
    e.setAddressId(t.addressId());
    e.setVersion(t.version());
  }
}
