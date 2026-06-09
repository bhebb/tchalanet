package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.drawresult.api.query.view.PublicDrawResultSlotDetailsView;

import java.time.LocalDate;
import java.util.List;

public record ListPublicDrawResultSlotDetailsQuery(
    List<String> slotKeys,
    String provider,
    LocalDate resultDate,
    int historyLimit)
    implements Query<List<PublicDrawResultSlotDetailsView>> {}
