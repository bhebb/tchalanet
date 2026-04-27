package com.tchalanet.server.core.user.infra.web.model;

import com.tchalanet.server.common.types.id.UserId;
import java.util.UUID;

public record MeResponse(
    UserId id,
    UUID keycloakSub,
    String username,
    String email,
    String firstName,
    String lastName,
    String displayName,
    boolean isNew,
    TenantContextResponse tenant,
    UserPreferenceResponse preferences,
    EffectiveUiContextResponse effective) {}
