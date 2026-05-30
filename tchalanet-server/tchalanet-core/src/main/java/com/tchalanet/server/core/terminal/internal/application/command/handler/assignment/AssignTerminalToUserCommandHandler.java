package com.tchalanet.server.core.terminal.internal.application.command.handler.assignment;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TerminalAssignmentId;
import com.tchalanet.server.core.terminal.api.command.AssignTerminalToUserCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.assignment.TerminalAssignmentReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.assignment.TerminalAssignmentWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalAssignedToUserEvent;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.internal.domain.model.assignment.TerminalAssignment;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AssignTerminalToUserCommandHandler
    implements VoidCommandHandler<AssignTerminalToUserCommand> {

  private final TerminalReaderPort reader;
  private final TerminalWriterPort writer;
  private final TerminalAssignmentReaderPort assignmentReader;
  private final TerminalAssignmentWriterPort assignmentWriter;
  private final DomainEventPublisher publisher;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(AssignTerminalToUserCommand cmd) {
    Terminal t = reader.getRequired(cmd.tenantId(), cmd.terminalId());
    Terminal updated = t.assignToUser(cmd.userId());
    boolean terminalChanged = updated != t;
    if (terminalChanged) {
      writer.save(updated);
    }

    // Keep the terminal_assignment ledger consistent with terminal.assigned_user_id.
    // Operation validation (ValidateTerminalForOperationQueryHandler) requires an ACTIVE
    // terminal_assignment row for the actor; without this the assigned user could never
    // operate the terminal ("terminal.assignment_missing"). Idempotent: only acts when the
    // user has no active assignment yet.
    boolean assignmentCreated = false;
    if (assignmentReader.findActive(cmd.tenantId(), cmd.terminalId(), cmd.userId()).isEmpty()) {
      Instant now = Instant.now(clock);
      assignmentReader.findActiveAssignmentByTerminal(cmd.tenantId(), cmd.terminalId())
          .ifPresent(existing -> assignmentWriter.save(existing.revoke(now)));
      assignmentWriter.save(TerminalAssignment.active(
          TerminalAssignmentId.of(idGenerator.newUuid()),
          cmd.tenantId(),
          cmd.terminalId(),
          cmd.userId(),
          now));
      assignmentCreated = true;
    }

    if (!terminalChanged && !assignmentCreated) return; // fully idempotent

    TerminalAssignedToUserEvent event =
        new TerminalAssignedToUserEvent(
            EventId.of(idGenerator.newUuid()),
            Instant.now(clock),
            cmd.tenantId(),
            cmd.terminalId(),
            cmd.userId(),
            cmd.actorUserId());
    AfterCommit.run(() -> publisher.publish(event));
  }
}
