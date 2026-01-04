package com.tchalanet.server.core.outlet.infra.persistence;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.application.port.out.OutletLookupPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutletPersistenceAdapter
    implements OutletReaderPort, OutletWriterPort, OutletLookupPort {

  private final OutletSpringRepository repo;

  @Override
  public Optional<Outlet> findById(OutletId id, TenantId tenantId) {
    return repo.findById(id.uuid())
        .filter(e -> tenantId.uuid().equals(e.getTenantId()))
        .map(this::toDomain);
  }

  @Override
  public void save(Outlet outlet) {
    // Convert domain to entity and save (creates or updates)
    OutletEntity entity = toEntity(outlet);
    repo.save(entity);
  }

  @Override
  public boolean isSalesBlocked(TenantId tenantId, OutletId outletId) {
    return repo.findById(outletId.uuid())
        .filter(e -> tenantId.uuid().equals(e.getTenantId()))
        .map(OutletEntity::isSalesBlocked)
        .orElse(false);
  }

  private Outlet toDomain(OutletEntity e) {
    OutletId oid = OutletId.of(e.getId());
    TenantId tid = e.getTenantId() == null ? null : TenantId.of(e.getTenantId());
    return new Outlet(
        oid,
        tid,
        e.getName(),
        e.getSlug(),
        e.isDayClosed(),
        e.isSalesBlocked(),
        e.getSalesBlockReason(),
        e.getSalesBlockedAt(),
        e.getTimezone(),
        e.getBusinessDayCutoff(),
        e.isReceiptPrintingEnabled(),
        e.getReceiptHeaderMessage(),
        e.getReceiptFooterMessage(),
        e.isRequireOpeningFloat(),
        e.getAddressId());
  }

  private OutletEntity toEntity(Outlet o) {
    OutletEntity e = new OutletEntity();
    e.setId(o.id().uuid());
    e.setTenantId(o.tenantId() == null ? null : o.tenantId().uuid());
    e.setName(o.name());
    e.setSlug(o.slug());
    e.setDayClosed(o.dayClosed());
    e.setSalesBlocked(o.salesBlocked());
    e.setSalesBlockReason(o.salesBlockReason());
    e.setSalesBlockedAt(o.salesBlockedAt());
    e.setTimezone(o.timezone());
    e.setBusinessDayCutoff(o.businessDayCutoff());
    e.setReceiptPrintingEnabled(o.receiptPrintingEnabled());
    e.setReceiptHeaderMessage(o.receiptHeaderMessage());
    e.setReceiptFooterMessage(o.receiptFooterMessage());
    e.setRequireOpeningFloat(o.requireOpeningFloat());
    e.setAddressId(o.addressId());
    return e;
  }
}
