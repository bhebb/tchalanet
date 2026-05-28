package com.tchalanet.server.features.pagemodel.dynamic.providers.publicdrawresults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.api.query.ListPublicDrawResultSlotsQuery;
import com.tchalanet.server.core.drawresult.api.query.SearchPublicDrawResultsQuery;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultHistoryRowView;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PublicDrawResultsPayloadAssemblerTest {

  private final QueryBus queryBus = mock(QueryBus.class);
  private final PublicDrawResultsPayloadAssembler assembler =
      new PublicDrawResultsPayloadAssembler(queryBus);

  @Nested
  @DisplayName("grouped reads")
  class GroupedReads {

    @Test
    @DisplayName("home spec (includeHistory=false) does only the slots query")
    void homeSpecSkipsHistory() {
      when(queryBus.ask(any(ListPublicDrawResultSlotsQuery.class))).thenReturn(List.of());

      var spec = new PublicDrawResultsPayloadAssembler.Spec(
          List.of("haiti.midi"), "haiti", false, 0);
      var payload = assembler.assemble(spec);

      assertThat(payload.history()).isEmpty();
      verify(queryBus, times(1)).ask(any(ListPublicDrawResultSlotsQuery.class));
      verify(queryBus, never()).ask(any(SearchPublicDrawResultsQuery.class));
    }

    @Test
    @DisplayName("page spec (includeHistory=true) issues exactly one history query")
    void pageSpecLoadsHistory() {
      when(queryBus.ask(any(ListPublicDrawResultSlotsQuery.class))).thenReturn(List.of());
      when(queryBus.ask(any(SearchPublicDrawResultsQuery.class)))
          .thenReturn(emptyHistoryPage());

      var spec = new PublicDrawResultsPayloadAssembler.Spec(
          List.of("haiti.midi"), "haiti", true, 10);
      assembler.assemble(spec);

      verify(queryBus, times(1)).ask(any(ListPublicDrawResultSlotsQuery.class));
      verify(queryBus, times(1)).ask(any(SearchPublicDrawResultsQuery.class));
    }

    @Test
    @DisplayName("history limit is clamped between 1 and 100")
    void clampsLimit() {
      when(queryBus.ask(any(ListPublicDrawResultSlotsQuery.class))).thenReturn(List.of());
      when(queryBus.ask(any(SearchPublicDrawResultsQuery.class)))
          .thenReturn(emptyHistoryPage());

      var captor = ArgumentCaptor.forClass(SearchPublicDrawResultsQuery.class);

      assembler.assemble(new PublicDrawResultsPayloadAssembler.Spec(
          List.of(), null, true, -5));
      assembler.assemble(new PublicDrawResultsPayloadAssembler.Spec(
          List.of(), null, true, 999));

      verify(queryBus, times(2)).ask(captor.capture());
      assertThat(captor.getAllValues().get(0).pageable().getPageSize()).isEqualTo(1);
      assertThat(captor.getAllValues().get(1).pageable().getPageSize()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("Spec.memoKey")
  class MemoKey {

    @Test
    @DisplayName("differs when include_history changes")
    void differsOnHistory() {
      var a = new PublicDrawResultsPayloadAssembler.Spec(List.of("k"), "p", false, 10);
      var b = new PublicDrawResultsPayloadAssembler.Spec(List.of("k"), "p", true, 10);
      assertThat(a.memoKey()).isNotEqualTo(b.memoKey());
    }

    @Test
    @DisplayName("equal when all fields equal")
    void equalForSameSpec() {
      var a = new PublicDrawResultsPayloadAssembler.Spec(List.of("k"), "p", true, 10);
      var b = new PublicDrawResultsPayloadAssembler.Spec(List.of("k"), "p", true, 10);
      assertThat(a.memoKey()).isEqualTo(b.memoKey());
    }
  }

  @Nested
  @DisplayName("slots payload")
  class Slots {

    @Test
    @DisplayName("null slots query result is converted to empty list")
    void nullSlotsHandled() {
      when(queryBus.ask(any(ListPublicDrawResultSlotsQuery.class))).thenReturn(null);

      var payload = assembler.assemble(new PublicDrawResultsPayloadAssembler.Spec(
          List.of(), null, false, 0));

      assertThat(payload.slots()).isEmpty();
    }
  }

  @SuppressWarnings("unchecked")
  private static TchPage<PublicDrawResultHistoryRowView> emptyHistoryPage() {
    return new TchPage<>(List.of(), 0, 10, 0L, 0, true, false, false);
  }
}
