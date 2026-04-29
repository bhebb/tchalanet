package com.tchalanet.server.features.publicdraw.model;

import java.time.LocalDate;

public record GetPublicDrawResultRequest(String slotKey, LocalDate drawDate) {}
