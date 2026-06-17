package com.tchalanet.server.core.uslottery.internal.infra.external.nj;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("NewJerseyDrawResultsMapper")
class NewJerseyDrawResultsMapperTest {

    private static final ZoneId TZ = ZoneId.of("America/New_York");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);
    private static final LocalTime TIME = LocalTime.of(12, 30);
    private static final Instant NOW = Instant.parse("2026-06-15T16:30:00Z");

    private final NewJerseyDrawResultsMapper mapper =
        new NewJerseyDrawResultsMapper(new JsonbUtils(JsonMapper.builder().build()));

    // Trimmed real payload (drawTime 1781496000000 = 2026-06-15 ET). Each draw has a FIREBALL
    // variant (ignored) and a Regular draw whose primary is a single concatenated string.
    private static final String BODY = """
        {"draws":[
          {"gameName":"Pick 3","id":"24204","name":"EVENING","status":"CLOSED","drawTime":1781496000000,
           "results":[
             {"primary":["FB-4","446","246","244"],"drawType":"FIREBALL"},
             {"primary":["246"],"drawType":"Regular"}]},
          {"gameName":"Pick 3","id":"24203","name":"MIDDAY","status":"CLOSED","drawTime":1781496000000,
           "results":[
             {"primary":["FB-6","642","962","946"],"drawType":"FIREBALL"},
             {"primary":["942"],"drawType":"Regular"}]}
        ]}""";

    private UsLotteryProviderQuery query(Set<String> codes, String slot, LocalDate date) {
        return new UsLotteryProviderQuery(date, TIME, TZ, codes, slot, false, false, NOW);
    }

    @Test
    @DisplayName("MIDDAY: splits Regular draw digits, ignores FIREBALL variant")
    void extractsCorrect() {
        var res = mapper.map(BODY, "hash", "http://nj", query(Set.of("PICK3"), "MIDDAY", DATE));

        assertThat(res.results()).hasSize(1);
        var r = res.results().getFirst();
        assertThat(r.externalGameCode()).isEqualTo("PICK3");
        assertThat(r.main()).containsExactly("9", "4", "2");
        assertThat(r.quality()).isEqualTo(ResultQuality.COMPLETE);
        assertThat(r.sourceFlags().origin()).isEqualTo("NJ_API");
    }

    @Test
    @DisplayName("evening slot selects the evening draw")
    void eveningSlot() {
        var res = mapper.map(BODY, "hash", "http://nj", query(Set.of("PICK3"), "EVENING", DATE));

        assertThat(res.results()).hasSize(1);
        assertThat(res.results().getFirst().main()).containsExactly("2", "4", "6");
    }

    @Test
    @DisplayName("wrong slot returns no result")
    void wrongSlot() {
        var midOnly = """
            {"draws":[{"gameName":"Pick 3","name":"MIDDAY","status":"CLOSED","drawTime":1781496000000,
              "results":[{"primary":["246"],"drawType":"Regular"}]}]}""";

        var res = mapper.map(midOnly, "hash", "http://nj", query(Set.of("PICK3"), "EVENING", DATE));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("wrong date returns no result")
    void wrongDate() {
        var res = mapper.map(BODY, "hash", "http://nj", query(Set.of("PICK3"), "MIDDAY", DATE.plusDays(1)));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("real OPEN draw (rule set, no results) is skipped")
    void notClosed() {
        // Trimmed real v2 payload: an OPEN upcoming draw carries a gameRuleSet and empty prizeTiers
        // but no winning numbers.
        var open = """
            {"draws":[{"gameName":"Pick 3","id":"24205","name":"MIDDAY","status":"OPEN",
              "closeTime":1781628795000,"drawTime":1781582400000,
              "gameRuleSet":{"basePrice":50,"minPrimarySelections":3,"maxPrimarySelections":3},
              "estimatedJackpot":0,"prizeTiers":[]}]}""";

        var res = mapper.map(open, "hash", "http://nj", query(Set.of("PICK3"), "MIDDAY", DATE));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("malformed entry does not fail the whole mapping")
    void malformedEntryTolerated() {
        var mixed = """
            {"draws":[
              {"gameName":"Pick 3","name":"MIDDAY","status":"CLOSED","drawTime":1781496000000,"results":null},
              {"gameName":"Pick 3","name":"MIDDAY","status":"CLOSED","drawTime":1781496000000,
               "results":[{"primary":["FB-1","112"],"drawType":"FIREBALL"},
                          {"primary":["123"],"drawType":"Regular"}]}
            ]}""";

        assertThatNoException().isThrownBy(() -> {
            var res = mapper.map(mixed, "hash", "http://nj", query(Set.of("PICK3"), "MIDDAY", DATE));
            assertThat(res.results()).hasSize(1);
            assertThat(res.results().getFirst().main()).containsExactly("1", "2", "3");
        });
    }

    @Test
    @DisplayName("garbage body returns empty, no throw")
    void garbageBody() {
        assertThatNoException().isThrownBy(() -> {
            var res = mapper.map("not json", "hash", "http://nj", query(Set.of("PICK3"), "MIDDAY", DATE));
            assertThat(res.results()).isEmpty();
        });
    }
}
