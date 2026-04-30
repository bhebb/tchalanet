package com.tchalanet.server.core.audit.infra.persistence.envers;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.enums.AuditActorType;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TchRevisionListenerTest {

  private final TchRevisionListener listener = new TchRevisionListener(new TchContextResolver());

  @AfterEach
  void clearContext() {
    TchContext.clear();
  }

  @Test
  void revisionIsEnrichedFromCurrentContext() {
    var tenantId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    TchRequestContext ctx = TchRequestContext.startupTenant(tenantId, "req-revision-1")
        .withAppUserId(userId);
    TchContext.set(ctx);

    var revision = new TchRevisionEntity();
    listener.newRevision(revision);

    assertThat(revision.getTenantId()).isEqualTo(tenantId);
    assertThat(revision.getUserId()).isEqualTo(userId);
    assertThat(revision.getRequestId()).isEqualTo("req-revision-1");
    assertThat(revision.getActorType()).isEqualTo(AuditActorType.USER.name());
    assertThat(revision.getApiScope()).isEqualTo("TENANT");
    assertThat(revision.isTenantOverridden()).isFalse();
  }

  @Test
  void revisionWithoutContextDoesNotThrowAndUsesSystemActor() {
    var revision = new TchRevisionEntity();

    listener.newRevision(revision);

    assertThat(revision.getTenantId()).isNull();
    assertThat(revision.getUserId()).isNull();
    assertThat(revision.getRequestId()).isNull();
    assertThat(revision.getActorType()).isEqualTo(AuditActorType.SYSTEM.name());
    assertThat(revision.getApiScope()).isNull();
    assertThat(revision.isTenantOverridden()).isFalse();
  }
}
