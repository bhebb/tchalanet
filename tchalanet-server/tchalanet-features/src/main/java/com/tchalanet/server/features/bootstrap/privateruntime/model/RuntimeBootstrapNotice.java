package com.tchalanet.server.features.bootstrap.privateruntime.model;

public record RuntimeBootstrapNotice(
    String code,
    String message,
    NoticeLevel level
) {
    public enum NoticeLevel { INFO, WARNING, ERROR }

    public static RuntimeBootstrapNotice warning(String code, String message) {
        return new RuntimeBootstrapNotice(code, message, NoticeLevel.WARNING);
    }
}
