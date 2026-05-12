package com.tchalanet.server.platform.identity.internal.web.model;

public record UpdateUserRequest(String firstName, String lastName, String email, String locale) {}
