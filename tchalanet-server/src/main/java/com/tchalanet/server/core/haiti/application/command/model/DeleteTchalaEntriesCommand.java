package com.tchalanet.server.core.haiti.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.List;
import java.util.UUID;

public record DeleteTchalaEntriesCommand(List<UUID> entryIds) implements Command<Void> {}
