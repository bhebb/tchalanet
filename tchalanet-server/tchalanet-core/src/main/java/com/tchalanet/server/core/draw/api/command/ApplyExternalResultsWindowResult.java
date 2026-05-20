package com.tchalanet.server.core.draw.api.command;

public record ApplyExternalResultsWindowResult(
    int inserted, int updated, int noop, int skipped, int notFound, int errors) {}
