package com.tchalanet.server.common.security;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.OperationalContextSource;
import com.tchalanet.server.common.context.OperationalRequestContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.terminal.application.query.model.GetCurrentOperationalContextQuery;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.tchalanet.server.common.constant.TchHeaders.X_DEVICE_ID;
import static com.tchalanet.server.common.constant.TchHeaders.X_TERMINAL_BINDING;
import static com.tchalanet.server.common.constant.TchHeaders.X_TERMINAL_ID;

@Component
@RequiredArgsConstructor
public class DefaultOperationalContextResolver implements OperationalContextResolver {

    private final QueryBus queryBus;

    @Override
    public Optional<OperationalRequestContext> resolve(
        TchRequestContext ctx,
        HttpServletRequest request
    ) {
        if (!shouldResolve(ctx, request)) {
            return Optional.empty();
        }

        var query = new GetCurrentOperationalContextQuery(
            ctx.effectiveTenantIdRequired(),
            ctx.currentUserIdRequired(),
            request.getHeader(X_TERMINAL_ID),
            request.getHeader(X_DEVICE_ID),
            request.getHeader(X_TERMINAL_BINDING)
        );

        var view = queryBus.ask(query);

        if (view == null || view.terminalId() == null || view.source() == OperationalContextSource.NONE) {
            return Optional.empty();
        }

        return Optional.of(new OperationalRequestContext(
            view.terminalId(),
            view.outletId(),
            view.salesSessionId(),
            view.source()
        ));
    }

    private boolean shouldResolve(TchRequestContext ctx, HttpServletRequest request) {
        if (ctx.isCashier() || ctx.isOperator()) {
            return true;
        }
        // Admins get operational context only when they carry an explicit terminal binding.
        if ((ctx.isTenantAdmin() || ctx.isSuperAdmin()) && hasTerminalBinding(request)) {
            return true;
        }
        return false;
    }

    private boolean hasTerminalBinding(HttpServletRequest request) {
        return StringUtils.isNotBlank(request.getHeader(X_TERMINAL_ID))
            || StringUtils.isNotBlank(request.getHeader(X_DEVICE_ID))
            || StringUtils.isNotBlank(request.getHeader(X_TERMINAL_BINDING));
    }
}
