package com.tchalanet.server.core.draw.application.command.model;

public record OpenDueDrawsResult(int opened, int skippedLocked, int skippedTooLateOrCutoffPassed) {}

