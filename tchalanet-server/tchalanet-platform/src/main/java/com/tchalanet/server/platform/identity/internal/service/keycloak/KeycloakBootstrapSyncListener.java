package com.tchalanet.server.platform.identity.internal.service.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "kc.bootstrap",
    name = "auto-run-on-startup",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class KeycloakBootstrapSyncListener {

    private final KeycloakBootstrapSyncService syncService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        var result = syncService.syncConfiguredUsers();
        log.info(
            "KC bootstrap startup sync finished attempted={} found={} updatedRows={}",
            result.attempted(),
            result.foundInKeycloak(),
            result.updatedRows());
    }
}
