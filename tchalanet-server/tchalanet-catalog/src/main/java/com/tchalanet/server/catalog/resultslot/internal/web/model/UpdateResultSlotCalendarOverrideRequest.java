package com.tchalanet.server.catalog.resultslot.internal.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Update the mutable fields of an override. The shape (specific vs recurring)
 * and the slot are immutable — recreate to change them.
 */
public record UpdateResultSlotCalendarOverrideRequest(
    boolean available,
    @NotBlank @Size(max = 96) String reasonCode,
    @Size(max = 255) String reasonLabel) {}
