package com.tchalanet.server.core.drawresult.api.command;

import com.tchalanet.server.common.types.id.DrawResultId;

public record OverrideDrawResultResult(
    DrawResultId drawResultId, boolean created, boolean updated) {}
