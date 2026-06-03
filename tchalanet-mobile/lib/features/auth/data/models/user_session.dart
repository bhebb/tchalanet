import 'user_role.dart';

class UserSession {
  const UserSession({
    required this.authenticated,
    this.userId,
    this.username,
    this.displayName,
    this.tenantId,
    this.tenantCode,
    this.roles = const [],
    this.tokenExpiresAt,
  });

  final bool authenticated;
  final String? userId;
  final String? username;
  final String? displayName;
  final String? tenantId;
  final String? tenantCode;
  final List<UserRole> roles;
  final DateTime? tokenExpiresAt;

  static const unauthenticated = UserSession(authenticated: false);

  bool hasRole(UserRole role) => roles.contains(role);
  bool get isCashier => hasRole(UserRole.cashier);
  bool get isTenantAdmin => hasRole(UserRole.tenantAdmin);
  bool get isPlatformAdmin => hasRole(UserRole.platformAdmin);

  // accessToken is intentionally omitted — never log session objects
  @override
  String toString() =>
      'UserSession(authenticated: $authenticated, username: $username, '
      'tenantId: $tenantId, roles: $roles)';
}
