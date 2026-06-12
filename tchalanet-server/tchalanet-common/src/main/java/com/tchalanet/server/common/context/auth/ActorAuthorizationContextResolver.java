package com.tchalanet.server.common.context.auth;

import com.tchalanet.server.common.context.TchRequestContext;

/**
 * Replaces external-token authorization hints with server-owned roles and permissions.
 */
public interface ActorAuthorizationContextResolver {

  TchRequestContext resolve(TchRequestContext context);
}
