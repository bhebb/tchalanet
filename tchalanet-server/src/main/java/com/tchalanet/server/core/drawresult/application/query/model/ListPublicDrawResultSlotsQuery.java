package com.tchalanet.server.core.drawresult.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotView;
import java.util.List;

public record ListPublicDrawResultSlotsQuery(
    List<String> slotKeys,
    String provider
) implements Query<List<PublicDrawResultSlotView>> {}
