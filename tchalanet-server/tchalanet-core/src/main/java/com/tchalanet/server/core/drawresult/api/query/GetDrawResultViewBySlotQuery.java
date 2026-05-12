package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import java.time.Instant;

public record GetDrawResultViewBySlotQuery(
    String slotKey,
    Instant occurredAt
) implements Query<DrawResultView> {}
