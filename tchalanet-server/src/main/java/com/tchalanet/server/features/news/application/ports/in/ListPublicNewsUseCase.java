package com.tchalanet.server.features.news.application.ports.in;

import com.tchalanet.server.features.news.domain.model.NewsArticle;
import java.util.List;

public interface ListPublicNewsUseCase {
  List<NewsArticle> listNews();
}
