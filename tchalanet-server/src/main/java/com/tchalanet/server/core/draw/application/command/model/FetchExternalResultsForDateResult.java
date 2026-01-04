package com.tchalanet.server.core.draw.application.command.model;

public record FetchExternalResultsForDateResult(
    int inserted, int updated, int noop, int skipped, int notFound) {}
