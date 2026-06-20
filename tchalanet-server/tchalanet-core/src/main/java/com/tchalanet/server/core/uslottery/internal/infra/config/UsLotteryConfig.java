package com.tchalanet.server.core.uslottery.internal.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = "tch.us-lottery",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class UsLotteryConfig {

    private static final String NEW_YORK_PROVIDER_KEY = "ny";
    private static final String FLORIDA_LOTTERY_PROVIDER = "fl";
    private static final String GA_PROVIDER_KEY = "ga";
    private static final String TENNESSEE_LOTTERY_KEY = "tn";
    private static final String TX_PROVIDER_KEY = "tx";
    private static final String PA_PROVIDER_KEY = "pa";
    private static final String NJ_PROVIDER_KEY = "nj";
    private static final String CA_PROVIDER_KEY = "ca";
    private static final String OH_PROVIDER_KEY = "oh";
    private static final String MI_PROVIDER_KEY = "mi";

    @Bean
    public RestClient.Builder restClientBuilder(RestClientFactory factory) {
        return factory.builder();
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.ny",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient nyLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {
        return getRestClient(builder, props, NEW_YORK_PROVIDER_KEY);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.fl",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient floridaLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {

        return getRestClient(builder, props, FLORIDA_LOTTERY_PROVIDER);

    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.ga",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient gaLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {
        return getRestClient(builder, props, GA_PROVIDER_KEY);

    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.tn",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient tnLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {
        return getRestClient(builder, props, TENNESSEE_LOTTERY_KEY);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.tx",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient txLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {
        return getRestClient(builder, props, TX_PROVIDER_KEY);

    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.pa",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient paLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {
        return getRestClient(builder, props, PA_PROVIDER_KEY);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.nj",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient njLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {
        return getRestClient(builder, props, NJ_PROVIDER_KEY);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.ca",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient caLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {
        return getRestClient(builder, props, CA_PROVIDER_KEY);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.oh",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient ohLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {
        return getRestClient(builder, props, OH_PROVIDER_KEY);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.mi",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient miLotteryRestClient(RestClientFactory builder, UsLotteryProperties props) {
        return getRestClient(builder, props, MI_PROVIDER_KEY);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "tch.us-lottery.providers.oh",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
    public RestClient ohAuthRestClient(RestClientFactory factory, UsLotteryProperties props) {
        var p = props.getProviders() != null ? props.getProviders().get(OH_PROVIDER_KEY) : null;
        var baseUrl = p != null ? p.getAuthBaseUrl() : null;

        log.info("Configuring Ohio auth RestClient with base URL: {} et prop:  {}", baseUrl, p);
        return factory.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Accept", "application/json, text/plain, */*")
            .defaultHeader("Origin", "https://www.ohiolottery.com")
            .defaultHeader("Referer", "https://www.ohiolottery.com/")
            .build();
    }

    private RestClient getRestClient(RestClientFactory factory, UsLotteryProperties props, String providerKey) {
        var p = props.getProviders() != null ? props.getProviders().get(providerKey) : null;
        var baseUrl = p != null ? p.getBaseUrl() : null;

        var b = (baseUrl == null || baseUrl.isBlank())
            ? factory.builder()
            : factory.builder().baseUrl(baseUrl);

        var headers = p == null || p.getHeaders() == null ? Map.<String, String>of() : p.getHeaders();
        for (var e : headers.entrySet()) {
            b = b.defaultHeader(e.getKey(), e.getValue());
        }

        return b.build();
    }

}
