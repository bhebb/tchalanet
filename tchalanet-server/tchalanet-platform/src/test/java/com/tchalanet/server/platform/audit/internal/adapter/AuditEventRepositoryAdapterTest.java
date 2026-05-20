package com.tchalanet.server.platform.audit.internal.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditActorType;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.audit.internal.persistence.AuditEventJpaEntity;
import com.tchalanet.server.platform.audit.internal.persistence.AuditEventJpaRepository;
import com.tchalanet.server.platform.audit.internal.service.AuditEvent;
import java.net.InetAddress;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("AuditEventRepositoryAdapter")
class AuditEventRepositoryAdapterTest {

  private static final UUID TENANT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ACTOR_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final String IP = "203.0.113.42";
  private static final String DETAILS_JSON = "{\"reason\":\"audit regression\"}";

  private AuditEventJpaRepository jpa;
  private AuditEventRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    jpa = mock(AuditEventJpaRepository.class);
    adapter = new AuditEventRepositoryAdapter(jpa, new JsonUtils(JsonMapper.builder().build()));
  }

  @Nested
  @DisplayName("When saving audit events")
  class WhenSavingAuditEvents {

    @Test
    @DisplayName("should bind ip as InetAddress and restore it when persisting an audit event")
    void shouldBindIpAsInetAddressAndRestoreItWhenPersistingAnAuditEvent() {
      when(jpa.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      var event = AuditEvent.of(
          TenantId.of(TENANT_UUID),
          null,
          AuditActorType.USER,
          ACTOR_UUID,
          AuditEntityType.SYSTEM,
          "entity-1",
          AuditAction.UPDATE,
          DETAILS_JSON,
          IP,
          "JUnit/1.0");

      var saved = adapter.save(event);

      var captor = ArgumentCaptor.forClass(AuditEventJpaEntity.class);
      verify(jpa).save(captor.capture());

      assertThat(captor.getValue().getIp())
          .isInstanceOf(InetAddress.class)
          .extracting(InetAddress::getHostAddress)
          .isEqualTo(IP);
      assertThat(saved.ip()).isEqualTo(IP);
    }
  }
}


