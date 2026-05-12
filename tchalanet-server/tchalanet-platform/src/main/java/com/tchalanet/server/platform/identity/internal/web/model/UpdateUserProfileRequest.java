package com.tchalanet.server.platform.identity.internal.web.model;

// Added phone field so controller can pass it to UpdateUserProfileCommand
public record UpdateUserProfileRequest(
    String firstName, String lastName, String email, String phone, String locale) {}
