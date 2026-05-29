package com.tchalanet.server.features.pagemodel.contract;

/**
 * Typed contract for a public content / news item in PageModel payloads.
 * Shared between public home and private dashboard public-content widgets.
 */
public record NewsItem(
    String id,
    String title,
    String snippet,
    String link,
    String source,
    String publishedAt) {}
