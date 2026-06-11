import 'package:flutter_riverpod/flutter_riverpod.dart';

class ForbiddenUiState {
  const ForbiddenUiState({
    required this.titleKey,
    required this.messageKey,
    required this.backActionKey,
  });

  final String titleKey;
  final String messageKey;
  final String backActionKey;
}

class ForbiddenViewModel extends Notifier<ForbiddenUiState> {
  @override
  ForbiddenUiState build() => const ForbiddenUiState(
    titleKey: 'auth.forbidden.title',
    messageKey: 'auth.forbidden.message',
    backActionKey: 'auth.forbidden.back',
  );
}

final forbiddenViewModelProvider =
    NotifierProvider.autoDispose<ForbiddenViewModel, ForbiddenUiState>(
      ForbiddenViewModel.new,
    );
