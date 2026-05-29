package com.tchalanet.server.core.terminal.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalAssignmentId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignment;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignmentStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalAssignmentTest {

  @Test
  void activeAssignmentOnlyMatchesAssignedUser() {
    var userId = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    var assignment = activeAssignment(userId);

    assertThat(assignment.activeFor(userId)).isTrue();
    assertThat(assignment.activeFor(UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000004"))))
        .isFalse();
  }

  @Test
  void revokedAssignmentDoesNotMatchAssignedUser() {
    var userId = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000003"));
    var assignment = activeAssignment(userId).revoke(Instant.parse("2026-05-26T11:00:00Z"));

    assertThat(assignment.status()).isEqualTo(TerminalAssignmentStatus.REVOKED);
    assertThat(assignment.activeFor(userId)).isFalse();
  }

  private static TerminalAssignment activeAssignment(UserId userId) {
    return TerminalAssignment.active(
        TerminalAssignmentId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")),
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        TerminalId.of(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        userId,
        Instant.parse("2026-05-26T10:00:00Z")
    );
  }
}
