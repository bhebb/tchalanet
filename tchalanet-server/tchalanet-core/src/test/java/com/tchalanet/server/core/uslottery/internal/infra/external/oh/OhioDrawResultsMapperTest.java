package com.tchalanet.server.core.uslottery.internal.infra.external.oh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.infra.external.oh.OhioDrawResultsClient.OhGame;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("OhioDrawResultsMapper")
class OhioDrawResultsMapperTest {

    private static final ZoneId TZ = ZoneId.of("America/New_York");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);
    private static final LocalTime TIME = LocalTime.of(12, 29);
    private static final Instant NOW = Instant.parse("2026-06-15T16:29:00Z");

    private final OhioDrawResultsMapper mapper =
        new OhioDrawResultsMapper(new JsonbUtils(JsonMapper.builder().build()));

    private UsLotteryProviderQuery query(String slot, LocalDate date) {
        return new UsLotteryProviderQuery(date, TIME, TZ, Set.of("PICK3"), slot, false, false, NOW);
    }

    // Real-structure fixture (matches OH API shape captured 2026-06-16).
    // drawDate is local ET with no timezone suffix. Slot inferred: hour<15=MIDDAY, ≥15=EVENING.
    private static final String BODY = """
        {"statusCode":200,"data":[
          {"id":11001,"drawDate":"2026-06-15T12:29:00",
           "numbers":[
             {"value":1,"position":1},
             {"value":2,"position":2},
             {"value":3,"position":3}
           ],"approved":true},
          {"id":11002,"drawDate":"2026-06-15T19:29:00",
           "numbers":[
             {"value":4,"position":1},
             {"value":5,"position":2},
             {"value":6,"position":3}
           ],"approved":true}
        ]}""";

    @Test
    @DisplayName("MIDDAY draw: hour<15 inferred, extracts digits in position order")
    void extractsCorrect() {
        var res = mapper.map(BODY, OhGame.PICK3, "hash", "http://oh", query("MIDDAY", DATE));

        assertThat(res.results()).hasSize(1);
        var r = res.results().getFirst();
        assertThat(r.externalGameCode()).isEqualTo("PICK3");
        assertThat(r.main()).containsExactly("1", "2", "3");
        assertThat(r.quality()).isEqualTo(ResultQuality.COMPLETE);
        assertThat(r.sourceFlags().origin()).isEqualTo("OH_API");
    }

    @Test
    @DisplayName("EVENING draw: hour≥15 inferred, selects the 19:29 entry")
    void eveningSlot() {
        var res = mapper.map(BODY, OhGame.PICK3, "hash", "http://oh", query("EVENING", DATE));
        assertThat(res.results()).hasSize(1);
        assertThat(res.results().getFirst().main()).containsExactly("4", "5", "6");
    }

    @Test
    @DisplayName("wrong slot: only MIDDAY draw present, EVENING query returns empty")
    void wrongSlot() {
        var midOnly = """
            {"statusCode":200,"data":[
              {"id":11001,"drawDate":"2026-06-15T12:29:00",
               "numbers":[{"value":1,"position":1},{"value":2,"position":2},{"value":3,"position":3}],
               "approved":true}
            ]}""";
        var res = mapper.map(midOnly, OhGame.PICK3, "hash", "http://oh", query("EVENING", DATE));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("wrong date returns no result")
    void wrongDate() {
        var res = mapper.map(BODY, OhGame.PICK3, "hash", "http://oh", query("MIDDAY", DATE.minusDays(2)));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("approved:false draw is skipped")
    void unapprovedSkipped() {
        var unapproved = """
            {"statusCode":200,"data":[
              {"id":11001,"drawDate":"2026-06-15T12:29:00",
               "numbers":[{"value":1,"position":1},{"value":2,"position":2},{"value":3,"position":3}],
               "approved":false}
            ]}""";
        var res = mapper.map(unapproved, OhGame.PICK3, "hash", "http://oh", query("MIDDAY", DATE));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("malformed draw does not fail the whole mapping")
    void malformedTolerated() {
        var body = """
            {"statusCode":200,"data":[
              {"id":999,"drawDate":"bad-date","numbers":null,"approved":true},
              {"id":11001,"drawDate":"2026-06-15T12:29:00",
               "numbers":[{"value":1,"position":1},{"value":2,"position":2},{"value":3,"position":3}],
               "approved":true}
            ]}""";

        assertThatNoException().isThrownBy(() -> {
            var res = mapper.map(body, OhGame.PICK3, "hash", "http://oh", query("MIDDAY", DATE));
            assertThat(res.results()).hasSize(1);
            assertThat(res.results().getFirst().main()).containsExactly("1", "2", "3");
        });
    }

    @Test
    @DisplayName("garbage body returns empty, no throw")
    void garbageBody() {
        assertThatNoException().isThrownBy(() -> {
            var res = mapper.map("nope", OhGame.PICK3, "hash", "http://oh", query("MIDDAY", DATE));
            assertThat(res.results()).isEmpty();
        });
    }
}
