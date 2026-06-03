import { computed, inject, Injectable, signal } from '@angular/core';
import Keycloak, { KeycloakProfile } from 'keycloak-js';

import { UserRole, UserSession } from '../../shared/types';

const supportedRoles: readonly UserRole[] = ['CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN'];

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly keycloak = inject(Keycloak);
  private readonly sessionState = signal<UserSession>({
    authenticated: false,
    roles: [],
  });

  readonly session = this.sessionState.asReadonly();
  readonly authenticated = computed(() => this.session().authenticated);

  constructor() {
    void this.refreshSession();
  }

  async refreshSession(): Promise<UserSession> {
    const authenticated = this.keycloak.authenticated === true;

    if (!authenticated) {
      return this.setAnonymousSession();
    }

    const token = toRecord(this.keycloak.tokenParsed);
    const profile = await this.loadProfileSafely();
    const session: UserSession = {
      authenticated: true,
      userId: readString(token, 'sub') ?? profile?.id,
      username: profile?.username ?? readString(token, 'preferred_username') ?? readString(token, 'email'),
      displayName:
        profileDisplayName(profile) ??
        readString(token, 'name') ??
        readString(token, 'preferred_username') ??
        readString(token, 'email') ??
        readString(token, 'sub'),
      tenantId: readString(token, 'tenant_id'),
      tenantCode: readString(token, 'tenant_code'),
      roles: this.roles(),
      tokenExpiresAt: tokenExpiresAt(token),
    };

    this.sessionState.set(session);
    return session;
  }

  hasRole(role: UserRole): boolean {
    return this.session().roles.includes(role);
  }

  login(redirectUri = globalThis.location.href): Promise<void> {
    return this.keycloak.login({ redirectUri });
  }

  logout(redirectUri = globalThis.location.origin + '/public'): Promise<void> {
    return this.keycloak.logout({ redirectUri });
  }

  private roles(): readonly UserRole[] {
    return collectRoles(this.keycloak)
      .map(role => role.toUpperCase())
      .filter((role): role is UserRole => supportedRoles.includes(role as UserRole));
  }

  private setAnonymousSession(): UserSession {
    const session: UserSession = {
      authenticated: false,
      roles: [],
    };

    this.sessionState.set(session);
    return session;
  }

  private async loadProfileSafely(): Promise<KeycloakProfile | null> {
    try {
      return await this.keycloak.loadUserProfile();
    } catch {
      return null;
    }
  }
}

function collectRoles(keycloak: Keycloak): readonly string[] {
  const realmRoles = keycloak.realmAccess?.roles ?? [];
  const resourceRoles = Object.values(keycloak.resourceAccess ?? {}).flatMap(access => access.roles);

  return [...new Set([...realmRoles, ...resourceRoles])];
}

function profileDisplayName(profile: KeycloakProfile | null): string | undefined {
  if (!profile) {
    return undefined;
  }

  const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(' ').trim();
  return fullName || profile.username || profile.email;
}

function toRecord(value: unknown): Readonly<Record<string, unknown>> {
  return typeof value === 'object' && value !== null ? (value as Readonly<Record<string, unknown>>) : {};
}

function readString(record: Readonly<Record<string, unknown>>, key: string): string | undefined {
  const value = record[key];
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

function tokenExpiresAt(token: Readonly<Record<string, unknown>>): string | undefined {
  const exp = token['exp'];

  if (typeof exp !== 'number') {
    return undefined;
  }

  return new Date(exp * 1000).toISOString();
}
