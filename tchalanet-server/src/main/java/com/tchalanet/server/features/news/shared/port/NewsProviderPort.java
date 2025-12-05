package com.tchalanet.server.features.news.shared.port;

import com.tchalanet.server.features.news.shared.LotteryNewsModels;

import java.util.UUID;

public interface NewsProviderPort {

    LotteryNewsModels.LotteryNewsFeedSnapshot fetchLatestNews();
}
