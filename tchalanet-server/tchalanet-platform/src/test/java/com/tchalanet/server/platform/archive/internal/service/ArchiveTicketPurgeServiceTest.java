package com.tchalanet.server.platform.archive.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.platform.archive.internal.config.ArchiveProperties;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLegalHoldJdbcRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@DisplayName("ArchiveTicketPurgeService")
class ArchiveTicketPurgeServiceTest {

  private static final LocalDate START = LocalDate.of(2025, 1, 1);
  private static final LocalDate END = LocalDate.of(2025, 2, 1);

  @Test
  @DisplayName("refuses purge when a legal hold overlaps the ticket period")
  void refusesPurgeWhenLegalHoldOverlapsPeriod() {
    var jdbc = mockJdbcWithMatchingCounts();
    var legalHoldRepo = mock(ArchiveLegalHoldJdbcRepository.class);
    when(legalHoldRepo.hasActiveHoldForPeriod("sales_ticket_line", START, END)).thenReturn(true);

    var service = service(jdbc, legalHoldRepo, true);

    assertThatThrownBy(() -> service.purge(null, START, END, 100,
        ArchiveTicketPurgeService.TicketPurgeMode.DELETE,
        UUID.randomUUID(), "legal hold safety test"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("legal hold");

    verify(jdbc, never()).update(any(String.class), any(MapSqlParameterSource.class));
  }

  @Test
  @DisplayName("dry-run returns eligible plan and does not delete rows")
  void dryRunReturnsEligiblePlanAndDoesNotDeleteRows() {
    var jdbc = mockJdbcWithMatchingCounts();
    var legalHoldRepo = mock(ArchiveLegalHoldJdbcRepository.class);
    var service = service(jdbc, legalHoldRepo, true);

    var result = service.purge(null, START, END, 100,
        ArchiveTicketPurgeService.TicketPurgeMode.DRY_RUN,
        UUID.randomUUID(), "dry-run ticket purge test");

    assertThat(result.plan().eligible()).isTrue();
    assertThat(result.plan().hotTickets()).isEqualTo(3);
    assertThat(result.plan().hotLines()).isEqualTo(6);
    assertThat(result.plan().hotCharges()).isEqualTo(3);
    assertThat(result.deletedTickets()).isZero();
    verify(jdbc, never()).update(any(String.class), any(MapSqlParameterSource.class));
  }

  @Test
  @DisplayName("delete mode removes charges then lines then tickets")
  void deleteModeRemovesChildrenBeforeTickets() {
    var jdbc = mockJdbcWithMatchingCounts();
    var legalHoldRepo = mock(ArchiveLegalHoldJdbcRepository.class);
    var service = service(jdbc, legalHoldRepo, true);

    when(jdbc.update(argThat(sqlContains("DELETE FROM sales_ticket_charge")), any(MapSqlParameterSource.class)))
        .thenReturn(3, 0);
    when(jdbc.update(argThat(sqlContains("DELETE FROM sales_ticket_line")), any(MapSqlParameterSource.class)))
        .thenReturn(6, 0);
    when(jdbc.update(argThat(sqlContains("DELETE FROM sales_ticket t")), any(MapSqlParameterSource.class)))
        .thenReturn(3, 0);

    var result = service.purge(null, START, END, 100,
        ArchiveTicketPurgeService.TicketPurgeMode.DELETE,
        UUID.randomUUID(), "execute ticket purge test");

    assertThat(result.deletedCharges()).isEqualTo(3);
    assertThat(result.deletedLines()).isEqualTo(6);
    assertThat(result.deletedTickets()).isEqualTo(3);

    var order = inOrder(jdbc);
    order.verify(jdbc).update(argThat(sqlContains("DELETE FROM sales_ticket_charge")), any(MapSqlParameterSource.class));
    order.verify(jdbc).update(argThat(sqlContains("DELETE FROM sales_ticket_line")), any(MapSqlParameterSource.class));
    order.verify(jdbc).update(argThat(sqlContains("DELETE FROM sales_ticket t")), any(MapSqlParameterSource.class));
  }

  @Test
  @DisplayName("delete mode is refused when archive cleanup is disabled")
  void deleteModeIsRefusedWhenCleanupDisabled() {
    var jdbc = mockJdbcWithMatchingCounts();
    var legalHoldRepo = mock(ArchiveLegalHoldJdbcRepository.class);
    var service = service(jdbc, legalHoldRepo, false);

    assertThatThrownBy(() -> service.purge(null, START, END, 100,
        ArchiveTicketPurgeService.TicketPurgeMode.DELETE,
        UUID.randomUUID(), "cleanup disabled test"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("enabled is false");
  }

  private static NamedParameterJdbcTemplate mockJdbcWithMatchingCounts() {
    var jdbc = mock(NamedParameterJdbcTemplate.class);

    when(jdbc.queryForObject(argThat(sqlContains("FROM sales_ticket t")),
        any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(3L);
    when(jdbc.queryForObject(argThat(sqlContains("FROM sales_ticket_line tl")),
        any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(6L);
    when(jdbc.queryForObject(argThat(sqlContains("FROM sales_ticket_charge c")),
        any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(3L);

    when(jdbc.queryForObject(argThat(sqlContains("COALESCE(SUM(row_count), 0)")),
        argThat(paramsWithTable("sales_ticket")), eq(Long.class))).thenReturn(3L);
    when(jdbc.queryForObject(argThat(sqlContains("COALESCE(SUM(row_count), 0)")),
        argThat(paramsWithTable("sales_ticket_line")), eq(Long.class))).thenReturn(6L);
    when(jdbc.queryForObject(argThat(sqlContains("COALESCE(SUM(row_count), 0)")),
        argThat(paramsWithTable("sales_ticket_charge")), eq(Long.class))).thenReturn(3L);

    when(jdbc.queryForObject(argThat(sqlContains("COUNT(*)")),
        argThat(paramsWithTable("sales_ticket")), eq(Long.class))).thenReturn(1L);
    when(jdbc.queryForObject(argThat(sqlContains("COUNT(*)")),
        argThat(paramsWithTable("sales_ticket_line")), eq(Long.class))).thenReturn(1L);
    when(jdbc.queryForObject(argThat(sqlContains("COUNT(*)")),
        argThat(paramsWithTable("sales_ticket_charge")), eq(Long.class))).thenReturn(1L);
    when(jdbc.queryForObject(argThat(sqlContains("table_name IN")),
        any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(0L);

    return jdbc;
  }

  private static ArchiveTicketPurgeService service(NamedParameterJdbcTemplate jdbc,
      ArchiveLegalHoldJdbcRepository legalHoldRepo, boolean cleanupEnabled) {
    var props = new ArchiveProperties(
        true,
        new ArchiveProperties.Storage("local", "./archive-data", "tchalanet-archive", "archive", 536870912L),
        new ArchiveProperties.Restore(Duration.ofDays(7), 1_000_000L, 5),
        new ArchiveProperties.Cleanup(cleanupEnabled, "DRY_RUN", 12, List.of("audit_log")));
    return new ArchiveTicketPurgeService(props, jdbc, legalHoldRepo);
  }

  private static ArgumentMatcher<String> sqlContains(String text) {
    return sql -> sql != null && sql.contains(text);
  }

  private static ArgumentMatcher<MapSqlParameterSource> paramsWithTable(String table) {
    return params -> params != null && params.hasValue("table") && table.equals(params.getValue("table"));
  }
}
