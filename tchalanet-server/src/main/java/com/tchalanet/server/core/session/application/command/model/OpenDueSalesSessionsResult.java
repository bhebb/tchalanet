package com.tchalanet.server.core.session.application.command.model;

public record OpenDueSalesSessionsResult(
    int countTargets,
    int sessionsOpened,
    int skippedAlreadyExists) {}
