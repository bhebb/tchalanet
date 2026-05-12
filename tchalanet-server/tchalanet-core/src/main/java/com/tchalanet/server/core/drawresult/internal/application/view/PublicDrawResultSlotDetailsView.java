package com.tchalanet.server.core.drawresult.internal.application.view;

import java.time.LocalTime;
import java.util.List;

public record PublicDrawResultSlotDetailsView(
    String slotKey,
    String provider,
    String label,
    String timezone,
    LocalTime drawTime,
    boolean active,
    PublicNextResultTimeView next,
    PublicDrawResultView latest,
    List<PublicDrawResultHistoryRowView> history) {}
