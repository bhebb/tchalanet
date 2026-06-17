import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/auth/auth_token_client.dart';
import 'package:tchalanet_mobile/core/runtime/runtime_models.dart';
import 'package:tchalanet_mobile/core/runtime/runtime_repository.dart';
import 'package:tchalanet_mobile/core/storage/op_context_storage.dart';
import 'package:tchalanet_mobile/core/storage/token_storage.dart';
import 'package:tchalanet_mobile/features/auth/data/models/user_role.dart';
import 'package:tchalanet_mobile/features/auth/data/repositories/auth_repository_impl.dart';

class _FakeTokenStorage implements TokenStorage {
  var cleared = false;
  String? accessToken;
  String? refreshToken;

  @override
  Future<void> clear() async => cleared = true;

  @override
  Future<String?> readAccessToken() async => accessToken;

  @override
  Future<String?> readRefreshToken() async => refreshToken;

  @override
  Future<void> writeAccessToken(String token) async => accessToken = token;

  @override
  Future<void> writeRefreshToken(String token) async => refreshToken = token;
}

class _FakeOpContextStorage extends OpContextStorage {
  _FakeOpContextStorage() : super(const FlutterSecureStorage());

  var cleared = false;

  @override
  Future<void> clear() async => cleared = true;
}

class _FakeAuthService implements AuthTokenClient {
  const _FakeAuthService(this.tokens);

  final AuthTokenData tokens;

  @override
  Future<AuthTokenData> login(AuthCredentials credentials) async => tokens;

  @override
  Future<void> logout() async {}

  @override
  Future<AuthTokenData> refresh([String? refreshToken]) async => tokens;
}

class _FakeRuntimeRepository implements RuntimeRepository {
  _FakeRuntimeRepository(this.bootstrap);

  final RuntimeBootstrap bootstrap;

  @override
  Future<RuntimeBootstrap> loadPublic(String locale) async => bootstrap;

  @override
  Future<RuntimeBootstrap> loadTenant() async => bootstrap;

  @override
  Future<RuntimeStateSnapshot> refreshTenantState() =>
      throw UnimplementedError();
}

void main() {
  test('logout clears tokens and tenant operational context', () async {
    final tokens = _FakeTokenStorage();
    final opContext = _FakeOpContextStorage();
    final repository = AuthRepositoryImpl(
      _FakeAuthService(AuthTokenData(accessToken: _validJwt())),
      tokens,
      opContext,
      _FakeRuntimeRepository(_runtimeBootstrap()),
    );

    await repository.logout();

    expect(tokens.cleared, isTrue);
    expect(opContext.cleared, isTrue);
  });

  test('login builds application session from private runtime', () async {
    final tokens = _FakeTokenStorage();
    final repository = AuthRepositoryImpl(
      _FakeAuthService(AuthTokenData(accessToken: _validJwt())),
      tokens,
      _FakeOpContextStorage(),
      _FakeRuntimeRepository(_runtimeBootstrap()),
    );

    final session = await repository.login(
      const AuthCredentials(email: 'user@example.com', password: 'secret'),
    );

    expect(session.userId, 'app-user-1');
    expect(session.username, 'runtime-user');
    expect(session.displayName, 'Runtime User');
    expect(session.tenantId, 'tenant-1');
    expect(session.tenantCode, 'TCH');
    expect(session.roles, [UserRole.cashier]);
    expect(session.tokenExpiresAt, isNotNull);
    expect(tokens.accessToken, isNotNull);
  });

  test(
    'login clears provider tokens when runtime cannot resolve a user',
    () async {
      final tokens = _FakeTokenStorage();
      final repository = AuthRepositoryImpl(
        _FakeAuthService(AuthTokenData(accessToken: _validJwt())),
        tokens,
        _FakeOpContextStorage(),
        _FakeRuntimeRepository(_runtimeBootstrap(user: null)),
      );

      await expectLater(
        repository.login(
          const AuthCredentials(email: 'user@example.com', password: 'secret'),
        ),
        throwsStateError,
      );

      expect(tokens.cleared, isTrue);
    },
  );
}

RuntimeBootstrap _runtimeBootstrap({
  RuntimeUser? user = const RuntimeUser(
    userId: 'app-user-1',
    username: 'runtime-user',
    displayName: 'Runtime User',
  ),
}) => RuntimeBootstrap(
  scope: RuntimeScope.tenant,
  locale: 'ht',
  i18nMessages: const {},
  features: const {},
  roles: const {'CASHIER'},
  permissions: const {},
  readinessStatus: 'READY',
  notifications: const RuntimeNotificationSummary(),
  notices: const [],
  user: user,
  tenantContext: const RuntimeTenantContext(
    tenantId: 'tenant-1',
    tenantCode: 'TCH',
  ),
);

String _validJwt() {
  const payload = 'eyJleHAiOjQxMDI0NDQ4MDB9';
  return 'header.$payload.signature';
}
