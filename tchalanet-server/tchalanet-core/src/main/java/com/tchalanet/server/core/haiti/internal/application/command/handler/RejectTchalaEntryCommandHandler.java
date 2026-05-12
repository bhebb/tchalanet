package com.tchalanet.server.core.haiti.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.application.command.model.RejectTchalaEntryCommand;
import com.tchalanet.server.core.haiti.application.port.out.TchalaEntryRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reject a pending Tchala entry (ops / moderator). */
@Component
@RequiredArgsConstructor
public class RejectTchalaEntryCommandHandler
    implements VoidCommandHandler<RejectTchalaEntryCommand> {

  private final TchalaEntryRepositoryPort repo;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(RejectTchalaEntryCommand command) {
    Objects.requireNonNull(command);
    var id = TchalaEntryId.of(command.entryId());
    var entry =
        repo.findById(id).orElseThrow(() -> new IllegalArgumentException("entry not found"));

    var now = Instant.now(clock);
    var rejected = entry.reject(command.reason(), now);
    repo.save(rejected);
  }
}
