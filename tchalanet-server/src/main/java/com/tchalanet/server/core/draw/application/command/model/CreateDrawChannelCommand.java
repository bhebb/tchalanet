package com.tchalanet.server.core.draw.application.command.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import java.util.UUID;
import java.util.List;

public record CreateDrawChannelCommand(
    TenantId tenantId,
    String code,
    String name,
    boolean active,
    String gameCode,
    String timezone,
    String drawTime,
    Integer cutoffSec,
    List<String> daysOfWeek,
    Integer sortOrder,
    String defaultSource,
    String label
    ) implements Command<DrawChannel> {}
