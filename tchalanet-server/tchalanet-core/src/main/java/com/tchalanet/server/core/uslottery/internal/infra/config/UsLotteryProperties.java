package com.tchalanet.server.core.uslottery.internal.infra.config;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "tch.us-lottery")
@ToString
public class UsLotteryProperties {

  private boolean enabled = true;

  private Map<String, ProviderProperties> providers;
  private CommonProperties common;

  /**
   * Optional Cloudflare Worker relay for providers whose base URL is blocked at the
   * datacenter/ASN level when called directly (see tchalanet-infra/cloudflare-worker-proxy).
   * Requests for a provider with {@code proxied: true} are rewritten to
   * {@code GET <proxyUrl>?url=<encoded original url>} with original headers forwarded under an
   * {@code X-Fwd-} prefix and {@code X-Proxy-Secret: <proxySecret>} added.
   */
  private String proxyUrl;

  private String proxySecret;

  @Getter
  @Setter
  public static class CommonProperties {
    private List<String> holidays;
  }

  @Getter
  @Setter
  @ToString
  public static class ProviderProperties {
    private boolean enabled = true;
    private String baseUrl;
    private String appToken;
    private String bearerToken;

    private String authBaseUrl;
    private String authPath;
    private String authUsername;
    private String authPassword;

    private String fallbackBaseUrl;
    private String fallbackPath;
    private String timezone;
    private String latestPath;
    private String alertPath;
    private Map<String, String> headers;
    private List<String> holidays;
    private boolean proxied = false;
  }
}
