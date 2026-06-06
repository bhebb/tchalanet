import {
  API_PATHS,
  APPLICATION_API_URL_PATTERN,
  AUTH_CONFIG,
  PORTAL_I18N_CONFIG,
  keycloakUrlForHostname,
} from './runtime-paths';

describe('runtime paths', () => {
  it('keeps i18n and settings API calls relative', () => {
    expect(API_PATHS.i18n.public).toBe('/api/v1/public/i18n');
    expect(API_PATHS.settings.public).toBe('/api/v1/public/settings');
    expect(API_PATHS.settings.tenantResolve).toBe('/api/v1/tenant/settings/resolve');
    expect(PORTAL_I18N_CONFIG.backendPath).toBe(API_PATHS.i18n.public);
  });

  it('selects the Keycloak host from the browser hostname', () => {
    expect(keycloakUrlForHostname('app.tchalanet.lan')).toBe(AUTH_CONFIG.lanUrl);
    expect(keycloakUrlForHostname('localhost')).toBe(AUTH_CONFIG.localUrl);
  });

  it('recognizes relative and approved local API URLs', () => {
    expect(APPLICATION_API_URL_PATTERN.test('/api/v1/public/settings')).toBe(true);
    expect(APPLICATION_API_URL_PATTERN.test('http://localhost:8083/api/v1/public/settings')).toBe(
      true,
    );
    expect(APPLICATION_API_URL_PATTERN.test('https://example.com/api/v1/public/settings')).toBe(
      false,
    );
  });
});
