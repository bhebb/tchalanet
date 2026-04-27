package com.tchalanet.server.core.haiti.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

/**
 * Command to merge one Tchala entry into another. Returns the UUID of the resulting canonical
 * entry.
 */
public record MergeTchalaEntriesCommand(UUID fromEntryId, UUID intoEntryId, String mergePolicy)
    implements Command<UUID> {}
