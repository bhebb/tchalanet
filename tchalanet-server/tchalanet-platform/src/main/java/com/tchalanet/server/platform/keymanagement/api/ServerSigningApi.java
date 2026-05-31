package com.tchalanet.server.platform.keymanagement.api;

import com.tchalanet.server.platform.keymanagement.api.model.ServerSignatureResult;
import com.tchalanet.server.platform.keymanagement.api.model.ServerSigningPurpose;

public interface ServerSigningApi {

    ServerSignatureResult sign(ServerSigningPurpose purpose, byte[] canonicalPayload);
}
