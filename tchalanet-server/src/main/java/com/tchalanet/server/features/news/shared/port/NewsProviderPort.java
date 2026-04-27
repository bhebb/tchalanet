package com.tchalanet.server.features.news.shared.port;

import com.tchalanet.server.features.news.shared.LotteryNewsModels;

public interface NewsProviderPort {

  LotteryNewsModels.LotteryNewsFeedSnapshot fetchLatestNews();
}
