package com.tchalanet.server.features.news.shared;


public interface NewsProvider {

  LotteryNewsModels.LotteryNewsFeedSnapshot fetchLatestNews();
}
