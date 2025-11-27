package com.tchalanet.server.news.domain.model;

import java.util.Objects;

/** Represents a single news article. This is a Value Object. */
public record NewsArticle(String id, String title, String content, String url) {
  public NewsArticle {
    Objects.requireNonNull(id, "ID cannot be null");
    Objects.requireNonNull(title, "Title cannot be null");
    Objects.requireNonNull(content, "Content cannot be null");
    Objects.requireNonNull(url, "URL cannot be null");
  }
}
