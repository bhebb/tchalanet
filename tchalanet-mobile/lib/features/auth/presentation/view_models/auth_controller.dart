import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/auth/auth_token_client.dart';
import '../../../../core/network/session_invalidation_controller.dart';
import '../../data/models/user_session.dart';
import '../../data/repositories/auth_repository.dart';
import '../../data/repositories/auth_repository_impl.dart';

sealed class AuthState {}

final class AuthUnknown extends AuthState {}

final class AuthUnauthenticated extends AuthState {
  AuthUnauthenticated({this.errorKey});
  final String? errorKey;
}

final class AuthLoading extends AuthState {}

final class AuthAuthenticated extends AuthState {
  AuthAuthenticated(this.session);
  final UserSession session;
}

class AuthController extends Notifier<AuthState> {
  late AuthRepository _repository;
  int _operationRevision = 0;

  @override
  AuthState build() {
    _repository = ref.watch(authRepositoryProvider);
    ref.listen(sessionInvalidationProvider, (previous, next) {
      if (previous != next && state is AuthAuthenticated) {
        logout();
      }
    });
    _restore();
    return AuthUnknown();
  }

  Future<void> _restore() async {
    final revision = _operationRevision;
    try {
      final session = await _repository.restoreSession();
      if (revision != _operationRevision) return;
      state = session != null
          ? AuthAuthenticated(session)
          : AuthUnauthenticated();
    } catch (_) {
      if (revision != _operationRevision) return;
      state = AuthUnauthenticated();
    }
  }

  Future<void> login(AuthCredentials credentials) async {
    final revision = ++_operationRevision;
    state = AuthLoading();
    try {
      final session = await _repository.login(credentials);
      if (revision != _operationRevision) return;
      state = AuthAuthenticated(session);
    } catch (_) {
      if (revision != _operationRevision) return;
      state = AuthUnauthenticated(errorKey: 'auth.login.error');
    }
  }

  Future<void> logout() async {
    final revision = ++_operationRevision;
    await _repository.logout();
    if (revision != _operationRevision) return;
    state = AuthUnauthenticated();
  }
}

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);

/// Derived provider — exposes the current UserSession to any widget.
/// Returns UserSession.unauthenticated when not logged in.
final userSessionProvider = Provider<UserSession>((ref) {
  final auth = ref.watch(authControllerProvider);
  return switch (auth) {
    AuthAuthenticated(:final session) => session,
    _ => UserSession.unauthenticated,
  };
});
