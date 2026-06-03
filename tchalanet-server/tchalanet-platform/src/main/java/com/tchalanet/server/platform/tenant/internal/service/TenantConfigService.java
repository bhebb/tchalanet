package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.platform.address.api.model.AddressView;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.request.ActivateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.ArchiveTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.CreateTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByCodeRequest;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.request.ListTenantsRequest;
import com.tchalanet.server.platform.tenant.api.model.request.SuspendTenantRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantIdentityRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsRequest;
import com.tchalanet.server.platform.tenant.api.model.view.TenantConfigView;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalDocumentConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenant.api.model.view.TenantRuntimeView;
import com.tchalanet.server.platform.tenant.internal.adapter.TenantPersistenceAdapter;
import com.tchalanet.server.platform.tenant.internal.domain.TenantConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Currency;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/*
 * Application service for tenant configuration lifecycle.
 *
 * <p>Internal settings are stored in tenant `config` (jsonb). During tenant creation,
 * settings are assembled by merging classpath fragments from `tenantconfig/*.json`.
 *
 * <p>For mutable settings updates, `communication` and `document.receipt` blocks are validated.
 * Schema-level validation can be introduced later without changing this persistence contract.
 */
@Service
@RequiredArgsConstructor
public class TenantConfigService {

    private final TenantPreContextLookupApi tenantRegistry;
    private final ThemeCatalog themeCatalog;
    private final AddressApi addressApi;
    private final TenantPersistenceAdapter tenants;
    private final DomainEventPublisher publisher;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final JsonUtils jsonUtils;
    private final TenantConfigValidator configValidator;

    @Transactional
    public void createTenant(CreateTenantRequest request) {
        var now = Instant.now(clock);
        var tenantId = com.tchalanet.server.common.types.id.TenantId.of(idGenerator.newUuid());
        var addressId =
            request.address() == null
                ? null
                : addressApi.upsertTenantPrimary(tenantId, request.address());
        JsonNode tenantInternalJson = getTenantInternalSettings();
        var tenant =
            TenantConfig.createDraft(
                tenantId,
                request.code(),
                request.name(),
                request.type(),
                request.timezone(),
                request.currency(),
                addressId,
                request.activeThemeId(),
                tenantInternalJson);
        if (Boolean.TRUE.equals(request.activate())) {
            tenant = tenant.activate(now);
        }
        var saved = tenants.create(tenant);
        publishStatus(now, saved.id(), Boolean.TRUE.equals(request.activate()) ? TenantStatus.DRAFT : null, saved.status(), Boolean.TRUE.equals(request.activate()) ? "activated_on_create" : "tenant_created");
    }

    /**
     * Builds default tenant settings by deep-merging all JSON fragments under
     * `classpath*:tenantconfig/*.json`.
     */
    private JsonNode getTenantInternalSettings() {
        var resolver = new PathMatchingResourcePatternResolver();
        try {
            var resources = resolver.getResources("classpath*:tenantconfig/*.json");
            if (resources.length == 0) {
                throw new IllegalStateException("No tenantconfig JSON found on classpath");
            }

            Arrays.sort(resources, Comparator.comparing(r -> r.getFilename() == null ? "" : r.getFilename()));
            var merged = jsonUtils.emptyObject();
            for (var resource : resources) {
                try (var is = resource.getInputStream()) {
                    var fragment = jsonUtils.parse(is);
                    if (fragment == null) {
                        continue;
                    }
                    if (!fragment.isObject()) {
                        throw new IllegalStateException("Tenant config fragment must be a JSON object: " + resource.getFilename());
                    }
                    deepMerge(merged, fragment);
                }
            }

            configValidator.validateAll(merged);
            return merged;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load tenant internal settings JSON", e);
        }
    }

    private void deepMerge(ObjectNode target, JsonNode source) {
        source.properties().forEach(entry -> {
            var fieldName = entry.getKey();
            var sourceValue = entry.getValue();
            var targetValue = target.get(fieldName);

            if (targetValue != null && targetValue.isObject() && sourceValue.isObject()) {
                deepMerge((ObjectNode) targetValue, sourceValue);
                return;
            }
            if (targetValue != null && targetValue.isArray() && sourceValue.isArray()) {
                ((ArrayNode) targetValue).addAll((ArrayNode) sourceValue);
                return;
            }
            target.set(fieldName, sourceValue);
        });
    }

