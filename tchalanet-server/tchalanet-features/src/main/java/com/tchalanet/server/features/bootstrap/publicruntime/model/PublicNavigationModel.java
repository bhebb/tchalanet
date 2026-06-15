package com.tchalanet.server.features.bootstrap.publicruntime.model;

import jakarta.annotation.Nullable;
import java.util.List;

/** Public navigation for the public shell (header items + optional footer items). */
public record PublicNavigationModel(
    List<PublicNavigationItem> items,
    @Nullable List<PublicNavigationItem> footerItems
) {}
