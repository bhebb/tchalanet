package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.assignment.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.limitpolicy.api.ScopeType;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LimitScopeMapperTest {

  private final LimitScopeMapper mapper = new LimitScopeMapper();

  private static final UUID ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

  // --- toType ---

  @Test
  void toType_tenant_scope_returns_tenant() {
    assertThat(mapper.toType(LimitScopeRef.tenant(TenantId.of(ID)))).isEqualTo(ScopeType.TENANT);
  }

  @Test
  void toType_draw_channel_scope_returns_draw_channel() {
    assertThat(mapper.toType(LimitScopeRef.drawChannel(DrawChannelId.of(ID)))).isEqualTo(ScopeType.DRAW_CHANNEL);
  }

  @Test
  void toType_seller_terminal_scope_returns_seller_terminal() {
    assertThat(mapper.toType(LimitScopeRef.sellerTerminal(SellerTerminalId.of(ID)))).isEqualTo(ScopeType.SELLER_TERMINAL);
  }

  @Test
  void toType_agent_scope_returns_agent() {
    assertThat(mapper.toType(LimitScopeRef.agent(UserId.of(ID)))).isEqualTo(ScopeType.AGENT);
  }

  // --- toId ---

  @Test
  void toId_extracts_uuid_from_tenant_scope() {
    assertThat(mapper.toId(LimitScopeRef.tenant(TenantId.of(ID)))).isEqualTo(ID);
  }

  @Test
  void toId_extracts_uuid_from_draw_channel_scope() {
    assertThat(mapper.toId(LimitScopeRef.drawChannel(DrawChannelId.of(ID)))).isEqualTo(ID);
  }

  @Test
  void toId_extracts_uuid_from_seller_terminal_scope() {
    assertThat(mapper.toId(LimitScopeRef.sellerTerminal(SellerTerminalId.of(ID)))).isEqualTo(ID);
  }

  @Test
  void toId_extracts_uuid_from_agent_scope() {
    assertThat(mapper.toId(LimitScopeRef.agent(UserId.of(ID)))).isEqualTo(ID);
  }

  // --- toDomain ---

  @Test
  void toDomain_tenant_produces_tenant_scope() {
    var result = mapper.toDomain(ScopeType.TENANT, ID);
    assertThat(result).isInstanceOf(LimitScopeRef.TenantScope.class);
    assertThat(((LimitScopeRef.TenantScope) result).tenantId().value()).isEqualTo(ID);
  }

  @Test
  void toDomain_draw_channel_produces_draw_channel_scope() {
    var result = mapper.toDomain(ScopeType.DRAW_CHANNEL, ID);
    assertThat(result).isInstanceOf(LimitScopeRef.DrawChannelScope.class);
    assertThat(((LimitScopeRef.DrawChannelScope) result).drawChannelId().value()).isEqualTo(ID);
  }

  @Test
  void toDomain_seller_terminal_produces_seller_terminal_scope() {
    var result = mapper.toDomain(ScopeType.SELLER_TERMINAL, ID);
    assertThat(result).isInstanceOf(LimitScopeRef.SellerTerminalScope.class);
    assertThat(((LimitScopeRef.SellerTerminalScope) result).sellerTerminalId().value()).isEqualTo(ID);
  }

  @Test
  void toDomain_agent_produces_agent_scope() {
    var result = mapper.toDomain(ScopeType.AGENT, ID);
    assertThat(result).isInstanceOf(LimitScopeRef.AgentScope.class);
    assertThat(((LimitScopeRef.AgentScope) result).userId().value()).isEqualTo(ID);
  }

  @Test
  void toDomain_null_type_throws() {
    assertThatThrownBy(() -> mapper.toDomain(null, ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("scopeType is required");
  }

  @Test
  void toDomain_null_id_throws() {
    assertThatThrownBy(() -> mapper.toDomain(ScopeType.TENANT, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("scopeId is required");
  }

  @Test
  void round_trip_tenant() {
    var original = LimitScopeRef.tenant(TenantId.of(ID));
    var result = mapper.toDomain(mapper.toType(original), mapper.toId(original));
    assertThat(result).isEqualTo(original);
  }

  @Test
  void round_trip_seller_terminal() {
    var original = LimitScopeRef.sellerTerminal(SellerTerminalId.of(ID));
    var result = mapper.toDomain(mapper.toType(original), mapper.toId(original));
    assertThat(result).isEqualTo(original);
  }
}
