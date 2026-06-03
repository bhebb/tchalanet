import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/user_session.dart';
import '../../data/repositories/auth_repository.dart';
import '../../data/repositories/auth_repository_impl.dart';

sealed class AuthState {}

final class AuthUnknown extends AuthState {}

final class AuthUnauthenticated extends AuthState {
  AuthUnauthenticated({this.errorMessage});
  final String? errorMessage;
}

final class AuthLoading extends AuthState {}

final class AuthAuthenticated extends AuthState {
  AuthAuthenticated(this.session);
  final UserSession session;
}

class AuthController extends Notifier<AuthState> {
  late AuthRepository _repository;

  @override
  AuthState build() {
    _repository = ref.watch(authRepositoryProvider);
    _restore();
    return AuthUnknown();
  }

  Future<void> _restore() async {
    try {
      final session = await _repository.restoreSession();
      state = session != null ? AuthAuthenticated(session) : AuthUnauthenticated();
    } catch (_) {
      state = AuthUnauthenticated();
    }
  }

  Future<void> login() async {
    state = AuthLoading();
    try {
      final session = await _repository.login();
      state = AuthAuthenticated(session);
    } catch (e) {
      state = AuthUnauthenticated(errorMessage: e.toString());
    }
  }

  Future<void> logout() async {
    await _repository.logout();
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
