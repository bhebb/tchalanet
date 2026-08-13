package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.exposure;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.limitpolicy.api.ScopeType;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScopePersistenceMapperTest {

  private static final UUID ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

  @Test
  void tenant_scope_maps_to_tenant_row() {
    var row = ScopePersistenceMapper.toRow(LimitScopeRef.tenant(TenantId.of(ID)));
    assertThat(row.scopeType()).isEqualTo(ScopeType.TENANT);
    assertThat(row.scopeId()).isEqualTo(ID);
  }

  @Test
  void draw_channel_scope_maps_to_draw_channel_row() {
    var row = ScopePersistenceMapper.toRow(LimitScopeRef.drawChannel(DrawChannelId.of(ID)));
    assertThat(row.scopeType()).isEqualTo(ScopeType.DRAW_CHANNEL);
    assertThat(row.scopeId()).isEqualTo(ID);
  }

  @Test
  void seller_terminal_scope_maps_to_seller_terminal_row() {
    var row = ScopePersistenceMapper.toRow(LimitScopeRef.sellerTerminal(SellerTerminalId.of(ID)));
    assertThat(row.scopeType()).isEqualTo(ScopeType.SELLER_TERMINAL);
    assertThat(row.scopeId()).isEqualTo(ID);
  }

  @Test
  void agent_scope_maps_to_agent_row() {
    var row = ScopePersistenceMapper.toRow(LimitScopeRef.agent(UserId.of(ID)));
    assertThat(row.scopeType()).isEqualTo(ScopeType.AGENT);
    assertThat(row.scopeId()).isEqualTo(ID);
  }
}
