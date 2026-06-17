package com.tchalanet.server.core.uslottery.internal.infra.external.pa;

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

@DisplayName("PennsylvaniaDrawResultsMapper")
class PennsylvaniaDrawResultsMapperTest {

    private static final ZoneId TZ = ZoneId.of("America/New_York");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);
    private static final LocalTime TIME = LocalTime.of(13, 35);
    private static final Instant NOW = Instant.parse("2026-06-15T17:35:00Z");

    private final PennsylvaniaDrawResultsMapper mapper =
        new PennsylvaniaDrawResultsMapper(new JsonbUtils(JsonMapper.builder().build()));

    // Trimmed real DrawingsData.aspx payload: PICK 4 day entry nesting its evening draw,
    // plus an unrelated jackpot game that must be ignored. Wild Ball is the trailing digit.
    private static final String BODY = """
        [
          {"DrawingDateAsString":"6/15/2026","GameName":"PICK 4","DrawingName":"PICK 4 (DAY)",
           "GameBallCount":4,"DrawingDate":"/Date(1781496000000)/",
           "DrawingNumbersAsList":["5","1","5","0","6"],"IsMidDayDrawing":true,
           "RelatedEveningDrawing":{"DrawingDateAsString":"6/15/2026","GameName":"PICK 4",
             "DrawingName":"PICK 4","GameBallCount":4,"DrawingDate":"/Date(1781496000000)/",
             "DrawingNumbersAsList":["2","8","1","1","3"],"IsMidDayDrawing":false,
             "RelatedEveningDrawing":null}},
          {"DrawingDateAsString":"6/15/2026","GameName":"Cash 5","DrawingName":"Cash 5",
           "GameBallCount":5,"DrawingDate":"/Date(1781496000000)/",
           "DrawingNumbersAsList":["01","06","19","23","35"],"IsMidDayDrawing":false,
           "RelatedEveningDrawing":null}
        ]""";

    private UsLotteryProviderQuery query(String slot) {
        return new UsLotteryProviderQuery(DATE, TIME, TZ, Set.of("PICK4"), slot, false, false, NOW);
    }

    @Test
    @DisplayName("midday PICK 4: strips Wild Ball into extras, main is first 4 digits")
    void middayPick4() {
        var res = mapper.map(BODY, "hash", "http://pa", query("MIDDAY"));

        assertThat(res.results()).hasSize(1);
        var r = res.results().getFirst();
        assertThat(r.externalGameCode()).isEqualTo("PICK4");
        assertThat(r.main()).containsExactly("5", "1", "5", "0");
        assertThat(r.extras()).containsExactly("6");
        assertThat(r.quality()).isEqualTo(ResultQuality.COMPLETE);
        assertThat(r.sourceFlags().origin()).isEqualTo("PA_API");
        assertThat(r.sourceFlags().metadata()).containsEntry("wild_ball", "6");
    }

    @Test
    @DisplayName("evening slot reads the nested RelatedEveningDrawing")
    void eveningNested() {
        var res = mapper.map(BODY, "hash", "http://pa", query("EVENING"));

        assertThat(res.results()).hasSize(1);
        var r = res.results().getFirst();
        assertThat(r.main()).containsExactly("2", "8", "1", "1");
        assertThat(r.extras()).containsExactly("3");
    }

    @Test
    @DisplayName("unrequested games (Cash 5) are ignored")
    void ignoresOtherGames() {
        var res = mapper.map(BODY, "hash", "http://pa", query("MIDDAY"));
        assertThat(res.results()).allSatisfy(r -> assertThat(r.externalGameCode()).isEqualTo("PICK4"));
    }

    @Test
    @DisplayName("wrong date returns no result")
    void wrongDate() {
        var body = BODY.replace("/Date(1781496000000)/", "/Date(1607000000000)/")
            .replace("6/15/2026", "6/14/2026");
        var res = mapper.map(body, "hash", "http://pa", query("MIDDAY"));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("malformed entry does not fail the whole mapping")
    void malformedTolerated() {
        var body = """
            [
              {"GameName":"PICK 4","DrawingNumbersAsList":null,"IsMidDayDrawing":true,"GameBallCount":4},
              {"DrawingDateAsString":"6/15/2026","GameName":"PICK 4","GameBallCount":4,
               "DrawingDate":"/Date(1781496000000)/","DrawingNumbersAsList":["1","2","3","4","9"],
               "IsMidDayDrawing":true,"RelatedEveningDrawing":null}
            ]""";

        assertThatNoException().isThrownBy(() -> {
            var res = mapper.map(body, "hash", "http://pa", query("MIDDAY"));
            assertThat(res.results()).hasSize(1);
            assertThat(res.results().getFirst().main()).containsExactly("1", "2", "3", "4");
        });
    }

    @Test
    @DisplayName("garbage body returns empty, no throw")
    void garbageBody() {
        assertThatNoException().isThrownBy(() -> {
            var res = mapper.map("<rss/>", "hash", "http://pa", query("MIDDAY"));
            assertThat(res.results()).isEmpty();
        });
    }
}
