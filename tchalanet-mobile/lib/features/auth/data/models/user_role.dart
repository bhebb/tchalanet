enum UserRole {
  platformAdmin,
  tenantAdmin,
  cashier,
  unknown;

  static UserRole fromString(String? value) => switch (value?.toUpperCase()) {
    'PLATFORM_ADMIN' => UserRole.platformAdmin,
    'TENANT_ADMIN' => UserRole.tenantAdmin,
    'CASHIER' => UserRole.cashier,
    _ => UserRole.unknown,
  };
}
