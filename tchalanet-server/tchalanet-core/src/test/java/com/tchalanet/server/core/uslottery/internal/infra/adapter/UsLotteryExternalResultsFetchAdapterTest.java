package com.tchalanet.server.core.uslottery.internal.infra.adapter;

import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultFetchQuery;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderClient;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.infra.registry.ProviderClientRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("UsLotteryExternalResultsFetchAdapter — fail-soft")
class UsLotteryExternalResultsFetchAdapterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);
    private static final LocalTime TIME = LocalTime.of(12, 0);
    private static final ZoneId TZ = ZoneId.of("America/New_York");
    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    private ExternalResultFetchQuery baseQuery;

    @BeforeEach
    void setUp() {
        baseQuery = new ExternalResultFetchQuery(
            "NY", DATE, TIME, TZ, Set.of("NUMBERS", "WIN4"), "MIDDAY", false, false, NOW);
    }

    @Test
    @DisplayName("unknown provider string → empty bundle, no throw")
    void unknownProvider() {
        var adapter = adapterWithNoClients();
        var query = queryWithProvider("UNKNOWN_XYZ");

        assertThatNoException().isThrownBy(() -> {
            var result = adapter.fetchProviderResults(query);
            assertThat(result.results()).isEmpty();
        });
    }

    @Test
    @DisplayName("null provider → empty bundle, no throw")
    void nullProvider() {
        var adapter = adapterWithNoClients();
        var query = queryWithProvider(null);

        assertThatNoException().isThrownBy(() -> {
            var result = adapter.fetchProviderResults(query);
            assertThat(result.results()).isEmpty();
        });
    }

    @Test
    @DisplayName("blank provider → empty bundle, no throw")
    void blankProvider() {
        var adapter = adapterWithNoClients();
        var query = queryWithProvider("   ");

        assertThatNoException().isThrownBy(() -> {
            var result = adapter.fetchProviderResults(query);
            assertThat(result.results()).isEmpty();
        });
    }

    @Test
    @DisplayName("no client registered for known provider → empty bundle, no throw")
    void noClientRegistered() {
        var adapter = adapterWithNoClients();

        assertThatNoException().isThrownBy(() -> {
            var result = adapter.fetchProviderResults(baseQuery);
            assertThat(result.results()).isEmpty();
        });
    }

    @Test
    @DisplayName("empty gameCodes → empty bundle, no throw")
    void emptyGameCodes() {
        var adapter = adapterWithThrowingClient();
        var query = queryWithGameCodes(Set.of());

        assertThatNoException().isThrownBy(() -> {
            var result = adapter.fetchProviderResults(query);
            assertThat(result.results()).isEmpty();
        });
    }

    @Test
    @DisplayName("null gameCodes → empty bundle, no throw")
    void nullGameCodes() {
        var adapter = adapterWithThrowingClient();
        var query = queryWithGameCodes(null);

        assertThatNoException().isThrownBy(() -> {
            var result = adapter.fetchProviderResults(query);
            assertThat(result.results()).isEmpty();
        });
    }

    @Test
    @DisplayName("client.fetch() throws → empty bundle, no throw")
    void clientThrows() {
        var adapter = adapterWithThrowingClient();

        assertThatNoException().isThrownBy(() -> {
            var result = adapter.fetchProviderResults(baseQuery);
            assertThat(result.results()).isEmpty();
        });
    }

    @Test
    @DisplayName("happy path → bundle with results")
    void happyPath() {
        var adapter = adapterWithSuccessfulClient();

        var result = adapter.fetchProviderResults(baseQuery);

        assertThat(result.results()).isEmpty();
        assertThat(result.provider()).isEqualTo("NY");
        assertThat(result.drawDate()).isEqualTo(DATE);
    }

    private UsLotteryExternalResultsFetchAdapter adapterWithNoClients() {
        return new UsLotteryExternalResultsFetchAdapter(new ProviderClientRegistry(List.of()));
    }

    private UsLotteryExternalResultsFetchAdapter adapterWithThrowingClient() {
        UsLotteryProviderClient throwingClient = new UsLotteryProviderClient() {
            @Override public UsLotteryProvider provider() { return UsLotteryProvider.NY; }
            @Override public UsLotteryProviderResponse fetch(UsLotteryProviderQuery q) {
                throw new RuntimeException("simulated provider failure");
            }
        };
        return new UsLotteryExternalResultsFetchAdapter(new ProviderClientRegistry(List.of(throwingClient)));
    }

    private UsLotteryExternalResultsFetchAdapter adapterWithSuccessfulClient() {
        UsLotteryProviderClient successClient = new UsLotteryProviderClient() {
            @Override public UsLotteryProvider provider() { return UsLotteryProvider.NY; }
            @Override public UsLotteryProviderResponse fetch(UsLotteryProviderQuery q) {
                return new UsLotteryProviderResponse(UsLotteryProvider.NY, q.drawDate(), q.drawTime(), q.timezone(), List.of(), null);
            }
        };
        return new UsLotteryExternalResultsFetchAdapter(new ProviderClientRegistry(List.of(successClient)));
    }

    private ExternalResultFetchQuery queryWithProvider(String provider) {
        return new ExternalResultFetchQuery(
            provider, DATE, TIME, TZ, Set.of("NUMBERS"), "MIDDAY", false, false, NOW);
    }

    private ExternalResultFetchQuery queryWithGameCodes(Set<String> gameCodes) {
        return new ExternalResultFetchQuery(
            "NY", DATE, TIME, TZ, gameCodes, "MIDDAY", false, false, NOW);
    }
}
