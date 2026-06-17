import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/runtime/runtime_models.dart';

void main() {
  test('authenticated runtime maps application user and tenant context', () {
    final runtime = RuntimeBootstrap.fromJson({
      'user': {
        'userId': 'user-1',
        'username': 'cashier',
        'displayName': 'Cashier One',
        'email': 'cashier@example.com',
      },
      'tenantContext': {'tenantId': 'tenant-1', 'tenantCode': 'TCH'},
      'entitlements': {
        'roles': ['CASHIER'],
        'permissions': ['ticket.sell'],
      },
    }, scope: RuntimeScope.tenant);

    expect(runtime.user?.userId, 'user-1');
    expect(runtime.user?.username, 'cashier');
    expect(runtime.tenantContext?.tenantId, 'tenant-1');
    expect(runtime.tenantContext?.tenantCode, 'TCH');
    expect(runtime.roles, {'CASHIER'});
  });
}
