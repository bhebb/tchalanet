package com.tchalanet.server.common.job.starter;

import com.tchalanet.server.common.job.key.JobKey;
import com.tchalanet.server.common.job.model.BatchJobStartResult;

import java.util.Map;

public interface BatchJobStarter {

    BatchJobStartResult start(JobKey jobKey, Map<String, String> params);
}
