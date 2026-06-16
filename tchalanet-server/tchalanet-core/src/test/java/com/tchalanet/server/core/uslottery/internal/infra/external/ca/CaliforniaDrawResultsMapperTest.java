package com.tchalanet.server.core.uslottery.internal.infra.external.ca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.infra.external.ca.CaliforniaDrawResultsClient.CaGame;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("CaliforniaDrawResultsMapper")
class CaliforniaDrawResultsMapperTest {

    private static final ZoneId TZ = ZoneId.of("America/Los_Angeles");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);
    private static final LocalTime TIME = LocalTime.of(18, 30);
    private static final Instant NOW = Instant.parse("2026-06-16T01:30:00Z");

    private final CaliforniaDrawResultsMapper mapper =
        new CaliforniaDrawResultsMapper(new JsonbUtils(JsonMapper.builder().build()));

    // Trimmed real DrawGamePastDrawResults payload (gameId 9, Daily 3). Two draws on 6/15:
    // DrawNumber 21107 (evening, 5-8-7) and 21106 (midday, 5-2-6); plus a 6/14 draw.
    private static final String BODY = """
        {"DrawGameId":9,"Name":"Daily 3",
         "MostRecentDraw":{"DrawNumber":21107,"DrawDate":"2026-06-15T07:00:00",
           "WinningNumbers":{"1":{"Number":"5","IsSpecial":false},"2":{"Number":"8","IsSpecial":false},"3":{"Number":"7","IsSpecial":false}}},
         "PreviousDraws":[
           {"DrawNumber":21107,"DrawDate":"2026-06-15T07:00:00",
            "WinningNumbers":{"1":{"Number":"5"},"2":{"Number":"8"},"3":{"Number":"7"}}},
           {"DrawNumber":21106,"DrawDate":"2026-06-15T07:00:00",
            "WinningNumbers":{"1":{"Number":"5"},"2":{"Number":"2"},"3":{"Number":"6"}}},
           {"DrawNumber":21105,"DrawDate":"2026-06-14T07:00:00",
            "WinningNumbers":{"1":{"Number":"6"},"2":{"Number":"6"},"3":{"Number":"2"}}}
         ]}""";

    private UsLotteryProviderQuery query(String slot, LocalDate date) {
        return new UsLotteryProviderQuery(date, TIME, TZ, Set.of("DAILY3"), slot, false, false, NOW);
    }

    @Test
    @DisplayName("EVENING picks the highest DrawNumber for the date")
    void eveningPicksLatest() {
        var res = mapper.map(BODY, CaGame.DAILY3, "hash", "http://ca", query("EVENING", DATE));

        assertThat(res.results()).hasSize(1);
        var r = res.results().getFirst();
        assertThat(r.externalGameCode()).isEqualTo("DAILY3");
        assertThat(r.main()).containsExactly("5", "8", "7");
        assertThat(r.quality()).isEqualTo(ResultQuality.COMPLETE);
        assertThat(r.sourceFlags().origin()).isEqualTo("CA_API");
        assertThat(r.sourceFlags().metadata()).containsEntry("draw_number", "21107");
    }

    @Test
    @DisplayName("MIDDAY picks the second-highest DrawNumber for the date")
    void middayPicksEarlier() {
        var res = mapper.map(BODY, CaGame.DAILY3, "hash", "http://ca", query("MIDDAY", DATE));

        assertThat(res.results()).hasSize(1);
        assertThat(res.results().getFirst().main()).containsExactly("5", "2", "6");
        assertThat(res.results().getFirst().sourceFlags().metadata()).containsEntry("draw_number", "21106");
    }

    @Test
    @DisplayName("blank slot takes the most recent draw")
    void blankSlot() {
        var res = mapper.map(BODY, CaGame.DAILY3, "hash", "http://ca", query("", DATE));
        assertThat(res.results()).hasSize(1);
        assertThat(res.results().getFirst().main()).containsExactly("5", "8", "7");
    }

    @Test
    @DisplayName("wrong date returns no result")
    void wrongDate() {
        var res = mapper.map(BODY, CaGame.DAILY3, "hash", "http://ca", query("EVENING", LocalDate.of(2026, 6, 13)));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("single draw on date is returned for any slot")
    void singleDraw() {
        var res = mapper.map(BODY, CaGame.DAILY3, "hash", "http://ca", query("MIDDAY", LocalDate.of(2026, 6, 14)));
        assertThat(res.results()).hasSize(1);
        assertThat(res.results().getFirst().main()).containsExactly("6", "6", "2");
    }

    @Test
    @DisplayName("garbage body returns empty, no throw")
    void garbageBody() {
        assertThatNoException().isThrownBy(() -> {
            var res = mapper.map("<html/>", CaGame.DAILY3, "hash", "http://ca", query("EVENING", DATE));
            assertThat(res.results()).isEmpty();
        });
    }
}
