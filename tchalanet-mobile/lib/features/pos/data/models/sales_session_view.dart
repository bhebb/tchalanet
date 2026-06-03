class SalesSessionView {
  const SalesSessionView({
    required this.sessionId,
    required this.status,
    this.outletId,
    this.openedAt,
    this.closedAt,
  });

  final String sessionId;
  final String status;
  final String? outletId;
  final DateTime? openedAt;
  final DateTime? closedAt;

  bool get isOpen => status == 'OPEN';

  static const none = SalesSessionView(sessionId: '', status: 'NONE');

  factory SalesSessionView.fromJson(Map<String, dynamic> json) =>
      SalesSessionView(
        sessionId: json['sessionId'] as String? ?? '',
        status: json['status'] as String? ?? 'NONE',
        outletId: json['outletId'] as String?,
        openedAt: json['openedAt'] != null
            ? DateTime.tryParse(json['openedAt'] as String)
            : null,
        closedAt: json['closedAt'] != null
            ? DateTime.tryParse(json['closedAt'] as String)
            : null,
      );
}
