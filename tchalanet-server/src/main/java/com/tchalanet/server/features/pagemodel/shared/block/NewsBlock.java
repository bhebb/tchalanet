package com.tchalanet.server.features.pagemodel.shared.block;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tchalanet.server.features.news.shared.LotteryNewsModels;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NewsBlock(List<NewsItem> items) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NewsItem(
        String title,
        String description,
        Instant publishedAt,
        String url
    ) {
        public static NewsItem fromDomain(LotteryNewsModels.LotteryNewsArticle a) {
            if (a == null) {
                return null;
            }
            return new NewsItem(
                a.title(),
                a.description(),
                a.publishedAt(),
                a.url() != null ? a.url().toString() : null
            );
        }
    }
}

