package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;

public record GetDrawResultViewByIdQuery(
    DrawResultId id
) implements Query<DrawResultView> {
}
