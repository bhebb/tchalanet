package com.tchalanet.server.core.limitpolicy.internal.infra.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.platform.archive.api.model.ArchiveDatasetKey;
import com.tchalanet.server.platform.archive.api.model.ArchiveExportRequest;
import com.tchalanet.server.platform.archive.api.model.ArchivePeriod;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class DrawExposureArchiveDatasetProviderTest {

  private final DrawExposureArchiveJdbcRepository repo =
      mock(DrawExposureArchiveJdbcRepository.class);
  private final DrawExposureArchiveDatasetProvider provider =
      new DrawExposureArchiveDatasetProvider(repo);

  private static final UUID TENANT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final ArchivePeriod PERIOD = ArchivePeriod.forMonth(2026, 7);

  @Test
  void planCountsRowsByArchivedDraw() {
    when(repo.countByArchivedDraw(any(Instant.class), eq(TENANT_ID))).thenReturn(42L);

    var plan = provider.plan(PERIOD, TENANT_ID);

    assertThat(plan.estimatedRowCount()).isEqualTo(42L);
    assertThat(plan.archivalNeeded()).isTrue();
  }

  @Test
  void exportStreamsAndHardDeletes() {
    var capturedRows = new ArrayList<Map<String, Object>>();
    Map<String, Object> fakeRow = Map.of("draw_id", UUID.randomUUID(), "selection_key", "34");

    doAnswer(
            invocation -> {
              Consumer<Map<String, Object>> rowSink = invocation.getArgument(2);
              rowSink.accept(fakeRow);
              return 1L;
            })
        .when(repo)
        .streamAndDelete(any(Instant.class), eq(TENANT_ID), any());

    var request =
        new ArchiveExportRequest(
            UUID.randomUUID(),
            ArchiveDatasetKey.of("draw_exposure", "Draw Exposure"),
            PERIOD,
            TENANT_ID,
            0,
            capturedRows::add);

    var result = provider.export(request);

    assertThat(result.rowsExported()).isEqualTo(1L);
    assertThat(capturedRows).hasSize(1);
    assertThat(capturedRows.get(0)).containsKey("selection_key");
    // verify hard-delete was executed (streamAndDelete is called, not a separate softDelete)
    verify(repo).streamAndDelete(any(Instant.class), eq(TENANT_ID), any());
  }

  @Test
  void generateLookupRowsReturnsEmpty() {
    var result = provider.generateLookupRows(PERIOD, TENANT_ID, UUID.randomUUID());
    assertThat(result).isEmpty();
  }

  @Test
  void keyIsStable() {
    assertThat(provider.key().tableName()).isEqualTo("draw_exposure");
  }
}
