package com.tchalanet.server.features.bootstrap.privateruntime.app;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.pagemodel.api.model.PageModelDoc;
import com.tchalanet.server.core.pagemodel.api.query.ResolveEffectivePageModelQuery;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateBootstrapSpace;
import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicResolver;
import com.tchalanet.server.features.pagemodel.dynamic.providers.json.PageModelJsonFragmentRegistry;
import com.tchalanet.server.features.pagemodel.runtime.PageRuntimeAssembler;
import com.tchalanet.server.features.pagemodel.runtime.PageRuntimeResponse;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

@Slf4j
@Component
@RequiredArgsConstructor
class PrivateShellNavigationResolver {

  private final PageModelJsonFragmentRegistry fragmentRegistry;
  private final JsonUtils jsonUtils;
  private final QueryBus queryBus;
  private final PageModelDynamicResolver dynamicResolver;
  private final PageRuntimeAssembler runtimeAssembler;
  private final ConcurrentMap<PrivateBootstrapSpace, Map<String, Object>> fallbackCache =
      new ConcurrentHashMap<>();

  Map<String, Object> resolve(PrivateBootstrapSpace space) {
    return fallback(space);
  }

  Map<String, Object> resolve(PrivateBootstrapSpace space, TchRequestContext ctx, String lang) {
    // Cashier navigation is owned by the Flutter bottom navigation, not a web drawer fragment.
    if (space == PrivateBootstrapSpace.CASHIER) {
      return Map.of();
    }
    return fromPublishedPageModel(space, ctx, lang).orElseGet(() -> fallback(space));
  }

  private Optional<Map<String, Object>> fromPublishedPageModel(
      PrivateBootstrapSpace space, TchRequestContext ctx, String lang) {
    if (ctx == null) {
      return Optional.empty();
    }
    try {
      var logicalId = logicalId(space);
      var tenantId =
          space == PrivateBootstrapSpace.ADMIN
              ? Optional.ofNullable(TenantId.nullableOf(ctx.tenantUuid()))
              : Optional.<TenantId>empty();
      var doc = queryBus.ask(new ResolveEffectivePageModelQuery(tenantId, logicalId));
      var shellDoc = shellOnly(doc);
      var resolved = dynamicResolver.resolve(shellDoc, lang, ctx);
      var runtime = runtimeAssembler.assemble(shellDoc, resolved);
      if (runtime.shell() instanceof PageRuntimeResponse.PrivateShell privateShell) {
        return navigationDrawer(privateShell.navigationDrawer());
      }
      return Optional.empty();
    } catch (Exception e) {
      log.warn(
          "private.shell.navigation.pagemodel.resolve_failed space={} fallback=classpath",
          space,
          e);
      return Optional.empty();
    }
  }

  private Optional<Map<String, Object>> navigationDrawer(Object value) {
    if (value == null) {
      return Optional.empty();
    }
    Map<String, Object> drawer = jsonUtils.convertValue(value, new TypeReference<>() {});
    return drawer.isEmpty() ? Optional.empty() : Optional.of(drawer);
  }

  private PageModelDoc shellOnly(PageModelDoc doc) {
    return new PageModelDoc(
        doc.meta(), doc.theme(), doc.shell(), new PageModelDoc.Content(null, Map.of()));
  }

  private Map<String, Object> fallback(PrivateBootstrapSpace space) {
    if (space == PrivateBootstrapSpace.CASHIER) {
      return Map.of();
    }
    return fallbackCache.computeIfAbsent(space, this::load);
  }

  private String logicalId(PrivateBootstrapSpace space) {
    return switch (space) {
      case ADMIN -> "private.dashboard.tenant_admin";
      case PLATFORM -> "private.dashboard.superadmin";
      case CASHIER -> throw new IllegalStateException("Cashier navigation is resolved directly");
    };
  }

  private Map<String, Object> load(PrivateBootstrapSpace space) {
    String fileKey =
        switch (space) {
          case ADMIN -> "private_shell_tenantadmin";
          case PLATFORM -> "private_shell_superadmin";
          case CASHIER ->
              throw new IllegalStateException("Cashier navigation is resolved directly");
        };
    String resourcePath = fragmentRegistry.resolve(fileKey);
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalStateException("Private shell fragment not found: " + fileKey);
      }
      var root = jsonUtils.parse(is);
      var navigation = root == null ? null : root.get("navigationDrawer");
      if (navigation == null || !navigation.isObject()) {
        throw new IllegalStateException(
            "Private shell fragment has no navigationDrawer: " + fileKey);
      }
      return jsonUtils.convertValue(navigation, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load private shell navigation: " + fileKey, e);
    }
  }
}
