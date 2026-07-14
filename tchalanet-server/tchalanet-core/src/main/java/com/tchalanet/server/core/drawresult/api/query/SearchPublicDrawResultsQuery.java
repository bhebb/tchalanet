package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultHistoryRowView;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public record SearchPublicDrawResultsQuery(
    List<String> slotKeys, String provider, LocalDate from, LocalDate to, Pageable pageable)
    implements Query<TchPage<PublicDrawResultHistoryRowView>> {}
