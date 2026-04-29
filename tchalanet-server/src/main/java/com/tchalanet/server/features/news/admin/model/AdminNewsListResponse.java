package com.tchalanet.server.features.news.admin.model;

import java.util.List;

public record AdminNewsListResponse(
    List<AdminNewsItem> internal, List<AdminNewsItem> external) {}
