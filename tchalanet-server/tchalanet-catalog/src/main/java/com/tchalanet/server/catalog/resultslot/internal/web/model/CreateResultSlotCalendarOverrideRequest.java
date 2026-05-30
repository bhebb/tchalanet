package com.tchalanet.server.catalog.resultslot.internal.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Create a provider calendar override for a result_slot. Provide exactly one of
 * {@code slotLocalDate} (specific dated occurrence) or {@code recurringMd}
 * (year-less 'MM-dd' annual rule) — the service enforces the XOR.
 */
public record CreateResultSlotCalendarOverrideRequest(
    LocalDate slotLocalDate,
    @Pattern(regexp = "^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$",
        message = "recurringMd must be 'MM-dd'")
    String recurringMd,
    boolean available,
    @NotBlank @Size(max = 96) String reasonCode,
    @Size(max = 255) String reasonLabel) {}
