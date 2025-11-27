package com.tchalanet.server.news.domain.ports.out;

import com.tchalanet.server.news.domain.model.NewsArticle;
import java.util.List;

public interface NewsProviderPort {
  List<NewsArticle> fetchLatestNews();
}
