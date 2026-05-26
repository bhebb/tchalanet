package com.tchalanet.server.features.pagemodel.dynamic.providers.json;

import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PageModelJsonFragmentRegistry {

  private static final Map<String, String> FRAGMENTS =
      Map.ofEntries(
          Map.entry("public_header_links", "pagemodel/fragments/public/header.links.json"),
          Map.entry("public_footer_links", "pagemodel/fragments/public/footer.links.json"),
          Map.entry("private_footer_links", "pagemodel/fragments/private/footer.links.json"),
          Map.entry("private_header_cashier", "pagemodel/fragments/private/cashier/header.links.json"),
          Map.entry("private_sidebar_cashier", "pagemodel/fragments/private/cashier/sidebar.links.json"),
          Map.entry(
              "private_cashier_quick_actions",
              "pagemodel/fragments/private/cashier/quick_actions.links.json"));

  public String resolve(String fileKey) {
    if (fileKey == null || fileKey.isBlank()) {
      throw new PageModelDynamicProviderException("MISSING_PROP", "props.file_key is required");
    }

    String resourcePath = FRAGMENTS.get(fileKey);
    if (resourcePath == null) {
      throw new PageModelDynamicProviderException(
          "JSON_FRAGMENT_NOT_FOUND", "Unknown JSON fragment key: " + fileKey);
    }

    return resourcePath;
  }

  public boolean contains(String fileKey) {
    return FRAGMENTS.containsKey(fileKey);
  }
}
