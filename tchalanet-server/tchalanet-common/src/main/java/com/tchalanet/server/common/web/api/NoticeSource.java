package com.tchalanet.server.common.web.api;

import jakarta.annotation.Nullable;

/**
 * Optional source metadata for a response notice.
 *
 * <p>Keep this small and generic. Feature-specific metadata can still be carried separately, but
 * frontend behavior should rely on these stable fields.
 */
public record NoticeSource(
    String source,
    @Nullable String service,
    @Nullable String operation
) {

    public static NoticeSource of(String source) {
        return new NoticeSource(source, null, null);
    }

    public NoticeSource service(String service) {
        return new NoticeSource(source, service, operation);
    }

    public NoticeSource operation(String operation) {
        return new NoticeSource(source, service, operation);
    }
}
