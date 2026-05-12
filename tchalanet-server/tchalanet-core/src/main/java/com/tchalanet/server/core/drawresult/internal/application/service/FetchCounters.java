package com.tchalanet.server.core.drawresult.internal.application.service;


import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowResult;

public final class FetchCounters {
    public int inserted;
    public int updated;
    public int skipped;
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

    public FetchExternalResultsWindowResult toResult() {
        return new FetchExternalResultsWindowResult(
            inserted,
            updated,
            errors,
            skipped + dryRunMatched + skippedConfirmed + skippedOverridden,
            notFoundTotal());
    }
}
