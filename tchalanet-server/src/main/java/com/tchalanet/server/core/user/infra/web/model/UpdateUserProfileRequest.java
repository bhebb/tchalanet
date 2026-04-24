package com.tchalanet.server.core.user.infra.web.model;

// Added phone field so controller can pass it to UpdateUserProfileCommand
public record UpdateUserProfileRequest(
    String firstName, String lastName, String email, String phone, String locale) {}
