package com.tchalanet.server.features.publicdraw.infra.web.model;

import java.time.LocalDate;

public record GetPublicDrawResultRequest(String slotKey, LocalDate drawDate) {}
