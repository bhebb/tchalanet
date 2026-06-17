package com.tchalanet.server.core.uslottery.internal.infra.registry;

import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

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

    public Optional<UsLotteryProviderClient> find(UsLotteryProvider provider) {
        return Optional.ofNullable(clientsByProvider.get(provider));
    }
}
