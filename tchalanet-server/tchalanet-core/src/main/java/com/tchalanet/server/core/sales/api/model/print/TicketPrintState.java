package com.tchalanet.server.core.sales.api.model.print;

import java.time.Instant;
import java.util.Objects;

public record TicketPrintState(
    TicketPrintStateStatus status,
    int printCount,
    Instant firstPrintedAt,
    Instant lastPrintedAt
) {
    public TicketPrintState {
        Objects.requireNonNull(status, "status is required");

        if (printCount < 0) {
            throw new IllegalArgumentException("printCount cannot be negative");
        }

        if (printCount == 0) {
            if (status != TicketPrintStateStatus.NOT_PRINTED) {
                throw new IllegalArgumentException("status must be NOT_PRINTED when printCount is 0");
            }
            if (firstPrintedAt != null || lastPrintedAt != null) {
                throw new IllegalArgumentException("printed timestamps must be null when printCount is 0");
            }
        }

        if (printCount > 0) {
            if (firstPrintedAt == null || lastPrintedAt == null) {
                throw new IllegalArgumentException("printed timestamps are required when printCount > 0");
            }
            if (lastPrintedAt.isBefore(firstPrintedAt)) {
                throw new IllegalArgumentException("lastPrintedAt cannot be before firstPrintedAt");
            }
        }
    }

    public static TicketPrintState notPrinted() {
        return new TicketPrintState(TicketPrintStateStatus.NOT_PRINTED, 0, null, null);
    }

    public TicketPrintState markPrinted(Instant now) {
        Objects.requireNonNull(now, "now is required");

        return new TicketPrintState(
            printCount == 0 ? TicketPrintStateStatus.PRINTED : TicketPrintStateStatus.REPRINTED,
            printCount + 1,
            printCount == 0 ? now : firstPrintedAt,
            now
        );
    }

    public boolean printed() {
        return printCount > 0;
    }

    public boolean reprinted() {
        return printCount > 1;
    }
}
