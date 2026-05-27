package com.tchalanet.server.features.news.admin.model;

import com.tchalanet.server.platform.publiccontent.api.model.PublicContentStatus;

public record ChangeStatusRequest(PublicContentStatus status) {}
