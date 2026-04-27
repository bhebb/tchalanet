package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.config.ApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QrPayloadBuilder {
  private final ApiProperties apiProperties;

  public String ticketVerifyUrl(String publicCode) {
    String basePath = apiProperties.basePath();
    String apiVer = apiProperties.apiVersion();
    String bp = (basePath == null || basePath.isBlank()) ? "/api" : basePath.replaceAll("/+$", "");
    String av = (apiVer == null || apiVer.isBlank()) ? "v1" : apiVer.replaceAll("^/|/+$", "");

    String prefix = bp;
    if (!prefix.endsWith("/")) prefix = prefix + "/" + av;
    else prefix = prefix + av;

    if (!prefix.startsWith("/")) prefix = "/" + prefix;

    return prefix + "/tickets/verify/" + publicCode;
  }
}
