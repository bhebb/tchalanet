package com.tchalanet.server.platform.tenant.internal.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatus;
import com.tchalanet.server.catalog.tenant.api.model.TenantType;
import com.tchalanet.server.common.context.tenant.TenantContextLookup;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.internal.resolver.TenantContextLookupService;

import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantConfigContextLookupTest {

    @Test
    void implementsCommonTenantContextLookupAndMapsRegistryView() {
        var tenantId = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var view = new TenantRegistryView(
            tenantId, "demo", "Demo Tenant",
            TenantStatus.ACTIVE, TenantType.BORLETTE,
            ZoneId.of("America/Toronto"), Currency.getInstance("CAD"),
            "fr", "fr-HT",
            Optional.empty(), Optional.empty());
        var lookup = new TenantContextLookupService(new FakeRegistry(view));

        assertThat(lookup).isInstanceOf(TenantContextLookup.class);

        var result = lookup.findById(tenantId);

        assertThat(result).hasValueSatisfying(info -> {
            assertThat(info.tenantId()).isEqualTo(tenantId);
            assertThat(info.tenantZoneId().getId()).isEqualTo("America/Toronto");
            assertThat(info.currency().getCurrencyCode()).isEqualTo("CAD");
        });
    }

    private record FakeRegistry(TenantRegistryView view) implements TenantPreContextLookupApi {
        @Override public Optional<TenantRegistryView> findByCode(String c) { return Optional.of(view); }
        @Override public Optional<TenantRegistryView> findById(TenantId id) { return Optional.of(view); }
        @Override public List<TenantId> listActiveTenantIds() { return List.of(view.tenantId()); }
        @Override public TchPage<TenantRegistryView> listTenants(TchPageRequest r) { return TchPage.of(List.of(), 0, 10, 0, 0, true, false, false); }
    }
}
