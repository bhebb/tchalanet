// Dev-only: wipe all browser auth state and force a hard reload.
export function purgeOidcBrowserCache(): void {
  try { localStorage.clear(); } catch { /* ignore */ }
  try { sessionStorage.clear(); } catch { /* ignore */ }
  console.info('[purgeOidcBrowserCache] storage cleared — reloading');
  globalThis.location.href = globalThis.location.origin + '/public';
}
