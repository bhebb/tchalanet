package com.tchalanet.server.core.offlinesync.application.service;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSaleSubmission;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OfflineSalesPayloadMapper {

  public List<OfflineSaleSubmission> mapSubmissions(OfflineBatchId batchId, List<OfflineSaleSubmission> submissions) {
    // Mapping to Sales typed command inputs will be completed when Sales sync model is finalized.
    return submissions;
  }
}

