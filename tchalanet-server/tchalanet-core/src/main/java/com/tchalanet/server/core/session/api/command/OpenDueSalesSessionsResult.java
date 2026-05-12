package com.tchalanet.server.core.session.api.command;

public record OpenDueSalesSessionsResult(
    int countTargets,
    int sessionsOpened,
    int skippedAlreadyExists) {}
