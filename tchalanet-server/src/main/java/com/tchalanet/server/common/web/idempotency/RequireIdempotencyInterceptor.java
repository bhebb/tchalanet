package com.tchalanet.server.common.web.idempotency;

import com.tchalanet.server.common.constant.TchHeaders;
import com.tchalanet.server.common.stereotype.RequireIdempotency;
import com.tchalanet.server.common.types.enums.IdempotencyScope;
import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class RequireIdempotencyInterceptor implements HandlerInterceptor {

    public static final String ATTR_IDEM_SCOPE = "tch.idem.scope";
    public static final String ATTR_IDEM_KEY = "tch.idem.key";
    public static final String ATTR_IDEM_BEGIN = "tch.idem.begin";

    @Override
    public boolean preHandle(
        @Nonnull HttpServletRequest request,
        @Nonnull HttpServletResponse response,
        @Nonnull Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod hm)) return true;

        // MVP: scope hardcodé pour SELL ticket (tu peux généraliser ensuite)
        IdempotencyScope scope = IdempotencyScope.SALES_SELL_TICKET;

        var method = hm.getMethod();
        boolean required = method.isAnnotationPresent(RequireIdempotency.class)
            || hm.getBeanType().isAnnotationPresent(RequireIdempotency.class);

        if (!required) return true;

        var key = request.getHeader(TchHeaders.IDEMPOTENCY_KEY);
        if (StringUtils.isBlank(key)) {
            response.sendError(BAD_REQUEST.value(), "Missing Idempotency-Key");
            return false;
        }

        key = key.trim();

        // Write key to per-request TchContext so downstream code/services can read it
        TchRequestContext ctx = TchContext.currentOrNull();
        if (ctx != null) {
            TchContext.set(ctx.withIdempotencyKey(key));
        }

        // still set attributes for backward compatibility / controllers
        request.setAttribute(ATTR_IDEM_SCOPE, scope);
        request.setAttribute(ATTR_IDEM_KEY, key);
        request.setAttribute(ATTR_IDEM_BEGIN, null); // controller fera begin() après avoir le DTO

        return true;
    }
}
