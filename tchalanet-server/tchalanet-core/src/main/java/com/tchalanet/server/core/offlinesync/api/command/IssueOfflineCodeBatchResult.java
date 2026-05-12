package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import java.util.List;

public record IssueOfflineCodeBatchResult(OfflineCodeBatchId codeBatchId, List<String> codes) {}

