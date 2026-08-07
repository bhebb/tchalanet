package com.tchalanet.server.platform.idempotence.internal.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.constant.CommonConstants;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchContextScope;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ProcessedEventJdbcAdapterTest {

  @AfterEach
  void cleanup() {
    TchContext.clear();
  }

  @Test
  void globalContextMarksProcessedInsideDefaultTenantRlsScope() {
    var jdbc = mock(JdbcTemplate.class);
    var adapter = new ProcessedEventJdbcAdapter(jdbc, new TchContextResolver());
    var eventId = UUID.randomUUID();
    var tenantAtInsert = new AtomicReference<UUID>();

    when(jdbc.queryForObject(anyString(), eq(String.class), anyString())).thenReturn("");
    when(jdbc.update(anyString(), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              tenantAtInsert.set(TchContext.currentOrThrow().tenantUuid());
              return 1;
            });

    var inserted =
        TchContextScope.runPlatformSystemResult(
            "test-global-event",
            () -> adapter.markProcessedIfAbsent("drawresult.resolve_available", eventId));

    assertThat(inserted).isTrue();
    assertThat(tenantAtInsert.get()).isEqualTo(CommonConstants.DEFAULT_TENANT_UUID);
    var inOrder = inOrder(jdbc);
    inOrder
        .verify(jdbc)
        .queryForObject(
            anyString(), eq(String.class), eq(CommonConstants.DEFAULT_TENANT_UUID.toString()));
    inOrder.verify(jdbc).update(anyString(), any(Object[].class));
    inOrder.verify(jdbc).queryForObject(anyString(), eq(String.class), eq(""));
    verify(jdbc).update(anyString(), any(Object[].class));
  }
}
