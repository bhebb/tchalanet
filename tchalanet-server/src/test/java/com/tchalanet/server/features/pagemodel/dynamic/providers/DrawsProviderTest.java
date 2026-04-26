package com.tchalanet.server.features.pagemodel.dynamic.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.publicdraw.application.query.model.GetLatestPublicDrawResultsQuery;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DrawsProviderTest {

  @Mock
  private QueryBus queryBus;

  @InjectMocks
  private DrawsProvider drawsProvider;

  @Test
  void should_use_default_limit_when_no_config() {
    // When
    drawsProvider.load(null, "widget-1", null, "fr", null);

    // Then
    verify(queryBus).send(new GetLatestPublicDrawResultsQuery(1));
  }

  @Test
  void should_use_limit_per_slot_from_config() {
    // Given
    var config = new PageModelDoc.WidgetConfig("DrawsWidget",
        new PageModelDoc.WidgetBinding("dynamic", "draws"),
        Map.of("limit_per_slot", 3));

    // When
    drawsProvider.load(null, "widget-1", config, "fr", null);

    // Then
    verify(queryBus).send(new GetLatestPublicDrawResultsQuery(3));
  }

  @Test
  void should_use_backward_compat_max_items() {
    // Given
    var config = new PageModelDoc.WidgetConfig("DrawsWidget",
        new PageModelDoc.WidgetBinding("dynamic", "draws"),
        Map.of("max_items", 4));

    // When
    drawsProvider.load(null, "widget-1", config, "fr", null);

    // Then
    verify(queryBus).send(new GetLatestPublicDrawResultsQuery(4));
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_return_empty_list_on_error() {
    // Given
    when(queryBus.send(any())).thenThrow(new RuntimeException("Bus error"));

    // When
    Object result = drawsProvider.load(null, "widget-1", null, "fr", null);

    // Then
    assertThat(result).isInstanceOf(Map.class);
    Map<String, Object> map = (Map<String, Object>) result;
    assertThat(map).containsKey("draws");
    assertThat((List<?>) map.get("draws")).isEmpty();
  }
}
