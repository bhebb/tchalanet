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
  });

  it('keeps i18n loader config local-only (i18n is delivered via bootstrap)', () => {
    expect(PORTAL_I18N_CONFIG.assetsPrefix).toBe('/assets/i18n/');
    expect(PORTAL_I18N_CONFIG.assetsSuffix).toBe('.json');
    expect('backendPath' in PORTAL_I18N_CONFIG).toBe(false);
  });

  it('selects the Keycloak host from the browser hostname', () => {
    expect(keycloakUrlForHostname('app.tchalanet.lan')).toBe(AUTH_CONFIG.lanUrl);
    expect(keycloakUrlForHostname('localhost')).toBe(AUTH_CONFIG.localUrl);
  });

  it('attaches the bearer to non-public Tchalanet API URLs only', () => {
    // Non-public API → bearer attached.
    expect(APPLICATION_API_URL_PATTERN.test('/api/v1/tenant/runtime/bootstrap')).toBe(true);
    expect(APPLICATION_API_URL_PATTERN.test('http://localhost:8083/api/v1/tenant/dashboard')).toBe(
      true,
    );
    // Public API → must stay anonymous (no bearer), even with an active session.
    expect(APPLICATION_API_URL_PATTERN.test('/api/v1/public/settings')).toBe(false);
    expect(APPLICATION_API_URL_PATTERN.test('http://localhost:8083/api/v1/public/runtime/bootstrap')).toBe(
      false,
    );
    // Foreign origin → never matched.
    expect(APPLICATION_API_URL_PATTERN.test('https://example.com/api/v1/tenant/dashboard')).toBe(
      false,
    );
  });
});
