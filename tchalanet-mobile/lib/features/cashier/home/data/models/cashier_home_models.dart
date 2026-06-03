class CashierHomeHeader {
  const CashierHomeHeader({required this.title, this.subtitle});

  final String title;
  final String? subtitle;

  factory CashierHomeHeader.fromJson(Map<String, dynamic> json) =>
      CashierHomeHeader(
        title: json['title'] as String? ?? '',
        subtitle: json['subtitle'] as String?,
      );
}

class CashierHomeRequiredStep {
  const CashierHomeRequiredStep({
    required this.type,
    required this.title,
    required this.message,
  });

  // Known types: SELECT_OPERATIONAL_CONTEXT, OPEN_SESSION
  final String type;
  final String title;
  final String message;

  factory CashierHomeRequiredStep.fromJson(Map<String, dynamic> json) =>
      CashierHomeRequiredStep(
        type: json['type'] as String? ?? '',
        title: json['title'] as String? ?? '',
        message: json['message'] as String? ?? '',
      );
}

class CashierHomeOpCtx {
  const CashierHomeOpCtx({
    required this.ready,
    required this.trusted,
    required this.missing,
    this.source,
    this.outletId,
    this.outletName,
    this.terminalId,
    this.terminalLabel,
    this.salesSessionId,
  });

  final bool ready;
  final bool trusted;
  final String? source;
  final String? outletId;
  final String? outletName;
  final String? terminalId;
  final String? terminalLabel;
  final String? salesSessionId;
  final List<String> missing;

  factory CashierHomeOpCtx.fromJson(Map<String, dynamic> json) =>
      CashierHomeOpCtx(
        ready: json['ready'] as bool? ?? false,
        trusted: json['trusted'] as bool? ?? false,
        source: json['source'] as String?,
        outletId: json['outletId'] as String?,
        outletName: json['outletName'] as String?,
        terminalId: json['terminalId'] as String?,
        terminalLabel: json['terminalLabel'] as String?,
        salesSessionId: json['salesSessionId'] as String?,
        missing: (json['missing'] as List<dynamic>?)
                ?.map((e) => e as String)
                .toList() ??
            [],
      );
}

class CashierHomeSession {
  const CashierHomeSession({
    required this.open,
    required this.ticketCount,
    this.openedAt,
    this.openedAtLabel,
    this.salesTotal,
  });

  final bool open;
  final int ticketCount;
  final DateTime? openedAt;
  final String? openedAtLabel;
  // Formatted by server, e.g. "42,850.00 HTG"
  final String? salesTotal;

  factory CashierHomeSession.fromJson(Map<String, dynamic> json) =>
      CashierHomeSession(
        open: json['open'] as bool? ?? false,
        ticketCount: (json['ticketCount'] as num?)?.toInt() ?? 0,
        openedAt: json['openedAt'] != null
            ? DateTime.tryParse(json['openedAt'] as String)
            : null,
        openedAtLabel: json['openedAtLabel'] as String?,
        salesTotal: json['salesTotal'] as String?,
      );
}

class CashierHomeDrawSummary {
  const CashierHomeDrawSummary({
    this.drawId,
    this.drawChannelId,
    this.label,
    this.scheduledAt,
    this.scheduledAtLabel,
    this.cutoffAt,
    this.cutoffLabel,
    this.status,
  });

  final String? drawId;
  final String? drawChannelId;
  final String? label;
  final DateTime? scheduledAt;
  final String? scheduledAtLabel;
  final DateTime? cutoffAt;
  final String? cutoffLabel;
  final String? status;

  factory CashierHomeDrawSummary.fromJson(Map<String, dynamic> json) =>
      CashierHomeDrawSummary(
        drawId: json['drawId'] as String?,
        drawChannelId: json['drawChannelId'] as String?,
        label: json['label'] as String?,
        scheduledAt: json['scheduledAt'] != null
            ? DateTime.tryParse(json['scheduledAt'] as String)
            : null,
        scheduledAtLabel: json['scheduledAtLabel'] as String?,
        cutoffAt: json['cutoffAt'] != null
            ? DateTime.tryParse(json['cutoffAt'] as String)
            : null,
        cutoffLabel: json['cutoffLabel'] as String?,
        status: json['status'] as String?,
      );
}

