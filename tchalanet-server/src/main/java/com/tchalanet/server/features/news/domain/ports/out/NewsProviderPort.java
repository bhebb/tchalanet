package com.tchalanet.server.features.news.domain.ports.out;

import com.tchalanet.server.features.news.domain.model.NewsArticle;
import java.util.List;

public interface NewsProviderPort {
  List<NewsArticle> fetchLatestNews();
}
