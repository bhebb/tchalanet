package com.tchalanet.server.core.draw.infra.web.model;

import java.time.LocalDate;

public record GetPublicDrawResultRequest(String channelCode, LocalDate drawDate) {}
