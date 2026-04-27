package com.tchalanet.server.core.drawresult.application.command.model;

public record FetchExternalResultsWindowResult(
    int inserted, int updated, int noop, int skipped, int notFound) {}
