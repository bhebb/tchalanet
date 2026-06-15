package com.tchalanet.server.platform.identity.internal.web.model;

import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Set;

public record MeResponse(
    UserId id,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName,
    boolean isNew,
    TenantContextResponse tenant,
    UserPreferenceResponse preferences,
    EffectiveUiContextResponse effective,
    Set<TchRole> roles,
    LandingResponse landing,
    Set<String> capabilities,
    ProfileActionsResponse profileActions) {}
