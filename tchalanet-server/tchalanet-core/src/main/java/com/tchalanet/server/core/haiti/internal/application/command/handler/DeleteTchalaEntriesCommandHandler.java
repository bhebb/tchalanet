package com.tchalanet.server.core.haiti.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.api.command.DeleteTchalaEntriesCommand;
import com.tchalanet.server.core.haiti.internal.application.port.out.TchalaEntryRepositoryPort;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Handler admin pour supprimer en dur une liste d'entrées Tchala. */
@Component
@RequiredArgsConstructor
public class DeleteTchalaEntriesCommandHandler
    implements VoidCommandHandler<DeleteTchalaEntriesCommand> {

  private final TchalaEntryRepositoryPort repo;

  @Override
  @TchTx
  public void handle(DeleteTchalaEntriesCommand command) {
    Objects.requireNonNull(command, "command");
    var entryIds = command.entryIds();
    if (entryIds == null || entryIds.isEmpty()) return;

    List<TchalaEntryId> ids =
        entryIds.stream()
            .filter(Objects::nonNull)
            .map(TchalaEntryId::of)
            .collect(Collectors.toList());

    if (ids.isEmpty()) return;

    repo.deleteAllByIds(ids);
  }
}
