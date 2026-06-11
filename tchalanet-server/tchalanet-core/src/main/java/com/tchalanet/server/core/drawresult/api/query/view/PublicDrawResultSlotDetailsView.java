package com.tchalanet.server.core.drawresult.api.query.view;

import java.time.LocalTime;
import java.util.List;

public record PublicDrawResultSlotDetailsView(
    String slotKey,
    String provider,
    /** Clé i18n stable (ex: "draw_channel.ny.eve.label"). Utilisée par le frontend pour la traduction. */
    String drawChannelLabelKey,
    /** Label public résolu côté backend (ex: "New York — Soir"). Fallback si l'i18n n'est pas disponible. */
    String label,
    String timezone,
    LocalTime drawTime,
    boolean active,
    PublicNextResultTimeView next,
    PublicDrawResultView latest,
    List<PublicDrawResultHistoryRowView> history) {}
