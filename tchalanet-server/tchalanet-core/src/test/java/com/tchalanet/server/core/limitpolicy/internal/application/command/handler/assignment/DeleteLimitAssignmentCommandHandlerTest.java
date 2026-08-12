package com.tchalanet.server.core.limitpolicy.internal.application.command.handler.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tchalanet.server.common.types.id.LimitAssignmentId;
import com.tchalanet.server.core.limitpolicy.api.command.DeleteLimitAssignmentCommand;
import com.tchalanet.server.core.limitpolicy.internal.application.port.out.assignment.LimitAssignmentWriterPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeleteLimitAssignmentCommandHandlerTest {

  private static final Instant NOW = Instant.parse("2026-08-12T10:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

  private final LimitAssignmentWriterPort writer = mock(LimitAssignmentWriterPort.class);
  private final DeleteLimitAssignmentCommandHandler handler =
      new DeleteLimitAssignmentCommandHandler(writer, FIXED);

  @Test
  void delegates_soft_delete_with_current_clock_time() {
    var id = LimitAssignmentId.of(UUID.randomUUID());
    handler.handle(new DeleteLimitAssignmentCommand(id));
    verify(writer).softDelete(id, NOW);
  }

  @Test
  void returns_result_with_same_id() {
    var id = LimitAssignmentId.of(UUID.randomUUID());
    var result = handler.handle(new DeleteLimitAssignmentCommand(id));
    assertThat(result.id()).isEqualTo(id);
  }
}
