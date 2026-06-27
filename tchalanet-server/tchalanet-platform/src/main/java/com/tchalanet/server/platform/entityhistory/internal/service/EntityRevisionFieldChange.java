package com.tchalanet.server.platform.entityhistory.internal.service;

public record EntityRevisionFieldChange(
    String field,
    String before,
    String after) {}
