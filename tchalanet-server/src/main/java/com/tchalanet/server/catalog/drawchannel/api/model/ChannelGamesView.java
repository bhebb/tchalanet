package com.tchalanet.server.catalog.drawchannel.api.model;

import java.util.List;

public record ChannelGamesView(String channelCode, List<GameSummaryView> games) {}
