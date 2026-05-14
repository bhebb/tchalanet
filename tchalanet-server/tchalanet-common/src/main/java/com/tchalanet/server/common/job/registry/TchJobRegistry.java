package com.tchalanet.server.common.job.registry;

import com.tchalanet.server.common.job.key.JobKey;
import java.util.Collection;
import java.util.Optional;

public interface TchJobRegistry {

    Collection<RegisteredJob> list();

    Optional<RegisteredJob> find(JobKey key);
}
