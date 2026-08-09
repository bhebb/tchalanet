import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/auth/firebase_auth_token_client.dart';

void main() {
  test('refresh waits through a transient null auth restore event', () async {
    final user = _FakeFirebaseUser('token-restored');
    final auth = _FakeFirebaseAuth(Stream<User?>.fromIterable([null, user]));
    final client = FirebaseAuthTokenClient(auth: auth);

    final result = await client.refresh();

    expect(result.accessToken, 'token-restored');
    expect(user.forceRefreshValues, [true]);
  });
}

class _FakeFirebaseAuth extends Fake implements FirebaseAuth {
  _FakeFirebaseAuth(this._authStateChanges);

  final Stream<User?> _authStateChanges;

  @override
  User? get currentUser => null;

  @override
  Stream<User?> authStateChanges() => _authStateChanges;
}

class _FakeFirebaseUser extends Fake implements User {
  _FakeFirebaseUser(this._token);

  final String _token;
  final List<bool> forceRefreshValues = [];

  @override
  Future<IdTokenResult> getIdTokenResult([bool forceRefresh = false]) async {
    forceRefreshValues.add(forceRefresh);
    return _FakeIdTokenResult(_token);
  }
}

class _FakeIdTokenResult extends Fake implements IdTokenResult {
  _FakeIdTokenResult(this._token);

  final String _token;

  @override
  String? get token => _token;

  @override
  DateTime? get expirationTime => DateTime.now().add(const Duration(hours: 1));
}
