package com.tchalanet.server.core.drawresult.internal.application.service;

import com.tchalanet.server.core.drawresult.api.command.FetchExternalResultsWindowResult;

public final class FetchCounters {

    public int inserted;
    public int updated;
    public int skipped;
    public int alreadyFetched;
    public int skippedConfirmed;
    public int skippedOverridden;
    public int slotNotFound;
    public int slotInactive;
    public int noExternalResult;
    public int errors;
    public int dryRunMatched;

    public int notFoundTotal() {
        return slotNotFound + slotInactive + noExternalResult;
    }

    public int skippedTotal() {
        return skipped + alreadyFetched + dryRunMatched + skippedConfirmed + skippedOverridden;
    }

    public FetchExternalResultsWindowResult toResult() {
        return new FetchExternalResultsWindowResult(
            inserted,
            updated,
            errors,
            skippedTotal(),
            notFoundTotal());
    }
}
