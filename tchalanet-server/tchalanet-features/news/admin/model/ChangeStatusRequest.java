package com.tchalanet.server.features.news.admin.model;

import com.tchalanet.server.features.news.shared.NewsStatus;

public record ChangeStatusRequest(NewsStatus status) {}
