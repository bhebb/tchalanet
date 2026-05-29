package com.tchalanet.server.core.terminal.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalDeviceBinding;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalDeviceBindingTest {

  @Test
  void posDeviceBindingIsOnlyCompatibleWithPhysicalPos() {
    var binding = binding(TerminalBindingType.POS_DEVICE, null);

    assertThat(binding.compatibleWith(TerminalKind.PHYSICAL, TerminalSurface.POS)).isTrue();
    assertThat(binding.compatibleWith(TerminalKind.VIRTUAL, TerminalSurface.MOBILE)).isFalse();
    assertThat(binding.compatibleWith(TerminalKind.VIRTUAL, TerminalSurface.WEB)).isFalse();
  }

  @Test
  void mobileAppBindingIsOnlyCompatibleWithVirtualMobile() {
    var binding = binding(TerminalBindingType.MOBILE_APP, null);

    assertThat(binding.compatibleWith(TerminalKind.VIRTUAL, TerminalSurface.MOBILE)).isTrue();
    assertThat(binding.compatibleWith(TerminalKind.PHYSICAL, TerminalSurface.POS)).isFalse();
  }

  @Test
  void adminSelectionBindingIsOnlyCompatibleWithVirtualWebOrBackOffice() {
    var binding = binding(TerminalBindingType.ADMIN_SELECTION, null);

    assertThat(binding.compatibleWith(TerminalKind.VIRTUAL, TerminalSurface.WEB)).isTrue();
    assertThat(binding.compatibleWith(TerminalKind.VIRTUAL, TerminalSurface.BACK_OFFICE)).isTrue();
    assertThat(binding.compatibleWith(TerminalKind.PHYSICAL, TerminalSurface.POS)).isFalse();
  }

  @Test
  void bindingExpiresWhenPastExpiry() {
    var binding = binding(
        TerminalBindingType.POS_DEVICE,
        Instant.parse("2026-05-26T10:05:00Z")
    );

    assertThat(binding.activeAt(Instant.parse("2026-05-26T10:04:59Z"))).isTrue();

    var expired = binding.expireIfDue(Instant.parse("2026-05-26T10:05:00Z"));

    assertThat(expired.status()).isEqualTo(TerminalBindingStatus.EXPIRED);
    assertThat(expired.activeAt(Instant.parse("2026-05-26T10:05:00Z"))).isFalse();
  }

  private static TerminalDeviceBinding binding(TerminalBindingType type, Instant expiresAt) {
    return TerminalDeviceBinding.active(
        TerminalBindingId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")),
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        type,
        "public-key",
        "secret-hash",
        "fingerprint-hash",
        Instant.parse("2026-05-26T10:00:00Z"),
        expiresAt
    );
  }
}
