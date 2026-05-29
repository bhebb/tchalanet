package com.tchalanet.server.common.context.operational;

import com.tchalanet.server.common.context.TchRequestContext;

public interface OperationalContextResolver {

    OperationalContextHint resolve(
        TchRequestContext requestContext,
        OperationalContextHeaderParser.HeaderReader headers
    );
}
