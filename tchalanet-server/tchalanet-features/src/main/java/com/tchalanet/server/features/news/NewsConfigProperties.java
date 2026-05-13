package com.tchalanet.server.features.news;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "tch.news")
public record NewsConfigProperties(
    Provider provider,
    Cache cache,
    Ttl ttl,
    Refresh refresh
) {

    public record Provider(
        URI url,
        String apiKey
    ) {
    }

    public record Cache(
        String key,
        String cacheName
    ) {
    }

    public record Ttl(
        long hours
    ) {
    }

    public record Refresh(
        String cron
    ) {
    }
}
