package com.tchalanet.server.core.uslottery.infra.registry;

import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderClientRegistry {

    private final Map<UsLotteryProvider, UsLotteryProviderClient> clientsByProvider;

    public ProviderClientRegistry(List<UsLotteryProviderClient> clients) {
        this.clientsByProvider =
            clients.stream()
                .collect(
                    Collectors.toUnmodifiableMap(
                        UsLotteryProviderClient::provider,
                        Function.identity()));
    }

    public UsLotteryProviderClient get(UsLotteryProvider provider) {
        var client = clientsByProvider.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("Unsupported lottery provider: " + provider);
        }
        return client;
    }
}
