package com.tchalanet.server.features.pos.profile.model;

import java.util.List;

public record PosProfileSettingsInfo(
    String locale,
    String timezone,
    String currency,
    List<String> supportedLocales,
    PosProfileReceiptSettings receipt,
    PosProfileNotificationSettings notifications) {}
