package com.tchalanet.server.platform.keymanagement.api.model;

import java.util.List;

public record BackendPublicKeySetView(
    String activeKeyId,
    List<BackendPublicKeyView> keys
) {}