class HomeAction {
  const HomeAction({
    required this.type,
    required this.label,
    required this.enabled,
    required this.route,
  });

  final String type;
  final String label;
  final bool enabled;
  final String route;

  factory HomeAction.fromJson(Map<String, dynamic> json) => HomeAction(
        type: json['type'] as String? ?? '',
        label: json['label'] as String? ?? '',
        enabled: json['enabled'] as bool? ?? false,
        route: json['route'] as String? ?? '',
      );
}

class HomeWidget {
  const HomeWidget({
    required this.key,
    required this.type,
    required this.data,
    this.title,
  });

  final String key;
  final String? title;
  final String type;
  final Map<String, dynamic> data;

  factory HomeWidget.fromJson(Map<String, dynamic> json) => HomeWidget(
        key: json['key'] as String? ?? '',
        title: json['title'] as String?,
        type: json['type'] as String? ?? '',
        data: (json['data'] as Map<String, dynamic>?) ?? {},
      );
}

class HomeNavigationItem {
  const HomeNavigationItem({
    required this.key,
    required this.label,
    required this.route,
  });

  final String key;
  final String label;
  final String route;

  factory HomeNavigationItem.fromJson(Map<String, dynamic> json) =>
      HomeNavigationItem(
        key: json['key'] as String? ?? '',
        label: json['label'] as String? ?? '',
        route: json['route'] as String? ?? '',
      );
}

class CashierHomeResponse {
  const CashierHomeResponse({
    required this.quickActions,
    required this.widgets,
    required this.navigation,
    required this.notices,
    this.surface,
    this.version,
    this.header,
    this.requiredStep,
    this.operationalContext,
    this.session,
    this.primaryDraw,
    this.primaryAction,
  });

  final String? surface;
  final String? version;
  final CashierHomeHeader? header;
  final CashierHomeRequiredStep? requiredStep;
  final CashierHomeOpCtx? operationalContext;
  final CashierHomeSession? session;
  final CashierHomeDrawSummary? primaryDraw;
  final HomeAction? primaryAction;
  final List<HomeAction> quickActions;
  final List<HomeWidget> widgets;
  final List<HomeNavigationItem> navigation;
  final List<String> notices;

  bool get isOperational => requiredStep == null;
  bool get needsOpContext =>
      requiredStep?.type == 'SELECT_OPERATIONAL_CONTEXT';
  bool get needsSession => requiredStep?.type == 'OPEN_SESSION';

