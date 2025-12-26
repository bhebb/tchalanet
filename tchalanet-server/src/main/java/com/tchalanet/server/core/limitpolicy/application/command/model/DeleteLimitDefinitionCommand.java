package com.tchalanet.server.core.limitpolicy.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;

public record DeleteLimitDefinitionCommand(UUID definitionId) implements Command<Void> {}
