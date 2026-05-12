package com.tchalanet.server.core.drawresult.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.drawresult.application.view.PublicDrawResultSlotDetailsView;
import java.util.List;

public record ListPublicDrawResultSlotDetailsQuery(
    List<String> slotKeys,
    String provider,
    int historyLimit)
    implements Query<List<PublicDrawResultSlotDetailsView>> {}
