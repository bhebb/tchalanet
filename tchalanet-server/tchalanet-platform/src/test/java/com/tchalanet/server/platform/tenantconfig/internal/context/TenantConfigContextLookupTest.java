package com.tchalanet.server.platform.tenant.internal.context;

import com.tchalanet.server.common.context.tenant.TenantContextLookup;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantContextLookupView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatsView;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import com.tchalanet.server.platform.tenant.internal.resolver.TenantContextLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantConfigContextLookupTest {

    @Test
    void implementsCommonTenantContextLookupAndMapsRegistryView() {
        var tenantId = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var view = new TenantContextLookupView(
            tenantId, "demo", "Demo Tenant",
            TenantStatus.ACTIVE, TenantType.BORLETTE,
            ZoneId.of("America/Toronto"), Currency.getInstance("CAD"),
            "fr", "fr-HT",
            Optional.empty(), Optional.empty(), Optional.empty());
        var lookup = new TenantContextLookupService(new FakeRegistry(view));

        assertThat(lookup).isInstanceOf(TenantContextLookup.class);

        var result = lookup.findById(tenantId);

        assertThat(result).hasValueSatisfying(info -> {
            assertThat(info.tenantId()).isEqualTo(tenantId);
            assertThat(info.tenantZoneId().getId()).isEqualTo("America/Toronto");
            assertThat(info.currency().getCurrencyCode()).isEqualTo("CAD");
        });
    }

    private record FakeRegistry(TenantContextLookupView view) implements TenantPreContextLookupApi {
        @Override
        public Optional<TenantContextLookupView> findByCode(String c) {
            return Optional.of(view);
        }

        @Override
        public Optional<TenantContextLookupView> findById(TenantId id) {
            return Optional.of(view);
        }

        @Override
        public List<TenantId> listActiveTenantIds() {
            return List.of(view.tenantId());
        }

        @Override
        public TchPage<TenantContextLookupView> listTenants(PageRequest r) {
            return TchPage.of(List.of(), 0, 10, 0, 0, true, false, false);
        }

        @Override
        public TenantStatsView stats() {
            return new TenantStatsView(1, 1, 0);
        }
    }
}
