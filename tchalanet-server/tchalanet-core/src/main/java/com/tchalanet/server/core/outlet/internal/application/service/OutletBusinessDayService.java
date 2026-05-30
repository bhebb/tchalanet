package com.tchalanet.server.core.outlet.internal.application.service;

import com.tchalanet.server.common.types.id.BusinessDayOverrideId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.api.query.OutletBusinessDayOverrideView;
import com.tchalanet.server.core.outlet.internal.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import com.tchalanet.server.core.outlet.internal.infra.persistence.OutletBusinessDayOverrideJpaEntity;
import com.tchalanet.server.core.outlet.internal.infra.persistence.OutletBusinessDayOverrideJpaRepository;
import com.tchalanet.server.core.outlet.internal.infra.persistence.OutletBusinessDayOverrideMapper;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OUTLET-LEVEL business-day overrides ({@code outlet_id NOT NULL}), owned by
 * core.outlet so the outlet can be validated (exists, belongs to the tenant via
 * RLS-scoped lookup, is ACTIVE) before any write — exactly why this surface does
 * not live in platform.tenantconfig.
 */
@Service
@RequiredArgsConstructor
public class OutletBusinessDayService {

  private final OutletReaderPort outletReader;
  private final OutletBusinessDayOverrideJpaRepository repo;
  private final OutletBusinessDayOverrideMapper mapper;

  @Transactional
  public OutletBusinessDayOverrideView upsert(
      TenantId tenantId, OutletId outletId, LocalDate businessDate,
      boolean open, String reasonCode, String label) {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(outletId, "outletId is required");
    Objects.requireNonNull(businessDate, "businessDate is required");
    requireActiveOutletOfTenant(tenantId, outletId);

    var existing = repo.findByTenantIdAndOutletIdAndBusinessDateAndDeletedAtIsNull(
        tenantId.value(), outletId.value(), businessDate);

    var e = existing.orElseGet(OutletBusinessDayOverrideJpaEntity::new);
    if (existing.isEmpty()) {
      e.setTenantId(tenantId.value());
      e.setOutletId(outletId.value());
      e.setBusinessDate(businessDate);
    }
    e.setOpen(open);
    e.setReasonCode(blankToNull(reasonCode));
    e.setLabel(blankToNull(label));
    return mapper.toView(repo.save(e));
  }

  @Transactional(readOnly = true)
  public List<OutletBusinessDayOverrideView> list(
      TenantId tenantId, OutletId outletId, LocalDate from, LocalDate to) {
    return mapper.toViews(
        repo.findByTenantIdAndOutletIdAndBusinessDateBetweenAndDeletedAtIsNullOrderByBusinessDateAsc(
            tenantId.value(), outletId.value(), from, to));
  }

  @Transactional
  public void softDelete(TenantId tenantId, OutletId outletId, BusinessDayOverrideId id) {
    var uuid = (id == null) ? null : id.value();
    var e = repo.findByIdAndDeletedAtIsNull(uuid)
        .orElseThrow(() -> new EntityNotFoundException("outlet_business_day_override_not_found"));
    if (!Objects.equals(e.getOutletId(), outletId.value())) {
      throw new EntityNotFoundException("outlet_business_day_override_not_found");
    }
    e.setDeletedAt(Instant.now());
    repo.save(e);
  }

  private void requireActiveOutletOfTenant(TenantId tenantId, OutletId outletId) {
    // RLS scopes findById to the current tenant; a foreign outlet is simply absent.
    var outlet = outletReader.getRequired(outletId);
    if (outlet.status() != OutletStatus.ACTIVE) {
      throw new IllegalStateException("Outlet is not active: " + outletId);
    }
  }

  private static String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s.trim();
  }
}
