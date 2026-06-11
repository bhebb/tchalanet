import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/storage/op_context_storage.dart';
import 'package:tchalanet_mobile/core/storage/token_storage.dart';
import 'package:tchalanet_mobile/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:tchalanet_mobile/features/auth/data/services/auth_service.dart';

class _FakeTokenStorage implements TokenStorage {
  var cleared = false;

  @override
  Future<void> clear() async => cleared = true;

  @override
  Future<String?> readAccessToken() async => null;

  @override
  Future<String?> readRefreshToken() async => null;

  @override
  Future<void> writeAccessToken(String token) async {}

  @override
  Future<void> writeRefreshToken(String token) async {}
}

class _FakeOpContextStorage extends OpContextStorage {
  _FakeOpContextStorage() : super(const FlutterSecureStorage());

  var cleared = false;

  @override
  Future<void> clear() async => cleared = true;
}

void main() {
  test('logout clears tokens and tenant operational context', () async {
    final tokens = _FakeTokenStorage();
    final opContext = _FakeOpContextStorage();
    final repository = AuthRepositoryImpl(const AuthService(), tokens, opContext);

    await repository.logout();

    expect(tokens.cleared, isTrue);
    expect(opContext.cleared, isTrue);
  });
}
