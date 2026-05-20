package com.tchalanet.server.core.haiti.api.command;

import com.tchalanet.server.common.bus.Command;
import java.util.Optional;
import java.util.UUID;

public record ApproveTchalaEntryCommand(
    UUID entryId, ApprovalMode mode, Optional<UUID> targetCanonicalId, String mergePolicy)
    implements Command<UUID> {
  public enum ApprovalMode {
    APPROVE_AS_NEW_CANONICAL,
    APPROVE_AND_MERGE_INTO_EXISTING,
    APPROVE_AND_REPLACE_EXISTING
  }
}
