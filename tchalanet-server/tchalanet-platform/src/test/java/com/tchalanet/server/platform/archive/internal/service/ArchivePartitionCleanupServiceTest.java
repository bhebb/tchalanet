package com.tchalanet.server.platform.archive.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLegalHoldJdbcRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@DisplayName("ArchivePartitionCleanupService")
class ArchivePartitionCleanupServiceTest {

  @Test
  @DisplayName("marks partition ineligible when an active legal hold overlaps the period")
  void marksPartitionIneligibleWhenActiveLegalHoldOverlapsPeriod() {
    var legalHoldRepo = mock(ArchiveLegalHoldJdbcRepository.class);
    var service = service(legalHoldRepo);

    when(legalHoldRepo.hasActiveHoldForPeriod(
        "audit_log", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)))
        .thenReturn(true);

    var plan = service.plan("audit_log", LocalDate.of(2026, 1, 1));

    assertThat(plan)
        .singleElement()
        .satisfies(item -> {
          assertThat(item.partitionName()).isEqualTo("audit_log_2025_01");
          assertThat(item.eligible()).isFalse();
          assertThat(item.ineligibleReason()).contains("legal hold");
        });
  }

  @Test
  @DisplayName("refuses execute cleanup when legal hold blocks the current plan")
  void refusesExecuteCleanupWhenLegalHoldBlocksCurrentPlan() {
    var legalHoldRepo = mock(ArchiveLegalHoldJdbcRepository.class);
    var service = service(legalHoldRepo);

    when(legalHoldRepo.hasActiveHoldForPeriod(
        "audit_log", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1)))
        .thenReturn(true);

    assertThatThrownBy(() -> service.executeCleanup(
        "audit_log_2025_01", ArchivePartitionCleanupService.CleanupMode.DETACH_ONLY))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("legal hold");

    verify(legalHoldRepo).hasActiveHoldForPeriod(
        "audit_log", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1));
  }

  private static ArchivePartitionCleanupService service(ArchiveLegalHoldJdbcRepository legalHoldRepo) {
    var jdbc = mock(NamedParameterJdbcTemplate.class);
    when(jdbc.queryForList(contains("FROM pg_class"), any(MapSqlParameterSource.class)))
        .thenReturn(List.of(Map.of("partition_name", "audit_log_2025_01")));
    when(jdbc.queryForList(contains("FROM archive_object"), any(MapSqlParameterSource.class)))
        .thenReturn(List.of(Map.of("id", java.util.UUID.randomUUID(), "row_count", 2L)));
    when(jdbc.queryForObject(
        contains("COUNT(*) FROM archive_object"),
        any(MapSqlParameterSource.class),
        eq(Integer.class)))
        .thenReturn(0);
    when(jdbc.queryForObject(contains("COUNT(*) FROM audit_log_2025_01"), anyMap(), eq(Long.class)))
        .thenReturn(2L);

    var props = new ArchiveProperties(
        true,
        new ArchiveProperties.Storage("local", "./archive-data", "tchalanet-archive", "archive", 536870912L),
        new ArchiveProperties.Restore(Duration.ofDays(7), 1_000_000L, 5),
        new ArchiveProperties.Cleanup(true, "DETACH_ONLY", 12, List.of("audit_log")));
    return new ArchivePartitionCleanupService(props, jdbc, legalHoldRepo);
  }
}
