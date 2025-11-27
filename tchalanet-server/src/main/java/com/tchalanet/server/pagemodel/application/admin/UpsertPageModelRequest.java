package com.tchalanet.server.pagemodel.application.admin;

import java.util.UUID;

public record UpsertPageModelRequest(
    UUID id, UUID tenantId, String code, String lang, String json) {}
