package com.tchalanet.server.platform.contactrequest.internal.web.model;

import jakarta.validation.constraints.Size;

public record UpdateContactNotesHttpRequest(
    String internalNotes,

    @Size(max = 80)
    String externalTool,

    @Size(max = 160)
    String externalReference
) {}