  factory CashierHomeResponse.fromJson(Map<String, dynamic> json) =>
      CashierHomeResponse(
        surface: json['surface'] as String?,
        version: json['version'] as String?,
        header: json['header'] != null
            ? CashierHomeHeader.fromJson(json['header'] as Map<String, dynamic>)
            : null,
        requiredStep: json['requiredStep'] != null
            ? CashierHomeRequiredStep.fromJson(
                json['requiredStep'] as Map<String, dynamic>)
            : null,
        operationalContext: json['operationalContext'] != null
            ? CashierHomeOpCtx.fromJson(
                json['operationalContext'] as Map<String, dynamic>)
            : null,
        session: json['session'] != null
            ? CashierHomeSession.fromJson(
                json['session'] as Map<String, dynamic>)
            : null,
        primaryDraw: json['primaryDraw'] != null
            ? CashierHomeDrawSummary.fromJson(
                json['primaryDraw'] as Map<String, dynamic>)
            : null,
        primaryAction: json['primaryAction'] != null
            ? HomeAction.fromJson(
                json['primaryAction'] as Map<String, dynamic>)
            : null,
        quickActions: (json['quickActions'] as List<dynamic>?)
                ?.map((e) => HomeAction.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        widgets: (json['widgets'] as List<dynamic>?)
                ?.map((e) => HomeWidget.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        navigation: (json['navigation'] as List<dynamic>?)
                ?.map((e) =>
                    HomeNavigationItem.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        notices: (json['notices'] as List<dynamic>?)
                ?.map((e) => e as String)
                .toList() ??
            [],
      );
}

// ─── Readiness models ─────────────────────────────────────────────────────────

enum CashierAttentionLevel { none, badge, card, blocked }

CashierAttentionLevel _parseAttentionLevel(String? raw) =>
    CashierAttentionLevel.values.firstWhere(
      (e) => e.name.toUpperCase() == raw?.toUpperCase(),
      orElse: () => CashierAttentionLevel.none,
    );

class CashierReadinessBlocker {
  const CashierReadinessBlocker({
    required this.type,
    required this.titleKey,
    required this.messageKey,
    required this.params,
  });

  final String type;
  final String titleKey;
  final String messageKey;
  final Map<String, dynamic> params;

  factory CashierReadinessBlocker.fromJson(Map<String, dynamic> json) =>
      CashierReadinessBlocker(
        type: json['type'] as String? ?? '',
        titleKey: json['titleKey'] as String? ?? '',
        messageKey: json['messageKey'] as String? ?? '',
        params: (json['params'] as Map<String, dynamic>?) ?? {},
      );
}

class CashierBadge {
  const CashierBadge({
    required this.type,
    required this.attentionLevel,
    required this.titleKey,
    required this.params,
  });

  final String type;
  final CashierAttentionLevel attentionLevel;
  final String titleKey;
  final Map<String, dynamic> params;

  factory CashierBadge.fromJson(Map<String, dynamic> json) => CashierBadge(
        type: json['type'] as String? ?? '',
        attentionLevel: _parseAttentionLevel(json['attentionLevel'] as String?),
        titleKey: json['titleKey'] as String? ?? '',
        params: (json['params'] as Map<String, dynamic>?) ?? {},
      );
}

class CashierNotification {
  const CashierNotification({
    required this.type,
    required this.attentionLevel,
    required this.titleKey,
    required this.messageKey,
    required this.params,
    this.actionType,
    this.actionKey,
  });

  final String type;
  final CashierAttentionLevel attentionLevel;
  final String titleKey;
  final String messageKey;
  final String? actionType;
  final String? actionKey;
  final Map<String, dynamic> params;

  factory CashierNotification.fromJson(Map<String, dynamic> json) =>
      CashierNotification(
        type: json['type'] as String? ?? '',
        attentionLevel: _parseAttentionLevel(json['attentionLevel'] as String?),
        titleKey: json['titleKey'] as String? ?? '',
        messageKey: json['messageKey'] as String? ?? '',
        actionType: json['actionType'] as String?,
        actionKey: json['actionKey'] as String?,
        params: (json['params'] as Map<String, dynamic>?) ?? {},
      );
}

class CashierReadinessResponse {
  const CashierReadinessResponse({
    required this.ready,
    required this.attentionLevel,
    required this.badges,
    required this.notifications,
    required this.blockers,
  });

  final bool ready;
  final CashierAttentionLevel attentionLevel;
  final List<CashierBadge> badges;
  final List<CashierNotification> notifications;
  final List<CashierReadinessBlocker> blockers;

  factory CashierReadinessResponse.fromJson(Map<String, dynamic> json) =>
      CashierReadinessResponse(
        ready: json['ready'] as bool? ?? false,
        attentionLevel:
            _parseAttentionLevel(json['attentionLevel'] as String?),
        badges: (json['badges'] as List<dynamic>?)
                ?.map((e) =>
                    CashierBadge.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        notifications: (json['notifications'] as List<dynamic>?)
                ?.map((e) =>
                    CashierNotification.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        blockers: (json['blockers'] as List<dynamic>?)
                ?.map((e) =>
                    CashierReadinessBlocker.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
      );
}
