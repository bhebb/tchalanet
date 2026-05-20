package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.internal.application.view.PublicDrawResultHistoryRowView;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public record SearchPublicDrawResultsQuery(
    List<String> slotKeys,
    String provider,
    LocalDate from,
    LocalDate to,
    Pageable pageable
) implements Query<TchPage<PublicDrawResultHistoryRowView>> {}
