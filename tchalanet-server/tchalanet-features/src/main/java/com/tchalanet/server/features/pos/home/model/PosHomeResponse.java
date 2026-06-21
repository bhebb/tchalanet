package com.tchalanet.server.features.pos.home.model;

import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import java.util.List;

public record PosHomeResponse(
    ClientSurface surface,
    String version,
    HomeHeader header,
    HomeRequiredStep requiredStep,
    PosHomeOperationalContext operationalContext,
    PosHomeSessionSummary session,
    PosHomeDrawSummary primaryDraw,
    HomeAction primaryAction,
    List<HomeAction> quickActions,
    List<HomeWidget> widgets,
    List<HomeNavigationItem> navigation,
    List<String> notices,
    /** Tenant currency code (e.g. "HTG"). Used by mobile to send sell/preview requests. */
    String currency) {}
