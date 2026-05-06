package com.tchalanet.server.features.pagemodel.dynamic.providers;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.drawresult.application.query.model.ListPublicDrawResultSlotsQuery;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PublicDrawResultsProviderTest {

  @Test
  void loadsPublicSlotsWithoutHistory() {
    var bus = new RecordingQueryBus();
    var provider = new PublicDrawResultsProvider(bus);

    provider.load(null, "home.draws", widget(), "fr", null);

    assertThat(bus.query).isInstanceOf(ListPublicDrawResultSlotsQuery.class);
    var query = (ListPublicDrawResultSlotsQuery) bus.query;
    assertThat(query.slotKeys()).containsExactly("ny_mid", "FL_EVE");
    assertThat(query.provider()).isEqualTo("ny");
    assertThat(query.includeHistory()).isFalse();
    assertThat(query.historyLimit()).isZero();
  }

  private static PageModelDoc.WidgetConfig widget() {
    return new PageModelDoc.WidgetConfig(
        "PublicDrawResultsWidget",
        new PageModelDoc.WidgetBinding("dynamic", "public_draw_results"),
        Map.of(
            "slot_keys", List.of("ny_mid", "FL_EVE"),
            "provider", "ny"));
  }

  private static final class RecordingQueryBus implements QueryBus {
    private Query<?> query;

    @SuppressWarnings("unchecked")
    @Override
    public <R> R send(Query<R> query) {
      this.query = query;
      return (R) List.of();
    }
  }
}
