import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { PrivateRuntimeInitializer } from './runtime/private-runtime-initializer';
import { AUTH_CLIENT } from './auth-client';
import { UserRole, UserSession } from './auth.types';

const supportedRoles: readonly UserRole[] = ['CASHIER', 'TENANT_ADMIN', 'SUPER_ADMIN'];

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly auth = inject(AUTH_CLIENT);
  private readonly runtime = inject(PrivateRuntimeInitializer);

  private readonly sessionState = signal<UserSession>({
    authenticated: false,
    roles: [],
  });

  readonly session = this.sessionState.asReadonly();
  readonly authenticated = computed(() => this.session().authenticated);

  async refreshSession(force = false): Promise<UserSession> {
    if (!(await this.auth.isAuthenticated())) {
      return this.setAnonymousSession();
    }

    if (!force && this.sessionState().authenticated) {
      return this.sessionState();
    }

    try {
      const bootstrap = await firstValueFrom(this.runtime.initialize());

      const session: UserSession = {
        authenticated: true,
        userId: bootstrap.user.userId ?? undefined,
        username: bootstrap.user.username ?? bootstrap.user.email ?? undefined,
        displayName:
          bootstrap.user.displayName ??
          bootstrap.user.username ??
          bootstrap.user.email ??
          bootstrap.user.userId ??
          undefined,
        tenantId: bootstrap.tenantContext?.tenantId,
        tenantCode: bootstrap.tenantContext?.tenantCode ?? undefined,
        roles: normalizeRoles(bootstrap.entitlements.roles),
        tokenExpiresAt: await this.auth.getTokenExpiresAt(),
        entryRoute: bootstrap.entryRoute ?? bootstrap.pageModelRef?.route ?? undefined,
        mustChangePassword: bootstrap.user.mustChangePassword ?? false,
        mustCompleteProfile: bootstrap.user.mustCompleteProfile ?? false,
      };

      this.sessionState.set(session);
      return session;
    } catch (err) {
      // 401/403 = Tchalanet refuses access (no AppUser mapping, missing role/tenant).
      // Force logout — the Firebase token is valid but this user has no platform access.
      if (isAccessDenied(err) || !this.sessionState().authenticated) {
        return this.setAnonymousSession();
      }
      // Network error, timeout, 5xx: transient failure.
      // Keep the last known session alive — do not kick the user out on a server hiccup.
      return this.sessionState();
    }
  }

  hasRole(role: UserRole): boolean {
    return this.session().roles.includes(role);
  }

  async login(email: string, password: string, remember = true): Promise<UserSession> {
    await this.auth.login({
      username: email,
      password,
      remember,
    });
    return this.refreshSession(true);
  }

  async sendPasswordlessLoginLink(email: string): Promise<void> {
    if (!this.auth.sendPasswordlessLoginLink) {
      throw new Error('Passwordless login is not supported by this auth client');
    }
    await this.auth.sendPasswordlessLoginLink(email);
  }

  async completePasswordlessLogin(): Promise<UserSession | null> {
    if (!this.auth.completePasswordlessLogin) {
      return null;
    }
    const completed = await this.auth.completePasswordlessLogin();
    return completed ? this.refreshSession(true) : null;
  }

  async sendPasswordResetEmail(email: string): Promise<void> {
    if (!this.auth.sendPasswordResetEmail) {
      throw new Error('Password reset is not supported by this auth client');
    }
    await this.auth.sendPasswordResetEmail(email);
  }

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    if (!this.auth.changePassword) {
      throw new Error('Password change is not supported by this auth client');
    }
    await this.auth.changePassword(currentPassword, newPassword);
  }

  async logout(): Promise<void> {
    await this.auth.logout();
    this.setAnonymousSession();
  }

  private setAnonymousSession(): UserSession {
    const session: UserSession = {
      authenticated: false,
      roles: [],
    };

    this.sessionState.set(session);
    return session;
  }
}

function isAccessDenied(err: unknown): boolean {
  return err instanceof HttpErrorResponse && (err.status === 401 || err.status === 403);
}

function normalizeRoles(roles: readonly string[] | undefined): readonly UserRole[] {
  return (roles ?? [])
    .map(role => role.toUpperCase())
    .map(role => {
      if (role === 'TENANT_OWNER') return 'TENANT_ADMIN';
      if (role === 'OPERATOR') return 'CASHIER';
      return role;
    })
    .filter((role): role is UserRole => supportedRoles.includes(role as UserRole));
}
