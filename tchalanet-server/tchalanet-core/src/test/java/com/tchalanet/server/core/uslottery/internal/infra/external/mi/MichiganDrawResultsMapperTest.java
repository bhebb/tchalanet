package com.tchalanet.server.core.uslottery.internal.infra.external.mi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.infra.external.mi.MichiganDrawResultsClient.MiGame;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("MichiganDrawResultsMapper")
class MichiganDrawResultsMapperTest {

    private static final ZoneId TZ = ZoneId.of("America/Detroit");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 15);
    private static final LocalTime TIME = LocalTime.of(12, 59);
    private static final Instant NOW = Instant.parse("2026-06-15T16:59:00Z");

    private final MichiganDrawResultsMapper mapper =
        new MichiganDrawResultsMapper(new JsonbUtils(JsonMapper.builder().build()));

    private UsLotteryProviderQuery query(String slot) {
        return new UsLotteryProviderQuery(DATE, TIME, TZ, Set.of("PICK3"), slot, false, false, NOW);
    }

    // Real Daily 4 response (2026-06-15), batched JSON array with integer winning-number arrays.
    private static final String BODY = """
        [{"data":{"jackpot":{"jackpot":"500000","__typename":"EstimatedJackpot"},
          "nextDrawDate":{"date":"2026-06-16T16:59:00.000Z","__typename":"Date"},
          "winningNumbers":{"isBonusDrawEve":false,"isBonusDrawMid":false,"resultsPending":false,
            "drawNumbersMid":[5,0,5,6],"drawNumbersEve":[3,6,2,4],
            "drawNumbersEveRedball":null,"drawNumbersMidRedball":null,
            "__typename":"GenericWinningNumbers"},
          "logoUrl":{"logoURL":"//images.example/x.webp","__typename":"LogoURL"}}},
         {"data":{"payout":{"resultsPending":false,"__typename":"GQL_PayoutDetails"}}}]""";

    @Test
    @DisplayName("MIDDAY reads drawNumbersMid (integer array) from batched payload")
    void midday() {
        var res = mapper.map(BODY, MiGame.DAILY4, "hash", "http://mi", query("MIDDAY"));

        assertThat(res.results()).hasSize(1);
        var r = res.results().getFirst();
        assertThat(r.externalGameCode()).isEqualTo("PICK4");
        assertThat(r.main()).containsExactly("5", "0", "5", "6");
        assertThat(r.quality()).isEqualTo(ResultQuality.COMPLETE);
        assertThat(r.sourceFlags().origin()).isEqualTo("MI_GRAPHQL");
    }

    @Test
    @DisplayName("EVENING reads drawNumbersEve (integer array) from batched payload")
    void evening() {
        var res = mapper.map(BODY, MiGame.DAILY4, "hash", "http://mi", query("EVENING"));
        assertThat(res.results()).hasSize(1);
        assertThat(res.results().getFirst().main()).containsExactly("3", "6", "2", "4");
    }

    @Test
    @DisplayName("resultsPending=true yields no result")
    void resultsPending() {
        var pending = """
            [{"data":{"winningNumbers":{"resultsPending":true,
              "drawNumbersMid":null,"drawNumbersEve":null,"__typename":"GenericWinningNumbers"}}}]""";
        var res = mapper.map(pending, MiGame.DAILY3, "hash", "http://mi", query("MIDDAY"));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("single (non-batched) object and delimited string numbers are parsed")
    void singleObjectStringNumbers() {
        var body = """
            {"data":{"winningNumbers":{"drawNumbersMid":"7-8-9-0","drawNumbersEve":"","resultsPending":false}}}""";

        var res = mapper.map(body, MiGame.DAILY4, "hash", "http://mi", query("MIDDAY"));
        assertThat(res.results()).hasSize(1);
        assertThat(res.results().getFirst().main()).containsExactly("7", "8", "9", "0");
    }

    @Test
    @DisplayName("missing winningNumbers node returns empty")
    void missingNode() {
        var res = mapper.map("[{\"data\":{\"payout\":null}}]", MiGame.DAILY3, "hash", "http://mi", query("MIDDAY"));
        assertThat(res.results()).isEmpty();
    }

    @Test
    @DisplayName("garbage body returns empty, no throw")
    void garbageBody() {
        assertThatNoException().isThrownBy(() -> {
            var res = mapper.map("oops", MiGame.DAILY3, "hash", "http://mi", query("MIDDAY"));
            assertThat(res.results()).isEmpty();
        });
    }
}
