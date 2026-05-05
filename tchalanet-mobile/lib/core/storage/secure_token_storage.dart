import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'token_storage.dart';

const _keyAccessToken = 'access_token';

class SecureTokenStorage implements TokenStorage {
  const SecureTokenStorage(this._storage);

  final FlutterSecureStorage _storage;

  @override
  Future<String?> readAccessToken() => _storage.read(key: _keyAccessToken);

  @override
  Future<void> writeAccessToken(String token) =>
      _storage.write(key: _keyAccessToken, value: token);

  @override
  Future<void> clear() => _storage.delete(key: _keyAccessToken);
}

final tokenStorageProvider = Provider<TokenStorage>((ref) {
  return const SecureTokenStorage(FlutterSecureStorage());
});