    public TenantConfigView getTenantById(GetTenantByIdRequest request) {
        return toView(
            tenantRegistry
                .findById(request.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + request.tenantId())));
    }

    public TenantConfigView getTenantByCode(GetTenantByCodeRequest request) {
        var code = request.code().trim().toLowerCase();
        return toView(
            tenantRegistry
                .findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with code: " + code)));
    }

    public TchPage<TenantConfigView> listTenants(ListTenantsRequest request) {
        return TchPageMapper.map(tenantRegistry.listTenants(PageRequest.of(request.pageable().getPageNumber(), request.pageable().getPageSize())), registry -> toView(registry, false));
    }

    @Transactional
    public void activateTenant(ActivateTenantRequest request) {
        var tenant = load(request.tenantId());
        var previous = tenant.status();
        var now = Instant.now(clock);
        var saved = tenants.update(tenant.activate(now));
        if (saved.status() != previous) {
            publishStatus(now, saved.id(), previous, saved.status(), "activated_by_admin");
        }
    }

    @Transactional
    public void suspendTenant(SuspendTenantRequest request) {
        var tenant = load(request.tenantId());
        var previous = tenant.status();
        var now = Instant.now(clock);
        var saved = tenants.update(tenant.suspend(now));
        if (saved.status() != previous) {
            publishStatus(now, saved.id(), previous, saved.status(), request.reason());
        }
    }

    @Transactional
    public void archiveTenant(ArchiveTenantRequest request) {
        var tenant = load(request.tenantId());
        var previous = tenant.status();
        var now = Instant.now(clock);
        var saved = tenants.update(tenant.archive(now));
        if (saved.status() != previous) {
            publishStatus(now, saved.id(), previous, saved.status(), request.reason());
        }
    }

    @Transactional
    public void updateTenantIdentity(UpdateTenantIdentityRequest request) {
        var tenant = load(request.tenantId());
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

    @Transactional
    public void updateTenantInternalSettings(UpdateTenantInternalSettingsRequest request) {
        configValidator.validateAll(request.settings());

        var tenant = load(request.tenantId());
        var now = Instant.now(clock);
        tenants.update(tenant.updateConfig(request.settings(), now));
    }

    /**
     * Returns the typed communication sub-config from persisted tenant internal settings.
     *
     * <p>Lookup is direct and required (no optional branching in service layer).
     */
    @Transactional(readOnly = true)
    public TenantInternalCommunicationConfig getTenantCommunicationConfig(GetTenantByIdRequest request) {
        var tenant = tenants.getRequiredByIdActive(request.tenantId());
        var config = tenant.config();
        if (config == null || config.isNull()) {
            return null;
        }
        var typed = jsonUtils.treeToValue(config, TenantInternalSettings.class);
        return typed == null ? null : typed.communication();
    }

    @Transactional(readOnly = true)
    public TenantInternalDocumentConfig getTenantDocumentConfig(GetTenantByIdRequest request) {
        var tenant = tenants.getRequiredByIdActive(request.tenantId());
        var config = tenant.config();
        if (config == null || config.isNull()) {
            return null;
        }
        var typed = jsonUtils.treeToValue(config, TenantInternalSettings.class);
        return typed == null ? null : typed.document();
    }

    @Transactional(readOnly = true)
    public TenantRuntimeView getTenantRuntimeView(String tenantCode) {
        var code = tenantCode.trim().toLowerCase();
        var registry = tenantRegistry.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + code));
        var locale = tenants.findByIdActive(registry.tenantId())
            .map(tc -> {
                if (tc.config() == null || tc.config().isNull()) return null;
                return jsonUtils.treeToValue(tc.config(), TenantInternalSettings.class);
            })
            .map(TenantInternalSettings::locale)
            .orElse(null);
        List<String> supportedLocales = locale != null ? locale.effectiveSupportedLanguages() : java.util.List.of();
        return new TenantRuntimeView(
            registry.code(),
            registry.name(),
            registry.status().name(),
            registry.timezone(),
            registry.currency(),
            registry.defaultLanguage(),
            registry.defaultLocale(),
            supportedLocales);
    }

    private TenantConfig load(com.tchalanet.server.common.types.id.TenantId tenantId) {
        return tenants.getRequiredByIdActive(tenantId);
    }

    private TenantConfigView toView(TenantContextLookupView registry) {
        return toView(registry, true);
    }

    private TenantConfigView toView(
        TenantContextLookupView registry, boolean includeDetails) {
        AddressView address = null;
        if (includeDetails && registry.addressId().isPresent()) {
            address = addressApi.get(registry.tenantId(), registry.addressId().get()).orElse(null);
        }
        var themeCode =
            includeDetails && registry.activeThemeId().isPresent()
                ? themeCatalog.findById(registry.activeThemeId().get()).map(theme -> theme.code()).orElse(null)
                : null;
        var internalSettings =
            includeDetails
                ? tenants.findByIdActive(registry.tenantId()).map(TenantConfig::config).orElse(null)
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
            address,
            internalSettings);
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
