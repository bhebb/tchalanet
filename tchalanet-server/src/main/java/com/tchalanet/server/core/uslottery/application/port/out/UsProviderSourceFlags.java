package com.tchalanet.server.core.uslottery.application.port.out;

import java.util.Map;

public record UsProviderSourceFlags(
    String origin,
    String sourceHash,
    String url,
    Map<String, String> metadata) {
}
