package com.tchalanet.server.core.draw.application.command.model;

public record ApplyExternalResultsForDateResult(
    int resulted, int skippedMissing, int skippedQuality, int skippedAlreadyLinked) {}
