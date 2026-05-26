package com.tchalanet.server.core.terminal.internal.infra.context;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.operational.OperationalContextHeaderParser;
import com.tchalanet.server.common.context.operational.OperationalContextHeaders;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextResolver;
import com.tchalanet.server.core.terminal.api.query.ResolveOperationalContextQuery;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TerminalOperationalContextResolver implements OperationalContextResolver {

    private final QueryBus queryBus;

    @Override
    public OperationalContextHint resolve(
        TchRequestContext requestContext,
        OperationalContextHeaderParser.HeaderReader headers
    ) {
        var candidate = OperationalContextHeaderParser.parseHint(headers);
        var credential = firstNonBlank(
            headers.getHeader(OperationalContextHeaders.DEVICE_BINDING),
            headers.getHeader(OperationalContextHeaders.VIRTUAL_TERMINAL_BINDING));

        return queryBus.ask(new ResolveOperationalContextQuery(
            requestContext.effectiveTenantIdOrNull(),
            requestContext.userId(),
            candidate,
            credential));
    }

    private static String firstNonBlank(String first, String second) {
        if (StringUtils.isNotBlank(first)) {
            return first.trim();
        }
        if (StringUtils.isNotBlank(second)) {
            return second.trim();
        }
        return null;
    }
}
