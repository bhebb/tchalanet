package com.tchalanet.server.core.uslottery.infra.web.model;

import com.tchalanet.server.core.uslottery.domain.model.UsLotteryProvider;

import java.util.UUID;

public record RefreshRequest(UsLotteryProvider provider, UUID tenantId, UUID userId) {}
