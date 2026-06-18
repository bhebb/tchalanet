package com.tchalanet.server.core.uslottery.internal.infra.external.oh.auth;

import java.util.Optional;

public interface OhioTokenProvider {
    Optional<String> bearerToken();
}
