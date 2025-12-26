package com.tchalanet.server.core.user.infra.web.dto;

public record UpdateUserProfileRequest(
    String firstName, String lastName, String email, String locale) {}
