enum RuntimeScope { public, tenant }

enum RuntimeStatus { ready, partial, blocked, forceReload, sessionExpired }

class RuntimeNotificationSummary {
  const RuntimeNotificationSummary({
    this.unreadCount = 0,
    this.criticalCount = 0,
  });

  final int unreadCount;
  final int criticalCount;

  factory RuntimeNotificationSummary.fromJson(Map<String, dynamic> json) =>
      RuntimeNotificationSummary(
        unreadCount: (json['unreadCount'] as num?)?.toInt() ?? 0,
        criticalCount: (json['criticalCount'] as num?)?.toInt() ?? 0,
      );
}

class RuntimeNotice {
  const RuntimeNotice({
    required this.code,
    required this.message,
    required this.level,
  });

  final String code;
  final String message;
  final String level;

  factory RuntimeNotice.fromJson(Map<String, dynamic> json) => RuntimeNotice(
    code: json['code']?.toString() ?? 'runtime.notice',
    message: json['message']?.toString() ?? '',
    level: json['level']?.toString() ?? 'INFO',
  );
}

class RuntimeVersions {
  const RuntimeVersions({
    required this.bootstrap,
    this.navigation,
    this.entitlements,
    this.theme,
    this.i18n,
    this.settings,
  });

  final String bootstrap;
  final String? navigation;
  final String? entitlements;
  final String? theme;
  final String? i18n;
  final String? settings;

  factory RuntimeVersions.fromJson(Map<String, dynamic> json) =>
      RuntimeVersions(
        bootstrap: json['bootstrapVersion']?.toString() ?? '',
        navigation: json['navigationVersion']?.toString(),
        entitlements: json['entitlementsVersion']?.toString(),
        theme: json['themeVersion']?.toString(),
        i18n: json['i18nVersion']?.toString(),
        settings: json['settingsVersion']?.toString(),
      );

  @override
  bool operator ==(Object other) =>
      other is RuntimeVersions &&
      bootstrap == other.bootstrap &&
      navigation == other.navigation &&
      entitlements == other.entitlements &&
      theme == other.theme &&
      i18n == other.i18n &&
      settings == other.settings;

  @override
  int get hashCode =>
      Object.hash(bootstrap, navigation, entitlements, theme, i18n, settings);
}

class RuntimeBootstrap {
  const RuntimeBootstrap({
    required this.scope,
    required this.locale,
    required this.i18nMessages,
    required this.features,
    required this.roles,
    required this.permissions,
    required this.readinessStatus,
    required this.notifications,
    required this.notices,
    this.user,
    this.tenantContext,
  });

  final RuntimeScope scope;
  final String locale;
  final Map<String, String> i18nMessages;
  final Map<String, bool> features;
  final Set<String> roles;
  final Set<String> permissions;
  final String readinessStatus;
  final RuntimeNotificationSummary notifications;
  final List<RuntimeNotice> notices;
  final RuntimeUser? user;
  final RuntimeTenantContext? tenantContext;

  bool hasFeature(String key, {bool safeDefault = false}) =>
      features[key] ?? safeDefault;

  bool hasPermission(String permission) => permissions.contains(permission);

  factory RuntimeBootstrap.fromJson(
    Map<String, dynamic> json, {
    required RuntimeScope scope,
  }) {
    final settings = _map(json['settings']);
    final i18n = _map(json['i18n']);
    final entitlements = _map(json['entitlements']);
    return RuntimeBootstrap(
      scope: scope,
      locale:
          (i18n['locale'] ?? i18n['lang'] ?? settings['locale'])?.toString() ??
          'ht',
      i18nMessages: _stringMap(i18n['messages']),
      features: _boolMap(settings['features']),
      roles: _stringSet(entitlements['roles']),
      permissions: _stringSet(entitlements['permissions']),
      readinessStatus: _map(json['readiness'])['status']?.toString() ?? 'READY',
      notifications: RuntimeNotificationSummary.fromJson(
        _map(json['notifications']),
      ),
      notices: _notices(json['notices']),
      user: scope == RuntimeScope.tenant
          ? RuntimeUser.fromJson(_map(json['user']))
          : null,
      tenantContext:
          scope == RuntimeScope.tenant && json['tenantContext'] != null
          ? RuntimeTenantContext.fromJson(_map(json['tenantContext']))
          : null,
    );
  }
}

class RuntimeUser {
  const RuntimeUser({this.userId, this.username, this.displayName, this.email});

  final String? userId;
  final String? username;
  final String? displayName;
  final String? email;

  factory RuntimeUser.fromJson(Map<String, dynamic> json) => RuntimeUser(
    userId: _nullableString(json['userId']),
    username: _nullableString(json['username']),
    displayName: _nullableString(json['displayName']),
    email: _nullableString(json['email']),
  );
}

class RuntimeTenantContext {
  const RuntimeTenantContext({required this.tenantId, this.tenantCode});

  final String tenantId;
  final String? tenantCode;

  factory RuntimeTenantContext.fromJson(Map<String, dynamic> json) =>
      RuntimeTenantContext(
        tenantId: json['tenantId']?.toString() ?? '',
        tenantCode: _nullableString(json['tenantCode']),
      );
}

class RuntimeStateSnapshot {
  const RuntimeStateSnapshot({
    required this.status,
    required this.readinessStatus,
    required this.notifications,
    required this.versions,
    required this.notices,
  });

  final RuntimeStatus status;
  final String readinessStatus;
  final RuntimeNotificationSummary notifications;
  final RuntimeVersions versions;
  final List<RuntimeNotice> notices;

  factory RuntimeStateSnapshot.fromJson(Map<String, dynamic> json) =>
      RuntimeStateSnapshot(
        status: switch (json['status']?.toString()) {
          'PARTIAL' => RuntimeStatus.partial,
          'BLOCKED' => RuntimeStatus.blocked,
          'FORCE_RELOAD' => RuntimeStatus.forceReload,
          'SESSION_EXPIRED' => RuntimeStatus.sessionExpired,
          _ => RuntimeStatus.ready,
        },
        readinessStatus:
            _map(json['readiness'])['status']?.toString() ?? 'READY',
        notifications: RuntimeNotificationSummary.fromJson(
          _map(json['notifications']),
        ),
        versions: RuntimeVersions.fromJson(_map(json['versions'])),
        notices: _notices(json['notices']),
      );
}

Map<String, dynamic> _map(Object? value) =>
    value is Map ? Map<String, dynamic>.from(value) : const {};

Map<String, String> _stringMap(Object? value) {
  if (value is! Map) return const {};
  return Map.unmodifiable({
    for (final entry in value.entries)
      if (entry.value != null) entry.key.toString(): entry.value.toString(),
  });
}

Map<String, bool> _boolMap(Object? value) {
  if (value is! Map) return const {};
  return Map.unmodifiable({
    for (final entry in value.entries)
      if (entry.value is bool) entry.key.toString(): entry.value as bool,
  });
}

Set<String> _stringSet(Object? value) => Set.unmodifiable(
  value is List ? value.whereType<String>() : const <String>[],
);

List<RuntimeNotice> _notices(Object? value) => List.unmodifiable([
  for (final notice in value is List ? value : const [])
    if (notice is Map)
      RuntimeNotice.fromJson(Map<String, dynamic>.from(notice)),
]);

String? _nullableString(Object? value) {
  final text = value?.toString();
  return text == null || text.isEmpty ? null : text;
}
