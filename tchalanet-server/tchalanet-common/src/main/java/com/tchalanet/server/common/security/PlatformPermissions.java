package com.tchalanet.server.common.security;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlatformPermissions {
    public static final String TENANT_OVERRIDE = "platform.tenant.override";

    /** Read archived entities (tickets, payouts, audit) via the archive lookup index. */
    public static final String ARCHIVE_READ = "archive.read";

    /** Trigger a platform archive run (SUPER_ADMIN scope). */
    public static final String ARCHIVE_RUN = "archive.run";

    /** Initiate a platform restore from archive (SUPER_ADMIN scope). */
    public static final String ARCHIVE_RESTORE = "archive.restore";

    /** List archive objects and run metadata (SUPER_ADMIN scope). */
    public static final String ARCHIVE_OBJECT_LIST = "archive.objects.list";
}
