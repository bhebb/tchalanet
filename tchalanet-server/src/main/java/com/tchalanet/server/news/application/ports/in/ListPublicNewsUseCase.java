package com.tchalanet.server.news.application.ports.in;

import com.tchalanet.server.news.domain.model.NewsArticle;
import java.util.List;

public interface ListPublicNewsUseCase {
  List<NewsArticle> listNews();
}
