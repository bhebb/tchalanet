package com.tchalanet.server.common.job.launch;

import com.tchalanet.server.common.job.key.JobKey;

import java.util.Map;

public interface BatchJobStarter {

    JobStartResult start(JobKey jobKey, Map<String, String> params);
}
