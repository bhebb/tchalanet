package com.tchalanet.server.core.haiti.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.application.command.model.ApproveTchalaEntryCommand;
import com.tchalanet.server.core.haiti.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.domain.tchala.model.MergePolicy;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Approve a pending Tchala entry as canonical, merge into an existing canonical, or replace
 * existing.
 */
@Component
@RequiredArgsConstructor
public class ApproveTchalaEntryCommandHandler
    implements CommandHandler<ApproveTchalaEntryCommand, UUID> {

  private final TchalaEntryRepositoryPort repo;
  private final Clock clock;

  @Override
  public UUID handle(ApproveTchalaEntryCommand command) {
    Objects.requireNonNull(command, "command");
    var entryId = TchalaEntryId.of(command.entryId());
    TchalaEntry pending =
        repo.findById(entryId).orElseThrow(() -> new IllegalArgumentException("entry not found"));

    if (pending.status() != null && pending.status().name().equals("APPROVED")) {
      // already approved
      return pending.id().value();
    }

    var now = Instant.now(clock);
    var mode = command.mode();

    return switch (mode) {
      case APPROVE_AS_NEW_CANONICAL -> doApproveAsNew(pending, now);
      case APPROVE_AND_MERGE_INTO_EXISTING -> doApproveAndMerge(pending, now);
      case APPROVE_AND_REPLACE_EXISTING ->
          doApproveAndReplace(
              pending,
              command
                  .targetCanonicalId()
                  .orElseThrow(
                      () -> new IllegalArgumentException("targetCanonicalId required for replace")),
              now);
    };
  }

  private UUID doApproveAsNew(TchalaEntry pending, Instant now) {
    // mark pending as approved canonical
    var approved = pending.approveAsCanonical(now);
    var saved = repo.save(approved);
    return saved.id().value();
  }

  private UUID doApproveAndMerge(TchalaEntry pending, Instant now) {
    // create updated canonical (or update existing by saving new canonical)
    var newCanonical = pending.approveAsCanonical(now);
    var savedCanonical = repo.save(newCanonical);

    // mark pending as merged
    var mergedFrom = pending.markMergedInto(savedCanonical.id(), now);
    repo.save(mergedFrom);

    return savedCanonical.id().value();
  }

  private UUID doApproveAndReplace(TchalaEntry pending, UUID targetCanonicalUuid, Instant now) {
    var targetId = TchalaEntryId.of(targetCanonicalUuid);
    TchalaEntry canonical =
        repo.findById(targetId)
            .orElseThrow(() -> new IllegalArgumentException("limitScopeRef canonical not found"));

    // archive existing canonical
    var archived = canonical.archive(now);
    repo.save(archived);

    // promote pending to canonical
    var approved = pending.approveAsCanonical(now);
    var saved = repo.save(approved);

    return saved.id().value();
  }

  private MergePolicy parsePolicy(String raw) {
    if (raw == null || raw.isBlank()) return MergePolicy.UNION_NUMBERS;
    try {
      return MergePolicy.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return MergePolicy.UNION_NUMBERS;
    }
  }
}
