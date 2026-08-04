package com.tchalanet.server.platform.idempotence.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class ProcessedEventCleanupServiceTest {

  @Mock private JdbcTemplate jdbc;
  @Mock private TenantPreContextLookupApi tenantRegistry;

  @Test
  @DisplayName("should delete rows older than retention")
  void shouldDeleteRowsOlderThanRetention() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-03T12:00:00Z"), ZoneOffset.UTC);
    ProcessedEventCleanupService service =
        new ProcessedEventCleanupService(jdbc, clock, Duration.ofDays(7), tenantRegistry);
    when(tenantRegistry.listActiveTenantIds())
        .thenReturn(List.of(TenantId.of(UUID.randomUUID()), TenantId.of(UUID.randomUUID())));
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(3);

    int deleted = service.cleanupExpired();

    assertThat(deleted).isEqualTo(6);
    ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
    verify(jdbc, times(2)).update(anyString(), args.capture());
    assertThat(args.getAllValues())
        .allSatisfy(
            values -> assertThat(values).containsExactly(Instant.parse("2026-07-27T12:00:00Z")));
  }
}
