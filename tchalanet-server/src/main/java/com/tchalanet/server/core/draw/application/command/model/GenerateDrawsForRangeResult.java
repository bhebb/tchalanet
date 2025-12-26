package com.tchalanet.server.core.draw.application.command.model;

public record GenerateDrawsForRangeResult(
    int created, int skipped, int alreadyExists, int conflicts) {}
