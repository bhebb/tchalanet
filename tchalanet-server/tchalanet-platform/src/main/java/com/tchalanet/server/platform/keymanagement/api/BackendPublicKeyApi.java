package com.tchalanet.server.platform.keymanagement.api;

import com.tchalanet.server.platform.keymanagement.api.model.BackendPublicKeySetView;

public interface BackendPublicKeyApi {

    BackendPublicKeySetView listActivePublicKeys();
}
