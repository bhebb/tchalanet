package com.tchalanet.server.core.outlet.infra.persistence;

import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.outlet.application.port.out.OutletLookupPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletWriterPort;
import com.tchalanet.server.core.outlet.application.query.model.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.application.query.model.OutletSummaryView;
import com.tchalanet.server.core.outlet.domain.model.Outlet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutletPersistenceAdapter
    implements OutletReaderPort, OutletWriterPort, OutletLookupPort {

  private final OutletSpringRepository repo;

  @Override
  public Optional<Outlet> findById(OutletId id) {
    // RLS enforces tenant scoping at DB level
    return repo.findById(id.value()).map(this::toDomain);
  }

  @Override
  public List<Outlet> listByTenant() {
    return repo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
  }

  @Override
  public void save(Outlet outlet) {
    OutletEntity entity = repo.findById(outlet.id().value()).orElseGet(OutletEntity::new);
    applyToEntity(outlet, entity);
    repo.save(entity);
  }

  @Override
  public boolean isSalesBlocked(OutletId outletId) {
    return repo.findById(outletId.value()).map(OutletEntity::isSalesBlocked).orElse(false);
  }

  @Override
  public TchPage<OutletSummaryView> search(OutletSearchCriteria c, TchPageRequest pageReq) {
    // RLS-scoped fetch then in-memory filter + paginate. Switch to Specifications when volume
    // grows past a few hundred outlets per tenant.
    List<OutletEntity> all = repo.findAll();
    String qNorm = c.q() == null ? null : c.q().trim().toLowerCase();
    List<OutletSummaryView> filtered =
        all.stream()
            .filter(e -> e.getDeletedAt() == null)
            .filter(
                e ->
                    qNorm == null
                        || (e.getName() != null && e.getName().toLowerCase().contains(qNorm))
                        || (e.getSlug() != null && e.getSlug().toLowerCase().contains(qNorm)))
            .filter(
                e ->
                    c.active() == null
                        || (c.active() == (!e.isSalesBlocked() && !e.isDayClosed())))
            .filter(e -> c.salesBlocked() == null || c.salesBlocked() == e.isSalesBlocked())
            .filter(e -> c.dayClosed() == null || c.dayClosed() == e.isDayClosed())
            .map(this::toSummaryView)
            .collect(Collectors.toList());

    Pageable pageable = pageReq.pageable();
    int total = filtered.size();
    int size = pageable.getPageSize();
    int pageNumber = pageable.getPageNumber();
    int from = (int) Math.min((long) pageNumber * size, total);
    int to = Math.min(from + size, total);
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
    boolean last = pageNumber + 1 >= totalPages;
    boolean hasNext = !last;
    boolean hasPrevious = pageNumber > 0;
    return TchPage.of(
        filtered.subList(from, to), pageNumber, size, total, totalPages, last, hasNext, hasPrevious);
  }

  private OutletSummaryView toSummaryView(OutletEntity e) {
    return new OutletSummaryView(
        OutletId.of(e.getId()),
        e.getTenantId() == null ? null : TenantId.of(e.getTenantId()),
        e.getName(),
        e.getSlug(),
        e.isDayClosed(),
        e.isSalesBlocked(),
        e.getSalesBlockReason(),
        e.getSalesBlockedAt(),
        e.getTimezone(),
        e.isAutoOpenSession(),
        e.isAutoCloseSession());
  }

  private Outlet toDomain(OutletEntity e) {
    return new Outlet(
        OutletId.of(e.getId()),
        e.getTenantId() == null ? null : TenantId.of(e.getTenantId()),
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
        e.isAutoOpenSession(),
        e.isAutoCloseSession(),
        UserId.nullableOf(e.getAutoSessionUserId()),
        TerminalId.nullableOf(e.getAutoSessionTerminalId()),
        e.getDefaultOpeningFloatCents(),
        AddressId.nullableOf(e.getAddressId()));
  }

  private void applyToEntity(Outlet o, OutletEntity e) {
    e.setId(o.id().value());
    e.setTenantId(o.tenantId() == null ? null : o.tenantId().value());
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
    e.setAutoOpenSession(o.autoOpenSession());
    e.setAutoCloseSession(o.autoCloseSession());
    e.setAutoSessionUserId(o.autoSessionUserId() == null ? null : o.autoSessionUserId().value());
    e.setAutoSessionTerminalId(
        o.autoSessionTerminalId() == null ? null : o.autoSessionTerminalId().value());
    e.setDefaultOpeningFloatCents(o.defaultOpeningFloatCents());
    e.setAddressId(o.addressId() == null ? null : o.addressId().value());
  }
}
