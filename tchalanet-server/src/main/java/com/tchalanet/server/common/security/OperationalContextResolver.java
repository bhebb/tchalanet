package com.tchalanet.server.common.security;

import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.context.TchRequestContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Resolves the candidate operational context (terminal/outlet/session) for a request.
 * Called inside TchContextFilter after actor resolution, before contextBinder.bind.
 */
public interface OperationalContextResolver {

    Optional<OperationalRequestContext> resolve(
        TchRequestContext ctx,
        HttpServletRequest request
    );
}
