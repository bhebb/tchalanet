package com.tchalanet.server.platform.identity.internal.web;

import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.http.TchHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Decides which identity resolver the bootstrap step should use, from the {@code X-Tch-Client-Type}
 * hint. This selects a resolver only — it grants nothing. The proof of identity and access remains
 * the verified JWT and the Tchalanet DB mappings.
 *
 * <ul>
 *   <li>{@code X-Tch-Client-Type: POS} → resolve as {@link TchActorType#SELLER_TERMINAL}.
 *   <li>header absent / anything else → resolve as {@link TchActorType#APP_USER}.
 * </ul>
 */
@Component
public class ExpectedActorTypeResolver {

    public TchActorType resolve(HttpServletRequest request) {
        var clientType = request.getHeader(TchHeaders.X_TCH_CLIENT_TYPE);

        if (StringUtils.equalsIgnoreCase(clientType, TchHeaders.CLIENT_TYPE_POS)) {
            return TchActorType.SELLER_TERMINAL;
        }

        return TchActorType.APP_USER;
    }
}
