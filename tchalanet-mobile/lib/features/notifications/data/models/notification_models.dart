enum NotificationSeverity { info, warning, error, critical }

enum NotificationKind { info, warning, actionRequired, systemError }

enum NotificationCategory {
  pageModel,
  tenantConfig,
  user,
  outlet,
  terminal,
  session,
  sales,
  draw,
  result,
  payout,
  batch,
  system,
  security,
}

enum NotificationStatus { unread, read, archived, expired }

class NotificationAction {
  const NotificationAction({required this.type, required this.url});

  final String type;
  final String url;

  factory NotificationAction.fromJson(Map<String, dynamic> json) =>
      NotificationAction(
        type: json['type']?.toString() ?? '',
        url: json['url']?.toString() ?? '',
      );
}

class NotificationItem {
  const NotificationItem({
    required this.id,
    required this.severity,
    required this.kind,
    required this.category,
    required this.status,
    required this.createdAt,
    this.titleKey,
    this.messageKey,
    this.titleText,
    this.messageText,
    this.payload = const {},
    this.action,
    this.readAt,
    this.archivedAt,
    this.expiresAt,
  });

  final String id;
  final NotificationSeverity severity;
  final NotificationKind kind;
  final NotificationCategory category;
  final String? titleKey;
  final String? messageKey;
  final String? titleText;
  final String? messageText;
  final Map<String, Object?> payload;
  final NotificationAction? action;
  final NotificationStatus status;
  final DateTime? readAt;
  final DateTime? archivedAt;
  final DateTime? expiresAt;
  final DateTime createdAt;

  factory NotificationItem.fromJson(Map<String, dynamic> json) =>
      NotificationItem(
        id: json['id']?.toString() ?? '',
        severity: _enumByServerName(
          NotificationSeverity.values,
          json['severity'],
          NotificationSeverity.info,
        ),
        kind: _enumByServerName(
          NotificationKind.values,
          json['kind'],
          NotificationKind.info,
        ),
        category: _enumByServerName(
          NotificationCategory.values,
          json['category'],
          NotificationCategory.system,
        ),
        titleKey: json['titleKey']?.toString(),
        messageKey: json['messageKey']?.toString(),
        titleText: json['titleText']?.toString(),
        messageText: json['messageText']?.toString(),
        payload: Map<String, Object?>.from(json['payload'] as Map? ?? const {}),
        action: json['action'] is Map
            ? NotificationAction.fromJson(
                Map<String, dynamic>.from(json['action'] as Map),
              )
            : null,
        status: _enumByServerName(
          NotificationStatus.values,
          json['status'],
          NotificationStatus.unread,
        ),
        readAt: _date(json['readAt']),
        archivedAt: _date(json['archivedAt']),
        expiresAt: _date(json['expiresAt']),
        createdAt:
            _date(json['createdAt']) ?? DateTime.fromMillisecondsSinceEpoch(0),
      );

  NotificationItem copyWith({
    NotificationStatus? status,
    DateTime? readAt,
    DateTime? archivedAt,
  }) => NotificationItem(
    id: id,
    severity: severity,
    kind: kind,
    category: category,
    titleKey: titleKey,
    messageKey: messageKey,
    titleText: titleText,
    messageText: messageText,
    payload: payload,
    action: action,
    status: status ?? this.status,
    readAt: readAt ?? this.readAt,
    archivedAt: archivedAt ?? this.archivedAt,
    expiresAt: expiresAt,
    createdAt: createdAt,
  );
}

class NotificationSummary {
  const NotificationSummary({
    required this.unreadCount,
    required this.criticalCount,
    required this.actionRequiredCount,
    required this.hasActionRequired,
  });

  final int unreadCount;
  final int criticalCount;
  final int actionRequiredCount;
  final bool hasActionRequired;

  static const empty = NotificationSummary(
    unreadCount: 0,
    criticalCount: 0,
    actionRequiredCount: 0,
    hasActionRequired: false,
  );

  factory NotificationSummary.fromJson(Map<String, dynamic> json) =>
      NotificationSummary(
        unreadCount: (json['unreadCount'] as num?)?.toInt() ?? 0,
        criticalCount: (json['criticalCount'] as num?)?.toInt() ?? 0,
        actionRequiredCount:
            (json['actionRequiredCount'] as num?)?.toInt() ?? 0,
        hasActionRequired: json['hasActionRequired'] as bool? ?? false,
      );
}

class NotificationPage {
  const NotificationPage({
    required this.items,
    required this.page,
    required this.totalPages,
    required this.hasNext,
  });

  final List<NotificationItem> items;
  final int page;
  final int totalPages;
  final bool hasNext;

  factory NotificationPage.fromJson(Map<String, dynamic> json) =>
      NotificationPage(
        items: [
          for (final item in json['items'] as List? ?? const [])
            NotificationItem.fromJson(Map<String, dynamic>.from(item as Map)),
        ],
        page: (json['page'] as num?)?.toInt() ?? 0,
        totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
        hasNext: json['hasNext'] as bool? ?? false,
      );
}

T _enumByServerName<T extends Enum>(List<T> values, Object? raw, T fallback) {
  final normalized = raw?.toString().toLowerCase().replaceAll('_', '');
  for (final value in values) {
    if (value.name.toLowerCase() == normalized) return value;
  }
  return fallback;
}

DateTime? _date(Object? value) =>
    value == null ? null : DateTime.tryParse(value.toString());
