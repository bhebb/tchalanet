package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.internal.application.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public record ListDrawResultsQuery(
    String slotKey,
    DrawResultStatus status,
    ResultQuality quality,
    LocalDate from,
    LocalDate to,
    Pageable pageable
) implements Query<TchPage<DrawResultView>> {
}
