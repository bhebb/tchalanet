// Dev-only: wipe browser auth state without redirecting to a specific app shell.
export async function purgeOidcBrowserCache(): Promise<void> {
  purgeWebStorage(globalThis.localStorage);
  purgeWebStorage(globalThis.sessionStorage);
  await purgeFirebaseIndexedDb();
  console.info('[purgeOidcBrowserCache] auth storage cleared');
}

function purgeWebStorage(storage: Storage | undefined): void {
  if (!storage) {
    return;
  }
  try {
    for (const key of Object.keys(storage)) {
      if (isAuthStorageKey(key)) {
        storage.removeItem(key);
      }
    }
  } catch {
    // Ignore storage access failures in locked-down browser contexts.
  }
}

function isAuthStorageKey(key: string): boolean {
  const normalized = key.toLowerCase();
  return (
    normalized.includes('firebase') ||
    normalized.includes('auth') ||
    normalized.includes('oidc') ||
    normalized.includes('token') ||
    normalized.includes('tchalanet.passwordlessloginemail') ||
    normalized.includes('tch.support')
  );
}

async function purgeFirebaseIndexedDb(): Promise<void> {
  const indexedDb = globalThis.indexedDB;
  if (!indexedDb) {
    return;
  }

  const databaseNames = await listDatabaseNames(indexedDb);
  await Promise.all(
    databaseNames
      .filter((name) => name.toLowerCase().includes('firebase'))
      .map((name) => deleteDatabase(indexedDb, name)),
  );
}

async function listDatabaseNames(indexedDb: IDBFactory): Promise<readonly string[]> {
  if (!('databases' in indexedDb)) {
    return ['firebaseLocalStorageDb'];
  }

  try {
    const databases = await indexedDb.databases();
    return databases.flatMap((database) => (database.name ? [database.name] : []));
  } catch {
    return ['firebaseLocalStorageDb'];
  }
}

function deleteDatabase(indexedDb: IDBFactory, name: string): Promise<void> {
  return new Promise((resolve) => {
    const request = indexedDb.deleteDatabase(name);
    request.onsuccess = () => resolve();
    request.onerror = () => resolve();
    request.onblocked = () => resolve();
  });
}
