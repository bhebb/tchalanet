import 'dart:math';

enum ClientDiagnosticCategory {
  api,
  connectivity,
  sale,
  print,
  scanner,
  printerConfig,
  flutter,
  async,
  device,
}

enum ClientDiagnosticSeverity { warn, error }

class ClientDiagnosticsPolicy {
  const ClientDiagnosticsPolicy({
    required this.enabled,
    required this.expiresAt,
    required this.maxEvents,
    required this.categories,
  });

  final bool enabled;
  final DateTime? expiresAt;
  final int maxEvents;
  final Set<ClientDiagnosticCategory> categories;

  bool allows(ClientDiagnosticCategory category, DateTime now) {
    final expiry = expiresAt;
    return enabled &&
        expiry != null &&
        now.isBefore(expiry) &&
        categories.contains(category);
  }

  factory ClientDiagnosticsPolicy.fromJson(Map<dynamic, dynamic> json) =>
      ClientDiagnosticsPolicy(
        enabled: json['enabled'] as bool? ?? false,
        expiresAt: DateTime.tryParse(
          json['expiresAt']?.toString() ?? '',
        )?.toUtc(),
        maxEvents: (json['maxEvents'] as num?)?.toInt() ?? 100,
        categories: (json['categories'] as List? ?? const [])
            .map((category) => _categoryFromWireName(category?.toString()))
            .toSet(),
      );
}

class ClientDiagnosticEvent {
  ClientDiagnosticEvent({
    String? eventId,
    required this.category,
    required this.occurredAtClient,
    required this.severity,
    required this.operation,
    this.errorCode,
    this.message,
    this.exceptionType,
    this.requestId,
    this.correlationId,
    this.httpStatus,
    this.endpointKey,
    this.appVersion,
    this.buildNumber,
    this.platform,
    this.deviceModel,
    this.osVersion,
    this.printerProvider,
    this.printerService,
    this.printerState,
    this.stackFrames = const [],
  }) : eventId = eventId ?? _eventId();

  final String eventId;
  final ClientDiagnosticCategory category;
  final DateTime occurredAtClient;
  final ClientDiagnosticSeverity severity;
  final String operation;
  final String? errorCode;
  final String? message;
  final String? exceptionType;
  final String? requestId;
  final String? correlationId;
  final int? httpStatus;
  final String? endpointKey;
  final String? appVersion;
  final String? buildNumber;
  final String? platform;
  final String? deviceModel;
  final String? osVersion;
  final String? printerProvider;
  final String? printerService;
  final String? printerState;
  final List<ClientDiagnosticStackFrame> stackFrames;

  String get fingerprint => [
    category.name,
    severity.name,
    operation,
    errorCode ?? '',
    exceptionType ?? '',
    httpStatus?.toString() ?? '',
    endpointKey ?? '',
    printerProvider ?? '',
    printerService ?? '',
    printerState ?? '',
  ].join('|');

  Map<String, dynamic> toJson() => {
    'eventId': eventId,
    'category': category.wireName,
    'occurredAtClient': occurredAtClient.toUtc().toIso8601String(),
    'severity': severity.name.toUpperCase(),
    'operation': operation,
    if (errorCode != null) 'errorCode': errorCode,
    if (message != null) 'message': _limit(message!, 512),
    if (exceptionType != null) 'exceptionType': _limit(exceptionType!, 160),
    if (requestId != null) 'requestId': requestId,
    if (correlationId != null) 'correlationId': correlationId,
    if (httpStatus != null) 'httpStatus': httpStatus,
    if (endpointKey != null) 'endpointKey': endpointKey,
    if (appVersion != null) 'appVersion': appVersion,
    if (buildNumber != null) 'buildNumber': buildNumber,
    if (platform != null) 'platform': platform,
    if (deviceModel != null) 'deviceModel': deviceModel,
    if (osVersion != null) 'osVersion': osVersion,
    if (printerProvider != null) 'printerProvider': printerProvider,
    if (printerService != null) 'printerService': printerService,
    if (printerState != null) 'printerState': printerState,
    if (stackFrames.isNotEmpty)
      'stackFrames': stackFrames
          .take(24)
          .map((frame) => frame.toJson())
          .toList(),
  };

  factory ClientDiagnosticEvent.fromJson(Map<dynamic, dynamic> json) =>
      ClientDiagnosticEvent(
        eventId: json['eventId']?.toString(),
        category: _categoryFromWireName(json['category']?.toString()),
        occurredAtClient:
            DateTime.tryParse(
              json['occurredAtClient']?.toString() ?? '',
            )?.toUtc() ??
            DateTime.now().toUtc(),
        severity: _severityFromWireName(json['severity']?.toString()),
        operation: json['operation']?.toString() ?? 'unknown',
        errorCode: json['errorCode']?.toString(),
        message: json['message']?.toString(),
        exceptionType: json['exceptionType']?.toString(),
        requestId: json['requestId']?.toString(),
        correlationId: json['correlationId']?.toString(),
        httpStatus: (json['httpStatus'] as num?)?.toInt(),
        endpointKey: json['endpointKey']?.toString(),
        appVersion: json['appVersion']?.toString(),
        buildNumber: json['buildNumber']?.toString(),
        platform: json['platform']?.toString(),
        deviceModel: json['deviceModel']?.toString(),
        osVersion: json['osVersion']?.toString(),
        printerProvider: json['printerProvider']?.toString(),
        printerService: json['printerService']?.toString(),
        printerState: json['printerState']?.toString(),
        stackFrames: (json['stackFrames'] as List? ?? const [])
            .whereType<Map>()
            .map((frame) => ClientDiagnosticStackFrame.fromJson(frame))
            .toList(growable: false),
      );
}

extension ClientDiagnosticCategoryWireName on ClientDiagnosticCategory {
  String get wireName => switch (this) {
    ClientDiagnosticCategory.printerConfig => 'PRINTER_CONFIG',
    _ => name.toUpperCase(),
  };
}

class ClientDiagnosticStackFrame {
  const ClientDiagnosticStackFrame({
    this.symbol,
    this.file,
    this.line,
    this.column,
  });

  final String? symbol;
  final String? file;
  final int? line;
  final int? column;

  Map<String, dynamic> toJson() => {
    if (symbol != null) 'symbol': _limit(symbol!, 160),
    if (file != null) 'file': _limit(file!, 240),
    if (line != null) 'line': line,
    if (column != null) 'column': column,
  };

  factory ClientDiagnosticStackFrame.fromJson(Map<dynamic, dynamic> json) =>
      ClientDiagnosticStackFrame(
        symbol: json['symbol']?.toString(),
        file: json['file']?.toString(),
        line: (json['line'] as num?)?.toInt(),
        column: (json['column'] as num?)?.toInt(),
      );
}

ClientDiagnosticCategory _categoryFromWireName(String? value) {
  final normalized = value?.toUpperCase();
  for (final category in ClientDiagnosticCategory.values) {
    if (category.wireName == normalized) return category;
  }
  return ClientDiagnosticCategory.api;
}

ClientDiagnosticSeverity _severityFromWireName(String? value) {
  final normalized = value?.toLowerCase();
  for (final severity in ClientDiagnosticSeverity.values) {
    if (severity.name == normalized) return severity;
  }
  return ClientDiagnosticSeverity.error;
}

String _eventId() {
  final random = Random.secure();
  final bytes = List.generate(16, (_) => random.nextInt(256));
  return bytes.map((byte) => byte.toRadixString(16).padLeft(2, '0')).join();
}

String _limit(String value, int max) =>
    value.length <= max ? value : value.substring(0, max);
