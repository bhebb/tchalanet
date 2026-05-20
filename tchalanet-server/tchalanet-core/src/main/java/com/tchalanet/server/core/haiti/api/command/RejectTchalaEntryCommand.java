package com.tchalanet.server.core.haiti.api.command;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record RejectTchalaEntryCommand(UUID entryId, String reason) implements Command<Void> {}
