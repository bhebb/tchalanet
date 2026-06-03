import { computed, inject, Injectable, signal } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';

import { UserRole, UserSession } from '../../shared/types';

const supportedRoles: readonly UserRole[] = ['CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN'];

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly keycloak = inject(KeycloakService);
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
    const authenticated = this.keycloak.isLoggedIn();

    if (!authenticated) {
      return this.setAnonymousSession();
    }

    const instance = this.keycloak.getKeycloakInstance();
    const token = toRecord(instance.tokenParsed);
    const profile = await this.loadProfileSafely();
    const session: UserSession = {
      authenticated: true,
      userId: readString(token, 'sub') ?? profile?.id,
      username: profile?.username ?? this.keycloak.getUsername(),
      displayName: profileDisplayName(profile) ?? this.keycloak.getUsername(),
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
    return this.keycloak.logout(redirectUri);
  }

  private roles(): readonly UserRole[] {
    const keycloakRoles = this.keycloak.getUserRoles(true);
    return keycloakRoles
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

  private async loadProfileSafely(): Promise<KeycloakProfileView | null> {
    try {
      return (await this.keycloak.loadUserProfile()) as KeycloakProfileView;
    } catch {
      return null;
    }
  }
}

interface KeycloakProfileView {
  readonly id?: string;
  readonly username?: string;
  readonly firstName?: string;
  readonly lastName?: string;
  readonly email?: string;
}

function profileDisplayName(profile: KeycloakProfileView | null): string | undefined {
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
