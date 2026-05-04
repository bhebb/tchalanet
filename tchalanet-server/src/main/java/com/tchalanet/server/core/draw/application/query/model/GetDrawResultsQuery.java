package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import jakarta.validation.constraints.NotNull;

public record GetDrawResultsQuery(@NotNull DrawId drawId) implements Query<DrawResultView> {}
