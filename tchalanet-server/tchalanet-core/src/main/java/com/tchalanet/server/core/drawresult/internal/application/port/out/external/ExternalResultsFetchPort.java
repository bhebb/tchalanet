package com.tchalanet.server.core.drawresult.internal.application.port.out.external;

public interface ExternalResultsFetchPort {

    ExternalResultFetchBundle fetchProviderResults(ExternalResultFetchQuery query);
}
