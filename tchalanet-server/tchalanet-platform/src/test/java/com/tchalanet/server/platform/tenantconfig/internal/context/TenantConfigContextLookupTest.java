package com.tchalanet.server.platform.tenantconfig.internal.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantBootstrapView;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.catalog.tenant.api.model.TenantStatsView;
import com.tchalanet.server.common.context.tenant.TenantContextLookup;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;

class TenantConfigContextLookupTest {

    @Test
    void implementsCommonTenantContextLookupAndMapsBootstrapView() {
        var tenantId = TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var timezone = ZoneId.of("America/Toronto");
        var currency = Currency.getInstance("CAD");
        var lookup = new TenantConfigContextLookup(new FakeTenantCatalog(new TenantBootstrapView(
                tenantId,
                "demo",
                "ACTIVE",
                "COMMERCIAL",
                timezone,
                currency)));

        assertThat(lookup).isInstanceOf(TenantContextLookup.class);

        var result = lookup.findById(tenantId);

        assertThat(result).hasValueSatisfying(info -> {
            assertThat(info.tenantId()).isEqualTo(tenantId);
            assertThat(info.tenantZoneId()).isEqualTo(timezone);
            assertThat(info.currency()).isEqualTo(currency);
        });
    }

    private record FakeTenantCatalog(TenantBootstrapView view) implements TenantCatalog {

        @Override
        public Optional<TenantId> findIdByCode(String codeLower) {
            return Optional.of(view.tenantId());
        }

        @Override
        public Optional<TenantBootstrapView> findBootstrapByCode(String codeLower) {
            return Optional.of(view);
        }

        @Override
        public Optional<TenantBootstrapView> findBootstrapById(TenantId tenantId) {
            return Optional.of(view);
        }

        @Override
        public Optional<TenantRegistryView> findRegistryById(TenantId tenantId) {
            return Optional.empty();
        }

        @Override
        public Optional<TenantRegistryView> findRegistryByCode(String codeLower) {
            return Optional.empty();
        }

        @Override
        public List<TenantId> listActiveTenantIds() {
            return List.of(view.tenantId());
        }

        @Override
        public Page<TenantRegistryView> listTenants(Pageable pageable) {
            return Page.empty(pageable);
        }

        @Override
        public TenantStatsView stats() {
            return null;
        }
    }
}
