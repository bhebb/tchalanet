import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../home/presentation/view_models/cashier_home_providers.dart';
import '../../../operationalcontext/data/storage/op_context_storage.dart';
import '../../data/models/cashier_session_models.dart';
import '../../data/services/cashier_session_service.dart';

sealed class SessionOpenState {
  const SessionOpenState();
}

final class SessionOpenIdle extends SessionOpenState {
  const SessionOpenIdle();
}

final class SessionOpenInProgress extends SessionOpenState {
  const SessionOpenInProgress();
}

final class SessionOpenSuccess extends SessionOpenState {
  const SessionOpenSuccess(this.session);
  final CashierSessionView session;
}

final class SessionOpenFailure extends SessionOpenState {
  const SessionOpenFailure(this.message);
  final String message;
}

class CashierSessionController extends Notifier<SessionOpenState> {
  @override
  SessionOpenState build() => const SessionOpenIdle();

  Future<void> openSession({
    required String outletId,
    required String terminalId,
    double openingFloat = 0.0,
  }) async {
    state = const SessionOpenInProgress();
    try {
      final session = await ref.read(cashierSessionServiceProvider).open(
            OpenSessionRequest(
              outletId: outletId,
              terminalId: terminalId,
              openingFloat: openingFloat,
            ),
          );
      // Persist session ID so X-Tch-Sales-Session-Id is sent on all requests
      await ref
          .read(opContextStorageProvider)
          .saveSessionId(session.sessionId);
      // Refresh home — now shows operational layout
      ref.invalidate(cashierHomeProvider);
      state = SessionOpenSuccess(session);
    } catch (e) {
      state = SessionOpenFailure(e.toString());
    }
  }

  void reset() => state = const SessionOpenIdle();
}

final cashierSessionControllerProvider =
    NotifierProvider<CashierSessionController, SessionOpenState>(
  CashierSessionController.new,
);
