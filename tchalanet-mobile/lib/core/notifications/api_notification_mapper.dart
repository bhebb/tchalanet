import '../network/api_exception.dart';
import '../network/api_notice.dart';
import 'app_notification.dart';
import 'support_reference.dart';

abstract final class ApiNotificationMapper {
  static AppNotificationKind kindForNotice(ApiNotice notice) =>
      switch (notice.severity) {
        ApiNoticeSeverity.info => AppNotificationKind.info,
        ApiNoticeSeverity.warning => AppNotificationKind.warning,
        ApiNoticeSeverity.error => AppNotificationKind.error,
      };

  static SupportReference? supportReferenceForException(
    ApiException exception,
  ) {
    final traceId = exception.traceId;
    if (traceId == null || traceId.isEmpty) return null;
    return SupportReference(
      traceId: traceId,
      code: exception.code,
      statusCode: exception.statusCode,
      errorId: exception.errorId,
    );
  }

  static SupportReference? supportReferenceForNotice(ApiNotice notice) {
    final traceId = notice.traceId;
    if (traceId == null || traceId.isEmpty) return null;
    return SupportReference(traceId: traceId, code: notice.code);
  }
}
