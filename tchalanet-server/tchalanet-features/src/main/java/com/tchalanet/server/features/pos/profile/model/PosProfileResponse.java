package com.tchalanet.server.features.pos.profile.model;

public record PosProfileResponse(
    String version,
    PosProfileTerminalInfo terminal,
    PosProfileSellerInfo seller,
    PosProfileCommercialInfo commercial,
    PosProfileSettingsInfo settings) {}
