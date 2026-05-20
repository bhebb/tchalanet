package com.tchalanet.server.common.job.registry;

import com.tchalanet.server.common.job.key.JobKey;
import java.util.Collection;
import java.util.Optional;

public interface TchJobRegistry {

    Optional<RegisteredJob> find(JobKey jobKey);

    Collection<RegisteredJob> list();
}
