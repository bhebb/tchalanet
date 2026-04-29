package com.tchalanet.server.features.pagemodel.dynamic.providers;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc;
import com.tchalanet.server.features.publicdraw.app.PublicLatestDrawResultsService;
import com.tchalanet.server.features.publicdraw.model.PublicLatestDrawResultsResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DrawsProviderTest {

  private final CapturingLatestDrawResultsService latestService =
      new CapturingLatestDrawResultsService();
  private final DrawsProvider drawsProvider = new DrawsProvider(latestService);

  @Test
  void should_use_default_limit_when_no_config() {
    // When
    drawsProvider.load(null, "widget-1", null, "fr", null);

    // Then
    assertThat(latestService.lastLimit()).isEqualTo(1);
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
    assertThat(latestService.lastLimit()).isEqualTo(3);
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
    assertThat(latestService.lastLimit()).isEqualTo(4);
  }

  @Test
  @SuppressWarnings("unchecked")
  void should_return_empty_list_on_error() {
    // Given
    latestService.failWith(new StacklessRuntimeException("Service error"));

    // When
    Object result = drawsProvider.load(null, "widget-1", null, "fr", null);

    // Then
    assertThat(result).isInstanceOf(Map.class);
    Map<String, Object> map = (Map<String, Object>) result;
    assertThat(map).containsKey("draws");
    assertThat((List<?>) map.get("draws")).isEmpty();
  }

  private static final class CapturingLatestDrawResultsService extends PublicLatestDrawResultsService {
    private Integer lastLimit;
    private RuntimeException failure;

    private CapturingLatestDrawResultsService() {
      super(null, null, null, null);
    }

    @Override
    public List<PublicLatestDrawResultsResponse> latest(int limitPerSlot) {
      this.lastLimit = limitPerSlot;
      if (failure != null) {
        throw failure;
      }
      return List.of();
    }

    private Integer lastLimit() {
      return lastLimit;
    }

    private void failWith(RuntimeException failure) {
      this.failure = failure;
    }
  }

  private static final class StacklessRuntimeException extends RuntimeException {
    private StacklessRuntimeException(String message) {
      super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  }
}
