package com.tchalanet.server.features.publicdrawresults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultDetailResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultHistoryResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultLatestResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSearchCriteria;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSlotsResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicDrawResultServiceTest {

  private final QueryBus queryBus = mock(QueryBus.class);
  private final PublicDrawResultViewMapper mapper = mock(PublicDrawResultViewMapper.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC);
  private final PublicDrawResultService service =
      new PublicDrawResultService(queryBus, mapper, clock);

  @Test
  void latestDelegatesQueryAndMapsResult() {
    var expected = new PublicDrawResultLatestResponse(List.of(), Instant.now());
    when(queryBus.ask(any())).thenReturn(null);
    when(mapper.toLatestResponse(isNull(), eq(5), any())).thenReturn(expected);

    var result = service.latest(List.of("GA_EVE"), null, 5);

    assertThat(result).isSameAs(expected);
  }

  @Test
  void latestNormalizesNullSlotKeysToEmptyList() {
    var expected = new PublicDrawResultLatestResponse(List.of(), Instant.now());
    when(queryBus.ask(any())).thenReturn(null);
    when(mapper.toLatestResponse(any(), any(), any())).thenReturn(expected);

    var result = service.latest(null, null, null);

    assertThat(result).isSameAs(expected);
  }

  @Test
  void slotsCallsQueryAndMapsSlots() {
    var expected = mock(PublicDrawResultSlotsResponse.class);
    when(queryBus.ask(any())).thenReturn(null);
    when(mapper.toSlotsResponse(any())).thenReturn(expected);

    var result = service.slots("GA");

    assertThat(result).isSameAs(expected);
  }

  @Test
  void historyDelegatesQueryAndMapsPage() {
    var expected = mock(PublicDrawResultHistoryResponse.class);
    when(queryBus.ask(any())).thenReturn(null);
    when(mapper.toHistoryResponse(any())).thenReturn(expected);

    var criteria = new PublicDrawResultSearchCriteria(List.of(), null, null, null, null);
    var result = service.history(criteria);

    assertThat(result).isSameAs(expected);
  }

  @Test
  void detailDelegatesQueryAndMapsDetail() {
    var drawResultId = DrawResultId.of(UUID.randomUUID());
    var expected = mock(PublicDrawResultDetailResponse.class);
    when(queryBus.ask(any())).thenReturn(null);
    when(mapper.toDetailResponse(any())).thenReturn(expected);

    var result = service.detail(drawResultId);

    assertThat(result).isSameAs(expected);
  }
}
