package com.tchalanet.server.news.web;

import com.tchalanet.server.news.application.ports.in.ListPublicNewsUseCase;
import com.tchalanet.server.news.domain.model.NewsArticle;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/news")
@RequiredArgsConstructor
public class NewsPublicController {

  private final ListPublicNewsUseCase newsUseCase;
  private final Random random = new Random();

  @GetMapping("/random")
  public List<NewsArticle> random(@RequestParam(defaultValue = "5") int count) {
    var all = newsUseCase.listNews();
    if (all.isEmpty()) return Collections.emptyList();
    // shuffle and pick count
    Collections.shuffle(all, random);
    return all.stream().limit(Math.max(0, count)).toList();
  }

  @GetMapping
  public List<NewsArticle> list(
      @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "20") int limit) {
    var all = newsUseCase.listNews();
    if (all.isEmpty()) return Collections.emptyList();
    int from = Math.max(0, offset);
    int to = Math.min(all.size(), from + Math.max(1, limit));
    return all.subList(from, to);
  }
}
