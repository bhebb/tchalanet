package com.tchalanet.server.core.drawresult.api.query.view;

import java.time.LocalTime;
import java.util.List;

public record PublicDrawResultSlotView(
    String slotKey,
    String provider,
    String label,
    String timezone,
    LocalTime drawTime,
    boolean active,
    PublicNextResultTimeView next,
    PublicDrawResultView latest,
    List<PublicDrawResultView> history
) {}
