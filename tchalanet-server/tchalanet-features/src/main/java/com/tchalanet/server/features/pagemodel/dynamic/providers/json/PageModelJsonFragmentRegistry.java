package com.tchalanet.server.features.pagemodel.dynamic.providers.json;

import com.tchalanet.server.core.pagemodel.api.dynamic.PageModelDynamicProviderException;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PageModelJsonFragmentRegistry {

  private static final Map<String, String> FRAGMENTS =
      Map.ofEntries(
          // Public shell fragments (v2 typed NavigationDestination printOptionsRequest)
          Map.entry("public_header_links", "pagemodel/fragments/public/public_header_links.json"),
          Map.entry("public_footer_links", "pagemodel/fragments/public/public_footer_links.json"),
          // Public content fragments
          Map.entry("public_hero", "pagemodel/fragments/public/public_hero.json"),
          Map.entry("public_features", "pagemodel/fragments/public/public_features.json"),
          Map.entry("public_tchala", "pagemodel/fragments/public/public_tchala.json"),
          // Private shell fragments (v2 typed — topAppBar + navigationDrawer per surface)
          Map.entry("private_shell_cashier", "pagemodel/fragments/private/cashier/private_shell_cashier.json"),
          Map.entry("private_shell_tenantadmin", "pagemodel/fragments/private/tenantadmin/private_shell_tenantadmin.json"),
          Map.entry("private_shell_tenant_admin", "pagemodel/fragments/private/tenantadmin/private_shell_tenantadmin.json"),
          Map.entry("private_shell_superadmin", "pagemodel/fragments/private/superadmin/private_shell_superadmin.json"),
          Map.entry("private_shell_super_admin", "pagemodel/fragments/private/superadmin/private_shell_superadmin.json"),
          // Private action fragments
          Map.entry(
              "private_cashier_quick_actions",
              "pagemodel/fragments/private/cashier/private_quick_actions_cashier.json"));

  public String resolve(String fileKey) {
    if (fileKey == null || fileKey.isBlank()) {
      throw new PageModelDynamicProviderException("MISSING_PROP", "props.fileKey is required");
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
