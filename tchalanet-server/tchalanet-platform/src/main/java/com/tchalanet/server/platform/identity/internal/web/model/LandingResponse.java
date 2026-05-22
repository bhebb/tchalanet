package com.tchalanet.server.platform.identity.internal.web.model;

import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import java.util.Set;

public record LandingResponse(ClientSurface preferredSurface, Set<ClientSurface> availableSurfaces) {}
