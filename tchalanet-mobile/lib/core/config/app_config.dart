const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080/api/v1',
);

// Keycloak — emulator default points to host port 8082
const kcBaseUrl = String.fromEnvironment(
  'KC_BASE_URL',
  defaultValue: 'http://10.0.2.2:8082',
);

const kcRealm = String.fromEnvironment(
  'KC_REALM',
  defaultValue: 'tchalanet',
);

const kcClientId = String.fromEnvironment(
  'KC_CLIENT_ID',
  defaultValue: 'tchalanet-mobile-pos',
);

const kcRedirectUri = 'com.tchalanet.mobile:/oauth2redirect';
