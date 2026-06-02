package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.common.types.id.BusinessDayOverrideId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.model.BusinessDayOverrideView;
import com.tchalanet.server.platform.tenant.internal.mapper.BusinessDayOverrideMapper;
import com.tchalanet.server.platform.tenant.internal.persistence.BusinessDayOverrideJpaEntity;
import com.tchalanet.server.platform.tenant.internal.persistence.BusinessDayOverrideJpaRepository;
import com.tchalanet.server.platform.tenant.internal.web.model.UpsertBusinessDayOverrideRequest;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TENANT-LEVEL writes for {@code business_day_override} ({@code outlet_id IS NULL}).
 *
 * <p>Outlet-level overrides are owned by {@code core.outlet} (it validates the
 * outlet belongs to the tenant / is active). This service must never touch
 * outlet-scoped rows — keeping {@code platform.tenantconfig} free of any
 * {@code core.outlet} dependency.
 *
 * <p>Idempotent upsert on the natural key (tenant, NULL, date). Tenant isolation
 * is enforced by RLS; {@code tenantId} comes from the request context, never
 * from client input.
 */
@Service
@RequiredArgsConstructor
public class BusinessDayOverrideAdminService {

  private final BusinessDayOverrideJpaRepository repo;
  private final BusinessDayOverrideMapper mapper;

  @Transactional
  public BusinessDayOverrideView upsert(TenantId tenantId, UpsertBusinessDayOverrideRequest req) {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(req.businessDate(), "businessDate is required");

    var existing = repo.findByTenantIdAndOutletIdIsNullAndBusinessDateAndDeletedAtIsNull(
        tenantId.value(), req.businessDate());

    var e = existing.orElseGet(BusinessDayOverrideJpaEntity::new);
    if (existing.isEmpty()) {
      e.setTenantId(tenantId.value());
      e.setOutletId(null); // tenant-level only
      e.setBusinessDate(req.businessDate());
    }
    e.setOpen(req.open());
    e.setReasonCode(blankToNull(req.reasonCode()));
    e.setLabel(blankToNull(req.label()));
    return mapper.toView(repo.save(e));
  }

  @Transactional(readOnly = true)
  public List<BusinessDayOverrideView> list(TenantId tenantId, LocalDate from, LocalDate to) {
    return mapper.toViews(
        repo.findByTenantIdAndOutletIdIsNullAndBusinessDateBetweenAndDeletedAtIsNullOrderByBusinessDateAsc(
            tenantId.value(), from, to));
  }

  @Transactional
  public void softDelete(TenantId tenantId, BusinessDayOverrideId id) {
    var uuid = (id == null) ? null : id.value();
    var e = repo.findByIdAndDeletedAtIsNull(uuid)
        .orElseThrow(() -> new EntityNotFoundException("business_day_override_not_found"));
    // RLS already guarantees the row belongs to the current tenant.
    e.setDeletedAt(Instant.now());
    repo.save(e);
  }

  private static String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s.trim();
  }
}
