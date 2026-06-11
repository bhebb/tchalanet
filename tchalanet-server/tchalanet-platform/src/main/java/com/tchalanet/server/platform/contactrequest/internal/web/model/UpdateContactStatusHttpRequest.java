package com.tchalanet.server.platform.contactrequest.internal.web.model;

import com.tchalanet.server.platform.contactrequest.api.ContactRequestStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateContactStatusHttpRequest(@NotNull ContactRequestStatus status) {}
