package com.tchalanet.server.core.user.infra.web.model;

public record UpdateUserProfileRequest(
    String firstName, String lastName, String email, String locale) {}
