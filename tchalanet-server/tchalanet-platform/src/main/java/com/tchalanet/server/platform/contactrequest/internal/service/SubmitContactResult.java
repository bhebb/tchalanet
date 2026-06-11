package com.tchalanet.server.platform.contactrequest.internal.service;

import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSubmittedView;

public record SubmitContactResult(
    ContactRequestSubmittedView view,
    boolean notificationFailed
) {}
