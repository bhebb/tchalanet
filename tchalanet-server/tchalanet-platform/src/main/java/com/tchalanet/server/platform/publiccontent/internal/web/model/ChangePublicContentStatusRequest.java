package com.tchalanet.server.platform.publiccontent.internal.web.model;

import com.tchalanet.server.platform.publiccontent.api.model.PublicContentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangePublicContentStatusRequest(@NotNull PublicContentStatus status) {}
