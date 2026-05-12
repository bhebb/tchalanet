package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.drawresult.application.view.DrawResultView;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
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
