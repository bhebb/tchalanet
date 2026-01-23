package com.tchalanet.server.core.haiti.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.application.command.model.MergeTchalaEntriesCommand;
import com.tchalanet.server.core.haiti.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.domain.tchala.model.MergePolicy;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaNumber;
import com.tchalanet.server.core.haiti.domain.tchala.service.TchalaMerge;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Handler to merge one Tchala entry into another (superadmin operation). Behavior: merge numbers
 * according to policy, create/update the target canonical entry, mark the source as MERGED into the
 * target and persist changes.
 */
@Component
@RequiredArgsConstructor
public class MergeTchalaEntriesCommandHandler
    implements CommandHandler<MergeTchalaEntriesCommand, UUID> {

  private final TchalaEntryRepositoryPort repo;
  private final Clock clock;
  private final IdGenerator idGenerator;

  @Override
  public UUID handle(MergeTchalaEntriesCommand command) {
    Objects.requireNonNull(command, "command");

    TchalaEntryId fromId = TchalaEntryId.of(command.fromEntryId());
    TchalaEntryId intoId = TchalaEntryId.of(command.intoEntryId());

    TchalaEntry from =
        repo.findById(fromId).orElseThrow(() -> new IllegalArgumentException("from not found"));
    TchalaEntry into =
        repo.findById(intoId).orElseThrow(() -> new IllegalArgumentException("into not found"));

    MergePolicy policy = parsePolicy(command.mergePolicy());
    List<TchalaNumber> merged = TchalaMerge.mergeNumbers(into.numbers(), from.numbers(), policy);

    Instant now = Instant.now(clock);

    // Create a new canonical entry representing the merged canonical state.
    TchalaEntry newInto =
        TchalaEntry.newCanonical(
            TchalaEntryId.of(idGenerator.newUuid()),
            into.lang(),
            into.dream(),
            merged,
            into.note(),
            into.source(),
            now);

    repo.save(newInto);

    // Mark the source as merged into the canonical entry
    TchalaEntry mergedFrom = from.markMergedInto(newInto.id(), now);
    repo.save(mergedFrom);

    return newInto.id().value();
  }

  private MergePolicy parsePolicy(String raw) {
    if (raw == null || raw.isBlank()) return MergePolicy.UNION_NUMBERS;
    return MergePolicy.valueOf(raw.trim().toUpperCase());
  }
}
