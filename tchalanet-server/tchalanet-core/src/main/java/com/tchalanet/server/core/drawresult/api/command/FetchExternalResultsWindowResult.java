package com.tchalanet.server.core.drawresult.api.command;

public record FetchExternalResultsWindowResult(
    int inserted, int updated, int noop, int skipped, int notFound) {}
