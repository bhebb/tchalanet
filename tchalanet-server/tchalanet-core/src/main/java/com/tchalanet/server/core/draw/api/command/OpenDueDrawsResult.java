package com.tchalanet.server.core.draw.api.command;

public record OpenDueDrawsResult(int opened, int skippedLocked, int skippedTooLateOrCutoffPassed) {}
