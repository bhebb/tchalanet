import '../models/user_session.dart';

abstract interface class AuthRepository {
  Future<UserSession> login();
  Future<UserSession?> restoreSession();
  Future<void> logout();
}
