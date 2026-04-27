package com.tchalanet.server.core.draw.application.command.model;

public record ApplyExternalResultsWindowResult(
    int inserted, int updated, int noop, int skipped, int notFound, int errors) {}
