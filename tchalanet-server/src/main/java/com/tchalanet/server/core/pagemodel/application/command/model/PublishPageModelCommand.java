package com.tchalanet.server.core.pagemodel.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PageModelId;

public record PublishPageModelCommand(PageModelId id) implements Command<Void> {}
