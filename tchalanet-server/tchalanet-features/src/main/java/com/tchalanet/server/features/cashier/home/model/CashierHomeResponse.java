package com.tchalanet.server.features.cashier.home.model;

import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import java.util.List;

public record CashierHomeResponse(
    ClientSurface surface,
    String version,
    HomeHeader header,
    HomeRequiredStep requiredStep,
    CashierHomeOperationalContext operationalContext,
    CashierHomeSessionSummary session,
    CashierHomeDrawSummary primaryDraw,
    HomeAction primaryAction,
    List<HomeAction> quickActions,
    List<HomeWidget> widgets,
    List<HomeNavigationItem> navigation,
    List<String> notices) {}
