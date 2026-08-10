import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/runtime/runtime_controller.dart';

class ForbiddenUiState {
  const ForbiddenUiState({
    required this.titleKey,
    required this.messageKey,
    required this.backActionKey,
    required this.contactActionKey,
    required this.contactMessageKey,
    this.runtimeReason,
  });

  final String titleKey;
  final String messageKey;
  final String backActionKey;
  final String contactActionKey;
  final String contactMessageKey;
  final String? runtimeReason;
}

class ForbiddenViewModel extends Notifier<ForbiddenUiState> {
  @override
  ForbiddenUiState build() {
    String? runtimeReason;
    try {
      final runtime = ref.watch(runtimeControllerProvider);
      final notices = [
        ...?runtime.snapshot?.notices,
        ...?runtime.bootstrap?.notices,
      ];
      runtimeReason = _firstBlockingNoticeMessage(notices);
    } catch (_) {
      runtimeReason = null;
    }

    return ForbiddenUiState(
      titleKey: 'auth.forbidden.title',
      messageKey: 'auth.forbidden.message',
      backActionKey: 'auth.forbidden.back',
      contactActionKey: 'auth.forbidden.contact_admin',
      contactMessageKey: 'auth.forbidden.contact_admin_message',
      runtimeReason: runtimeReason,
    );
  }
}

String? _firstBlockingNoticeMessage(Iterable<dynamic> notices) {
  for (final notice in notices) {
    final message = notice.message.trim();
    if (message.isEmpty) continue;
    final level = notice.level.toUpperCase();
    final code = notice.code.toUpperCase();
    if (level == 'ERROR' || level == 'CRITICAL' || code.contains('BLOCK')) {
      return message;
    }
  }
  return null;
}

final forbiddenViewModelProvider =
    NotifierProvider.autoDispose<ForbiddenViewModel, ForbiddenUiState>(
      ForbiddenViewModel.new,
    );
