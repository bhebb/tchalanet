class AuthCredentials {
  const AuthCredentials({
    required this.email,
    required this.password,
    this.terminalCode,
  });

  factory AuthCredentials.terminal({
    required String terminalCode,
    required String pin,
    required String domain,
  }) {
    final normalizedTerminalCode = terminalCode.trim();
    return AuthCredentials(
      email: '${normalizedTerminalCode.toLowerCase()}@$domain',
      password: pin,
      terminalCode: normalizedTerminalCode,
    );
  }

  final String email;
  final String password;
  final String? terminalCode;
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
