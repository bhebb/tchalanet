package com.tchalanet.server.features.bootstrap;

public record RuntimeBootstrapNotice(
    String code,
    String message,
    NoticeLevel level
) {
    public enum NoticeLevel { INFO, WARNING, ERROR }

    public static com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeBootstrapNotice warning(String code, String message) {
        return new com.tchalanet.server.features.bootstrap.publicruntime.model.RuntimeBootstrapNotice(code, message, NoticeLevel.WARNING);
    }
}
