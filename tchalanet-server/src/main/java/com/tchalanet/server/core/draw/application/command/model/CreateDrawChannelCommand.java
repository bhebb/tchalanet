package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import java.util.UUID;

public record CreateDrawChannelCommand(
    UUID tenantId,
    String code,
    String name,
    boolean active) implements Command<DrawChannel> {}
