package com.tchalanet.server.features.pagemodel.shared.init;

import java.util.UUID;

public final class PageModelTenantConstants {

    private PageModelTenantConstants() {
    }

    // TODO: ajuster cette valeur en fonction de la stratégie de tenant "par défaut"
    public static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
}

