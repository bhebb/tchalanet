package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.common.bus.QueryBus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PosSaleContextResolver {

    private final QueryBus queryBus;

}
