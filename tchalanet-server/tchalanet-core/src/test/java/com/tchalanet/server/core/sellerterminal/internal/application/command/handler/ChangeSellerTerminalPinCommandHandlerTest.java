package com.tchalanet.server.core.sellerterminal.internal.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sellerterminal.api.command.ChangeSellerTerminalPinCommand;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalCommissionStatsView;
import com.tchalanet.server.core.sellerterminal.api.model.SellerTerminalSummaryRow;
import com.tchalanet.server.core.sellerterminal.api.query.SellerTerminalSearchCriteria;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.domain.model.SellerTerminal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChangeSellerTerminalPinCommandHandlerTest {

  private static final TenantId TENANT_ID =
      TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
  private static final SellerTerminalId TERMINAL_ID =
      SellerTerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));

  @Test
  void clearsTheMandatoryPinChangeGateAfterUpdatingTheExternalIdentity() {
    var terminal =
        SellerTerminal.createPending(
                TERMINAL_ID,
                TENANT_ID,
                "POS-001",
                "Terminal principal",
                "Terminal",
                "Principal",
                "terminal@example.test",
                null,
                null,
                BigDecimal.TEN)
            .activate(Instant.EPOCH)
            .resetPin(Instant.parse("2026-07-20T00:00:00Z"));
    var saved = new AtomicReference<SellerTerminal>();
    var identityPinChanged = new AtomicReference<Boolean>(false);

    new ChangeSellerTerminalPinCommandHandler(
            reader(terminal),
            savedTerminal -> {
              saved.set(savedTerminal);
              return savedTerminal;
            },
            identityProvision(identityPinChanged))
        .handle(new ChangeSellerTerminalPinCommand(TENANT_ID, TERMINAL_ID, "654321"));

    assertThat(identityPinChanged.get()).isTrue();
    assertThat(saved.get()).isNotNull();
    assertThat(saved.get().mustChangePin()).isFalse();
  }

  private static SellerTerminalReaderPort reader(SellerTerminal terminal) {
    return new SellerTerminalReaderPort() {
      @Override
      public Optional<SellerTerminal> findById(TenantId tenantId, SellerTerminalId terminalId) {
        return TENANT_ID.equals(tenantId) && TERMINAL_ID.equals(terminalId)
            ? Optional.of(terminal)
            : Optional.empty();
      }

      @Override
      public Optional<SellerTerminal> findByExternalSubject(
          String provider, String issuer, String externalSubject) {
        return Optional.empty();
      }

      @Override
      public TchPage<SellerTerminalSummaryRow> search(
          TenantId tenantId, SellerTerminalSearchCriteria criteria, TchPageRequest pageRequest) {
        throw new UnsupportedOperationException("not used");
      }

      @Override
      public List<String> terminalCodes(TenantId tenantId) {
        throw new UnsupportedOperationException("not used");
      }

      @Override
      public SellerTerminalCommissionStatsView commissionStats(
          TenantId tenantId, BigDecimal tenantDefaultRate) {
        throw new UnsupportedOperationException("not used");
      }
    };
  }

  private static SellerTerminalIdentityProvisionPort identityProvision(
      AtomicReference<Boolean> identityPinChanged) {
    return new SellerTerminalIdentityProvisionPort() {
      @Override
      public void provision(
          SellerTerminalId id,
          TenantId tenantId,
          String terminalCode,
          String displayName,
          String initialPin) {
        throw new UnsupportedOperationException("not used");
      }

      @Override
      public boolean hasExternalIdentity(SellerTerminalId id) {
        return true;
      }

      @Override
      public void resetPin(SellerTerminalId id, TenantId tenantId, String newPin) {
        assertThat(id).isEqualTo(TERMINAL_ID);
        assertThat(tenantId).isEqualTo(TENANT_ID);
        assertThat(newPin).isEqualTo("654321");
        identityPinChanged.set(true);
      }
    };
  }
}
