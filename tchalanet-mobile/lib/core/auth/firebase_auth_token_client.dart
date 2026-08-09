import 'dart:async';

import 'package:firebase_auth/firebase_auth.dart';

import 'auth_token_client.dart';

const _firebaseUserRestoreTimeout = Duration(seconds: 2);

class FirebaseAuthTokenClient implements AuthTokenClient {
  FirebaseAuthTokenClient({FirebaseAuth? auth})
    : _auth = auth ?? FirebaseAuth.instance;

  final FirebaseAuth _auth;

  @override
  Future<AuthTokenData> login(AuthCredentials credentials) async {
    final result = await _auth.signInWithEmailAndPassword(
      email: credentials.email.trim(),
      password: credentials.password,
    );
    return _tokens(result.user);
  }

  @override
  Future<AuthTokenData> refresh([String? refreshToken]) =>
      _tokens(_currentUser(), forceRefresh: true);

  @override
  Future<void> logout() => _auth.signOut();

  Future<User?> _currentUser() async {
    final current = _auth.currentUser;
    if (current != null) return current;
    try {
      return await _auth
          .authStateChanges()
          .where((user) => user != null)
          .cast<User>()
          .first
          .timeout(_firebaseUserRestoreTimeout);
    } on TimeoutException {
      return null;
    }
  }

  Future<AuthTokenData> _tokens(
    FutureOr<User?> userSource, {
    bool forceRefresh = false,
  }) async {
    final user = await userSource;
    if (user == null) {
      throw StateError('Firebase authentication session is unavailable');
    }
    final result = await user.getIdTokenResult(forceRefresh);
    final token = result.token;
    if (token == null || token.isEmpty) {
      throw StateError('Firebase ID token is unavailable');
    }
    return AuthTokenData(accessToken: token, expiresAt: result.expirationTime);
  }
}
