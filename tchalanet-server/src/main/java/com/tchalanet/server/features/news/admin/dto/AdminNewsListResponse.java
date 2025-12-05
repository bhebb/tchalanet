package com.tchalanet.server.features.news.admin.dto;

import java.util.List;


public record AdminNewsListResponse(
    List<AdminNewsItemDto> internal,
    List<AdminNewsItemDto> external
) {
}
