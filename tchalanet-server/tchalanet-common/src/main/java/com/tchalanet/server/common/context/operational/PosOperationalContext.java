package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.types.id.UserId;

public record PosOperationalContext(
    UserId sellerUserId,
    OperationalContextRole role,
    OperationalContextSource source,
    OperationalContextTrust trustLevel
) implements OperationalRequestContext {
}
