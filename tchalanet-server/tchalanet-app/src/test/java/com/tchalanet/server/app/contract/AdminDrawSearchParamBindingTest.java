package com.tchalanet.server.app.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.GlobalErrorHandler;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPagingArgumentResolver;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.ListDrawsQuery;
import com.tchalanet.server.core.draw.internal.infra.web.DrawQueryAdminController;
import com.tchalanet.server.core.draw.internal.infra.web.mapper.DrawAdminWebMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * `scheduledBefore` is what makes the "Tirages passés" filter correct: the business-date window
 * (from/to) cannot express "already drawn" for a draw happening later today. These tests pin the
 * query-param binding so the filter cannot silently degrade back to a whole-day match.
 */
class AdminDrawSearchParamBindingTest {

  @Test
  void scheduledBeforeIsBoundAsAnInstantAndReachesTheQuery() throws Exception {
    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any())).thenReturn(TchPage.of(List.of(), 0, 20, 0L, 0, true, false, false));

    mvc(queryBus)
        .perform(
            get("/admin/draws")
                .param("from", "2026-07-29")
                .param("to", "2026-07-30")
                .param("scheduledBefore", "2026-07-30T14:00:00Z"))
        .andExpect(status().isOk());

    var criteria = capturedCriteria(queryBus);
    assertThat(criteria.scheduledBefore()).isEqualTo(Instant.parse("2026-07-30T14:00:00Z"));
    assertThat(criteria.from()).isEqualTo(LocalDate.of(2026, 7, 29));
    assertThat(criteria.to()).isEqualTo(LocalDate.of(2026, 7, 30));
    assertThat(criteria.status()).isNull();
  }

  @Test
  void statusAndScheduledBoundsAreIndependent() throws Exception {
    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any())).thenReturn(TchPage.of(List.of(), 0, 20, 0L, 0, true, false, false));

    mvc(queryBus)
        .perform(
            get("/admin/draws")
                .param("status", "OPEN")
                .param("scheduledAfter", "2026-07-30T14:00:00Z"))
        .andExpect(status().isOk());

    var criteria = capturedCriteria(queryBus);
    assertThat(criteria.status()).isEqualTo(DrawStatus.OPEN);
    assertThat(criteria.scheduledAfter()).isEqualTo(Instant.parse("2026-07-30T14:00:00Z"));
    assertThat(criteria.scheduledBefore()).isNull();
  }

  @Test
  void omittingTheScheduledBoundsLeavesThemNull() throws Exception {
    var queryBus = mock(QueryBus.class);
    when(queryBus.ask(any())).thenReturn(TchPage.of(List.of(), 0, 20, 0L, 0, true, false, false));

    mvc(queryBus).perform(get("/admin/draws")).andExpect(status().isOk());

    var criteria = capturedCriteria(queryBus);
    assertThat(criteria.scheduledBefore()).isNull();
    assertThat(criteria.scheduledAfter()).isNull();
  }

  private static com.tchalanet.server.core.draw.api.query.DrawSearchCriteria capturedCriteria(
      QueryBus queryBus) {
    var captor = ArgumentCaptor.forClass(ListDrawsQuery.class);
    org.mockito.Mockito.verify(queryBus).ask(captor.capture());
    return captor.getValue().criteria();
  }

  private static MockMvc mvc(QueryBus queryBus) {
    return MockMvcBuilders.standaloneSetup(
            new DrawQueryAdminController(
                queryBus, mock(DrawAdminWebMapper.class), Clock.systemUTC()))
        .setControllerAdvice(new GlobalErrorHandler())
        .setCustomArgumentResolvers(new TchPagingArgumentResolver())
        .build();
  }
}
