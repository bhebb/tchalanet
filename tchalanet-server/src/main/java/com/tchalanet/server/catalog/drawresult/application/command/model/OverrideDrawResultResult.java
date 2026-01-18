package com.tchalanet.server.catalog.drawresult.application.command.model;

import com.tchalanet.server.common.types.id.DrawResultId;

public record OverrideDrawResultResult(
    DrawResultId drawResultId, boolean created, boolean updated) {}
