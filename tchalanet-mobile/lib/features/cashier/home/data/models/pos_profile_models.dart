import 'cashier_home_models.dart' show idValue;

class PosProfileTerminalInfo {
  const PosProfileTerminalInfo({
    required this.ready,
    required this.trusted,
    required this.mustChangePin,
    this.id,
    this.code,
    this.displayName,
    this.status,
    this.lastSeenAt,
    this.source,
  });

  final String? id;
  final String? code;
  final String? displayName;
  final String? status;
  final bool ready;
  final bool trusted;
  final bool mustChangePin;
  final DateTime? lastSeenAt;
  final String? source;

  factory PosProfileTerminalInfo.fromJson(Map<String, dynamic> json) =>
      PosProfileTerminalInfo(
        id: idValue(json['id']),
        code: json['code'] as String?,
        displayName: json['displayName'] as String?,
        status: json['status'] as String?,
        ready: json['ready'] as bool? ?? false,
        trusted: json['trusted'] as bool? ?? false,
        mustChangePin: json['mustChangePin'] as bool? ?? false,
        lastSeenAt: json['lastSeenAt'] != null
            ? DateTime.tryParse(json['lastSeenAt'] as String)
            : null,
        source: json['source'] as String?,
      );
}

class PosProfileSellerInfo {
  const PosProfileSellerInfo({
    this.firstName,
    this.lastName,
    this.displayName,
    this.email,
    this.phoneNumber,
    this.addressId,
  });

  final String? firstName;
  final String? lastName;
  final String? displayName;
  final String? email;
  final String? phoneNumber;
  final String? addressId;

  factory PosProfileSellerInfo.fromJson(Map<String, dynamic> json) =>
      PosProfileSellerInfo(
        firstName: json['firstName'] as String?,
        lastName: json['lastName'] as String?,
        displayName: json['displayName'] as String?,
        email: json['email'] as String?,
        phoneNumber: json['phoneNumber'] as String?,
        addressId: idValue(json['addressId']),
      );
}

class PosProfileCommercialInfo {
  const PosProfileCommercialInfo({
    this.tenantId,
    this.tenantCode,
    this.currency,
    this.commissionRate,
  });

  final String? tenantId;
  final String? tenantCode;
  final String? currency;
  final num? commissionRate;

  factory PosProfileCommercialInfo.fromJson(Map<String, dynamic> json) =>
      PosProfileCommercialInfo(
        tenantId: idValue(json['tenantId']),
        tenantCode: json['tenantCode'] as String?,
        currency: json['currency'] as String?,
        commissionRate: json['commissionRate'] as num?,
      );
}

class PosProfileSettingsInfo {
  const PosProfileSettingsInfo({
    required this.supportedLocales,
    required this.receipt,
    required this.notifications,
    this.locale,
    this.timezone,
    this.currency,
  });

  final String? locale;
  final String? timezone;
  final String? currency;
  final List<String> supportedLocales;
  final PosProfileReceiptSettings receipt;
  final PosProfileNotificationSettings notifications;

  factory PosProfileSettingsInfo.fromJson(Map<String, dynamic> json) =>
      PosProfileSettingsInfo(
        locale: json['locale'] as String?,
        timezone: json['timezone'] as String?,
        currency: json['currency'] as String?,
        supportedLocales:
            (json['supportedLocales'] as List<dynamic>?)
                ?.map((e) => e.toString())
                .toList() ??
            const ['ht', 'fr', 'en'],
        receipt: PosProfileReceiptSettings.fromJson(
          (json['receipt'] as Map<String, dynamic>?) ?? const {},
        ),
        notifications: PosProfileNotificationSettings.fromJson(
          (json['notifications'] as Map<String, dynamic>?) ?? const {},
        ),
      );
}

class PosProfileReceiptSettings {
  const PosProfileReceiptSettings({
    required this.autoPrint,
    required this.copyCount,
  });

  final bool autoPrint;
  final int copyCount;

  factory PosProfileReceiptSettings.fromJson(Map<String, dynamic> json) =>
      PosProfileReceiptSettings(
        autoPrint: json['autoPrint'] as bool? ?? true,
        copyCount: (json['copyCount'] as num?)?.toInt() ?? 1,
      );
}

class PosProfileNotificationSettings {
  const PosProfileNotificationSettings({
    required this.enabled,
    required this.criticalOnly,
  });

  final bool enabled;
  final bool criticalOnly;

  factory PosProfileNotificationSettings.fromJson(Map<String, dynamic> json) =>
      PosProfileNotificationSettings(
        enabled: json['enabled'] as bool? ?? true,
        criticalOnly: json['criticalOnly'] as bool? ?? false,
      );
}

class PosProfileResponse {
  const PosProfileResponse({
    required this.terminal,
    required this.seller,
    required this.commercial,
    required this.settings,
    this.version,
  });

  final String? version;
  final PosProfileTerminalInfo terminal;
  final PosProfileSellerInfo seller;
  final PosProfileCommercialInfo commercial;
  final PosProfileSettingsInfo settings;

  factory PosProfileResponse.fromJson(Map<String, dynamic> json) =>
      PosProfileResponse(
        version: json['version'] as String?,
        terminal: PosProfileTerminalInfo.fromJson(
          (json['terminal'] as Map<String, dynamic>?) ?? const {},
        ),
        seller: PosProfileSellerInfo.fromJson(
          (json['seller'] as Map<String, dynamic>?) ?? const {},
        ),
        commercial: PosProfileCommercialInfo.fromJson(
          (json['commercial'] as Map<String, dynamic>?) ?? const {},
        ),
        settings: PosProfileSettingsInfo.fromJson(
          (json['settings'] as Map<String, dynamic>?) ?? const {},
        ),
      );
}
