// Script utilitaire (dev) pour purger le cache OIDC dans localStorage/sessionStorage
export function purgeOidcBrowserCache() {
  const lsKeys = [
    'authnResult',
    'authWellKnownEndPoints',
    'authStorageSession',
    'authUserData',
  ];
  for (const k of lsKeys) {
    try {
      localStorage.removeItem(k);
    } catch {
      // Ignore storage access errors in restrictive browser contexts.
    }
  }
  Object.keys(sessionStorage).forEach(k => {
    if (k.includes('oidc') || k.includes('auth') || k.includes('silent-renew')) {
      try {
        sessionStorage.removeItem(k);
      } catch {
        // Ignore storage access errors in restrictive browser contexts.
      }
    }
  });
  console.info('[purgeOidcBrowserCache] Cache OIDC purgé. Rechargez la page.');
}

// Pour usage rapide en console:
// import('./app/purge-oidc-cache').then(m => m.purgeOidcBrowserCache());

