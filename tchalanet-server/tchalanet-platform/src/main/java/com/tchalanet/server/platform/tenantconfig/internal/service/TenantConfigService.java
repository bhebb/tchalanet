package com.tchalanet.server.platform.tenantconfig.internal.service;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatus;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.platform.address.api.model.AddressView;
import com.tchalanet.server.platform.address.internal.service.AddressCrudService;
import com.tchalanet.server.platform.tenantconfig.api.model.ActivateTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.ArchiveTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.CreateTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.GetTenantByCodeQuery;
import com.tchalanet.server.platform.tenantconfig.api.model.GetTenantByIdQuery;
import com.tchalanet.server.platform.tenantconfig.api.model.ListTenantsQuery;
import com.tchalanet.server.platform.tenantconfig.api.model.SuspendTenantCommand;
import com.tchalanet.server.platform.tenantconfig.api.model.TenantConfigView;
import com.tchalanet.server.platform.tenantconfig.api.model.UpdateTenantIdentityCommand;
import com.tchalanet.server.platform.tenantconfig.internal.adapter.TenantPersistenceAdapter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantConfigService {

  private final TenantCatalog tenantCatalog;
  private final ThemeCatalog themeCatalog;
  private final AddressCrudService addressCrudService;
  private final TenantPersistenceAdapter tenants;
  private final DomainEventPublisher publisher;
  private final Clock clock;
  private final IdGenerator idGenerator;

  @Transactional
  public void createTenant(CreateTenantCommand request) {
    var now = Instant.now(clock);
    var tenantId = com.tchalanet.server.common.types.id.TenantId.of(idGenerator.newUuid());
    var addressId =
        request.address() == null
            ? null
            : addressCrudService.upsertTenantPrimary(tenantId, request.address());
    var tenant =
        TenantConfig.createDraft(
            tenantId,
            request.code(),
            request.name(),
            request.type(),
            request.timezone(),
            request.currency(),
            addressId,
            request.activeThemeId());
    if (Boolean.TRUE.equals(request.activate())) {
      tenant = tenant.activate(now);
    }
    var saved = tenants.create(tenant);
    publishStatus(now, saved.id(), Boolean.TRUE.equals(request.activate()) ? TenantStatus.DRAFT : null, saved.status(), Boolean.TRUE.equals(request.activate()) ? "activated_on_create" : "tenant_created");
  }

  public TenantConfigView getTenantById(GetTenantByIdQuery request) {
    return toView(
        tenantCatalog
            .findRegistryById(request.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + request.tenantId())));
  }

  public TenantConfigView getTenantByCode(GetTenantByCodeQuery request) {
    var code = request.code().trim().toLowerCase();
    return toView(
        tenantCatalog
            .findRegistryByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found with code: " + code)));
  }

  public TchPage<TenantConfigView> listTenants(ListTenantsQuery request) {
    return TchPageMapper.map(tenantCatalog.listTenants(request.pageable()), registry -> toView(registry, false));
  }

  @Transactional
  public void activateTenant(ActivateTenantCommand request) {
    var tenant = load(request.tenantId());
    var previous = tenant.status();
    var now = Instant.now(clock);
    var saved = tenants.update(tenant.activate(now));
    if (saved.status() != previous) {
      publishStatus(now, saved.id(), previous, saved.status(), "activated_by_admin");
    }
  }

  @Transactional
  public void suspendTenant(SuspendTenantCommand request) {
    var tenant = load(request.tenantId());
    var previous = tenant.status();
    var now = Instant.now(clock);
    var saved = tenants.update(tenant.suspend(now));
    if (saved.status() != previous) {
      publishStatus(now, saved.id(), previous, saved.status(), request.reason());
    }
  }

  @Transactional
  public void archiveTenant(ArchiveTenantCommand request) {
    var tenant = load(request.tenantId());
    var previous = tenant.status();
    var now = Instant.now(clock);
    var saved = tenants.update(tenant.archive(now));
    if (saved.status() != previous) {
      publishStatus(now, saved.id(), previous, saved.status(), request.reason());
    }
  }

  @Transactional
  public void updateTenantIdentity(UpdateTenantIdentityCommand request) {
    var registry =
        tenantCatalog
            .findRegistryById(request.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + request.tenantId()));
    var tenant = TenantConfig.fromRegistryView(registry);
    var now = Instant.now(clock);
    var changed = new LinkedHashSet<String>();
    if (request.name() != null && !Objects.equals(request.name(), tenant.name())) {
      tenant = tenant.rename(request.name(), now);
      changed.add("name");
    }
    if (request.timezone() != null || request.currency() != null) {
      var timezone = request.timezone() == null ? tenant.timezone() : ZoneId.of(request.timezone());
      var currency = request.currency() == null ? tenant.currency() : Currency.getInstance(request.currency());
      if (!Objects.equals(timezone, tenant.timezone()) || !Objects.equals(currency, tenant.currency())) {
        tenant = tenant.updateLocale(timezone, currency, now);
        changed.add("locale");
      }
    }
    if (changed.isEmpty()) return;
    tenants.update(tenant);
    var event =
        new TenantIdentityUpdatedEvent(
            EventId.of(idGenerator.newUuid()), now, tenant.id(), Set.copyOf(changed));
    AfterCommit.run(() -> publisher.publish(event));
  }

  private TenantConfig load(com.tchalanet.server.common.types.id.TenantId tenantId) {
    return TenantConfig.fromRegistryView(
        tenantCatalog
            .findRegistryById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId)));
  }

  private TenantConfigView toView(com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView registry) {
    return toView(registry, true);
  }

  private TenantConfigView toView(
      com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView registry, boolean includeDetails) {
    AddressView address = null;
    if (includeDetails && registry.addressId().isPresent()) {
      address = addressCrudService.get(registry.tenantId(), registry.addressId().get()).orElse(null);
    }
    var themeCode =
        includeDetails && registry.activeThemeId().isPresent()
            ? themeCatalog.findById(registry.activeThemeId().get()).map(theme -> theme.code()).orElse(null)
            : null;
    return new TenantConfigView(
        registry.tenantId(),
        registry.code(),
        registry.name(),
        registry.type(),
        registry.timezone(),
        registry.currency(),
        registry.status(),
        registry.activeThemeId().orElse(null),
        themeCode,
        address);
  }

  private void publishStatus(
      Instant now,
      com.tchalanet.server.common.types.id.TenantId tenantId,
      TenantStatus from,
      TenantStatus to,
      String reason) {
    var event =
        new TenantStatusChangedEvent(
            EventId.of(idGenerator.newUuid()), now, tenantId, from, to, reason);
    AfterCommit.run(() -> publisher.publish(event));
  }
}
