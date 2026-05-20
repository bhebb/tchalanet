package com.tchalanet.server.features.publicdrawresults.model;

import com.tchalanet.server.core.drawresult.internal.application.view.PublicNextResultTimeView;
import java.time.LocalTime;
import java.util.List;

public record PublicDrawResultSlotResponse(
    String slotKey,
    String provider,
    String label,
    String timezone,
    LocalTime drawTime,
    PublicNextResultTimeView next,
    PublicDrawResultItemResponse latest,
    List<PublicDrawResultItemResponse> history) {}
