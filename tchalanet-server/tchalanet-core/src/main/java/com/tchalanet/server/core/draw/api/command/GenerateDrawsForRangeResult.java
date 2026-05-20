package com.tchalanet.server.core.draw.api.command;

public record GenerateDrawsForRangeResult(
    int created, int skipped, int alreadyExists, int conflicts) {}
