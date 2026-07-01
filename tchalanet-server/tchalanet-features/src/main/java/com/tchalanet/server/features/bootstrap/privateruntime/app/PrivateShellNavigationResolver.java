package com.tchalanet.server.features.bootstrap.privateruntime.app;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateBootstrapSpace;
import com.tchalanet.server.features.pagemodel.dynamic.providers.json.PageModelJsonFragmentRegistry;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

@Component
@RequiredArgsConstructor
class PrivateShellNavigationResolver {

    private final PageModelJsonFragmentRegistry fragmentRegistry;
    private final JsonUtils jsonUtils;
    private final ConcurrentMap<PrivateBootstrapSpace, Map<String, Object>> cache = new ConcurrentHashMap<>();

    Map<String, Object> resolve(PrivateBootstrapSpace space) {
        return cache.computeIfAbsent(space, this::load);
    }

    private Map<String, Object> load(PrivateBootstrapSpace space) {
        String fileKey = switch (space) {
            case ADMIN -> "private_shell_tenantadmin";
            case PLATFORM -> "private_shell_superadmin";
            case CASHIER -> "private_shell_cashier";
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
                throw new IllegalStateException("Private shell fragment has no navigationDrawer: " + fileKey);
            }
            return jsonUtils.convertValue(navigation, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load private shell navigation: " + fileKey, e);
        }
    }
}
