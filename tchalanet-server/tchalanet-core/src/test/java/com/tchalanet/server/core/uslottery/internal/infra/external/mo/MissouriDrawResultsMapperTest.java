package com.tchalanet.server.core.uslottery.internal.infra.external.mo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MissouriDrawResultsMapper")
class MissouriDrawResultsMapperTest {

  private static final ZoneId TZ = ZoneId.of("America/Chicago");
  private static final LocalDate DATE = LocalDate.of(2026, 8, 6);
  private static final Instant NOW = Instant.parse("2026-08-07T12:00:00Z");

  private final MissouriDrawResultsMapper mapper = new MissouriDrawResultsMapper();

  private static final String PICK3_HTML =
      """
      <html><body>
        <h1>Winning Numbers</h1>
        <section>
          Thursday, Aug 6
          Evening
          <span>5</span><span>7</span><span>7</span>
          Wild Ball: 3
          Midday
          <span>9</span><span>1</span><span>6</span>
          Wild Ball: 2
        </section>
        <section>
          Wednesday, Aug 5 Evening 1 2 3 Wild Ball: 4 Midday 4 5 6 Wild Ball: 7
        </section>
      </body></html>
      """;

  private static final String PICK4_HTML =
      """
      <html><body>
        <section>
          Thursday, Aug 6
          Evening 1 8 7 8 Wild Ball: 0
          Midday 5 5 1 8 Wild Ball: 0
        </section>
      </body></html>
      """;

  private UsLotteryProviderQuery query(String slot, LocalTime time, String gameCode) {
    return new UsLotteryProviderQuery(DATE, time, TZ, Set.of(gameCode), slot, false, false, NOW);
  }

  @Test
  @DisplayName("maps Pick 3 evening result and Wild Ball extra")
  void pick3Evening() {
    var res =
        mapper.map(
            PICK3_HTML,
            "PICK3",
            "hash",
            "https://mo/pick3",
            query("EVENING", LocalTime.of(21, 0), "PICK3"));

    assertThat(res.results()).hasSize(1);
    var r = res.results().getFirst();
    assertThat(r.externalGameCode()).isEqualTo("PICK3");
    assertThat(r.main()).containsExactly("5", "7", "7");
    assertThat(r.extras()).containsExactly("3");
    assertThat(r.quality()).isEqualTo(ResultQuality.COMPLETE);
    assertThat(r.sourceFlags().origin()).isEqualTo("MO_HTML");
    assertThat(r.sourceFlags().metadata()).containsEntry("provider_slot_code", "EVENING");
  }

  @Test
  @DisplayName("maps Pick 3 midday result")
  void pick3Midday() {
    var res =
        mapper.map(
            PICK3_HTML,
            "PICK3",
            "hash",
            "https://mo/pick3",
            query("MIDDAY", LocalTime.of(12, 45), "PICK3"));

    assertThat(res.results()).hasSize(1);
    assertThat(res.results().getFirst().main()).containsExactly("9", "1", "6");
    assertThat(res.results().getFirst().extras()).containsExactly("2");
  }

  @Test
  @DisplayName("maps Pick 4 evening result")
  void pick4Evening() {
    var res =
        mapper.map(
            PICK4_HTML,
            "PICK4",
            "hash",
            "https://mo/pick4",
            query("EVENING", LocalTime.of(21, 0), "PICK4"));

    assertThat(res.results()).hasSize(1);
    assertThat(res.results().getFirst().externalGameCode()).isEqualTo("PICK4");
    assertThat(res.results().getFirst().main()).containsExactly("1", "8", "7", "8");
    assertThat(res.results().getFirst().extras()).containsExactly("0");
  }

  @Test
  @DisplayName("wrong slot or game returns no result")
  void wrongSlotOrGame() {
    assertThat(
            mapper
                .map(
                    PICK3_HTML,
                    "PICK3",
                    "hash",
                    "https://mo/pick3",
                    query("NIGHT", LocalTime.of(21, 0), "PICK3"))
                .results())
        .isEmpty();
    assertThat(
            mapper
                .map(
                    PICK3_HTML,
                    "PICK3",
                    "hash",
                    "https://mo/pick3",
                    query("EVENING", LocalTime.of(21, 0), "PICK4"))
                .results())
        .isEmpty();
  }

  @Test
  @DisplayName("malformed HTML is tolerated")
  void malformedTolerated() {
    assertThatNoException()
        .isThrownBy(
            () -> {
              var res =
                  mapper.map(
                      "<html>no result",
                      "PICK3",
                      "hash",
                      "https://mo/pick3",
                      query("EVENING", LocalTime.of(21, 0), "PICK3"));
              assertThat(res.results()).isEmpty();
            });
  }
}
