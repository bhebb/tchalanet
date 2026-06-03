import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'token_storage.dart';

const _keyAccessToken = 'access_token';
const _keyRefreshToken = 'refresh_token';

class SecureTokenStorage implements TokenStorage {
  const SecureTokenStorage(this._storage);

  final FlutterSecureStorage _storage;

  @override
  Future<String?> readAccessToken() => _storage.read(key: _keyAccessToken);

  @override
  Future<void> writeAccessToken(String token) =>
      _storage.write(key: _keyAccessToken, value: token);

  @override
  Future<String?> readRefreshToken() => _storage.read(key: _keyRefreshToken);

  @override
  Future<void> writeRefreshToken(String token) =>
      _storage.write(key: _keyRefreshToken, value: token);

  @override
  Future<void> clear() async {
    await _storage.delete(key: _keyAccessToken);
    await _storage.delete(key: _keyRefreshToken);
  }
}

final tokenStorageProvider = Provider<TokenStorage>((ref) {
  return const SecureTokenStorage(FlutterSecureStorage());
});
