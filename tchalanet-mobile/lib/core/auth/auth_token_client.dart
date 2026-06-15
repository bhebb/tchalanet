class AuthCredentials {
  const AuthCredentials({required this.email, required this.password});

  final String email;
  final String password;
}

class AuthTokenData {
  const AuthTokenData({
    required this.accessToken,
    this.refreshToken,
    this.expiresAt,
  });

  final String accessToken;
  final String? refreshToken;
  final DateTime? expiresAt;
}

abstract interface class AuthTokenClient {
  Future<AuthTokenData> login(AuthCredentials credentials);
  Future<AuthTokenData> refresh([String? refreshToken]);
  Future<void> logout();
}
