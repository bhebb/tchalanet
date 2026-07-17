import { credsFor, type Role } from './env';

/**
 * Global setup for the emulator (variant A) run. Enabled with
 * `WEB_E2E_EMULATOR=1`. It:
 *   1. fails fast if the Firebase Auth emulator is not reachable — answering
 *      "make sure it's launched first"; and
 *   2. creates the seeded users (idempotent) for REST-stub mode, or verifies
 *      API-bootstrap users for Docker API mode.
 *
 * Roles/permissions come from the stubbed `/runtime/private` (see api-stub.ts),
 * so the emulator only needs the user to exist for password sign-in.
 * No-op unless WEB_E2E_EMULATOR=1 or WEB_E2E_API=1.
 */

const EMULATOR_HOST = process.env['TCH_FIREBASE_EMULATOR_HOST'] ?? '127.0.0.1:9099';
const ROLES: Role[] = ['superAdmin', 'admin', 'cashier'];

async function ensureEmulatorUp(): Promise<void> {
  try {
    // The emulator root responds with a banner/JSON once it is listening.
    await fetch(`http://${EMULATOR_HOST}/`);
  } catch (err) {
    throw new Error(
      `Firebase Auth emulator not reachable at http://${EMULATOR_HOST} — start it ` +
        `first (e.g. \`make up-firebase-emulator\`). ${(err as Error).message}`,
    );
  }
}

async function ensureUser(email: string, password: string): Promise<void> {
  const url =
    `http://${EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp` +
    `?key=fake-api-key`;
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, returnSecureToken: true }),
  });
  if (res.ok) return;
  const body = await res.text();
  if (body.includes('EMAIL_EXISTS')) return; // already provisioned — idempotent
  throw new Error(`Emulator signUp failed for ${email} (${res.status}): ${body}`);
}

async function verifyUser(email: string, password: string): Promise<void> {
  const url =
    `http://${EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword` +
    `?key=fake-api-key`;
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, returnSecureToken: true }),
  });
  if (res.ok) return;

  const body = await res.text();
  throw new Error(
    `Emulator sign-in failed for ${email} (${res.status}): ${body}. ` +
      `For WEB_E2E_API=1, start Firebase emulator before the API so the API ` +
      `bootstrap creates linked users, or restart the API after the emulator is up.`,
  );
}

export default async function globalSetup(): Promise<void> {
  const emulatorMode = process.env['WEB_E2E_EMULATOR'] === '1';
  const apiMode = process.env['WEB_E2E_API'] === '1';
  if (!emulatorMode && !apiMode) return;

  await ensureEmulatorUp();
  for (const role of ROLES) {
    const creds = credsFor(role);
    if (!creds) continue;
    if (apiMode) await verifyUser(creds.email, creds.password);
    else await ensureUser(creds.email, creds.password);
  }
}
