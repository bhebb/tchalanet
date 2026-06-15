import 'package:firebase_auth/firebase_auth.dart';

import 'auth_token_client.dart';

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
      _tokens(_auth.currentUser, forceRefresh: true);

  @override
  Future<void> logout() => _auth.signOut();

  Future<AuthTokenData> _tokens(User? user, {bool forceRefresh = false}) async {
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
