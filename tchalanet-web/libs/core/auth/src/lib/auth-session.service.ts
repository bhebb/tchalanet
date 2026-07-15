import { isPlatformBrowser } from '@angular/common';
import { computed, DestroyRef, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { TchBackendClient } from '@tch/api';
import { TchRuntimeConfigStore } from '@tch/shared-config';
import { PrivateRuntimeInitializer } from './runtime/private-runtime-initializer';
import { AUTH_CLIENT } from './auth-client';
import { UserRole, UserSession } from './auth.types';

const supportedRoles: readonly UserRole[] = ['CASHIER', 'TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN'];
const AUTH_OPERATION_TIMEOUT_MS = 15_000;
const PERMISSION_REFRESH_INTERVAL_MS = 30 * 60 * 1000;

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly auth = inject(AUTH_CLIENT);
  private readonly runtime = inject(PrivateRuntimeInitializer);
  private readonly backend = inject(TchBackendClient);
  private readonly runtimeConfig = inject(TchRuntimeConfigStore);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);

  private readonly sessionState = signal<UserSession>({
    authenticated: false,
    roles: [],
  });
  private refreshTimerId: ReturnType<typeof setInterval> | null = null;

  readonly session = this.sessionState.asReadonly();
  readonly authenticated = computed(() => this.session().authenticated);

  constructor() {
    this.destroyRef.onDestroy(() => this.stopPermissionRefresh());
  }

  async refreshSession(force = false): Promise<UserSession> {
    if (!(await this.auth.isAuthenticated())) {
      return this.setAnonymousSession();
    }

    if (!force && this.sessionState().authenticated) {
      return this.sessionState();
    }

    try {
      const bootstrap = await firstValueFrom(
        this.runtime.initialize().pipe(timeout({ first: AUTH_OPERATION_TIMEOUT_MS })),
      );

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
        roles: normalizeRoles(
          [...(bootstrap.user.roles ?? []), ...(bootstrap.entitlements.roles ?? [])],
          bootstrap.space,
        ),
        permissions: normalizePermissions(bootstrap.entitlements.permissions),
        tokenExpiresAt: await this.auth.getTokenExpiresAt(),
        entryRoute: bootstrap.entryRoute ?? bootstrap.pageModelRef?.route ?? undefined,
        mustChangePassword: bootstrap.user.mustChangePassword ?? false,
        mustCompleteProfile: bootstrap.user.mustCompleteProfile ?? false,
      };

      this.sessionState.set(session);
      this.startPermissionRefresh();
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

  hasPermission(permission: string): boolean {
    const normalized = permission.trim().toLowerCase();
    return !!normalized && (this.session().permissions ?? []).includes(normalized);
  }

  async login(identifier: string, password: string): Promise<UserSession> {
    const resolvedIdentifier = await withTimeout(
      this.resolveLoginIdentifier(identifier),
      AUTH_OPERATION_TIMEOUT_MS,
      'auth.login_lookup.timeout',
    );
    await withTimeout(
      this.auth.login({
        username: resolvedIdentifier,
        password,
      }),
      AUTH_OPERATION_TIMEOUT_MS,
      'auth.login.timeout',
    );
    await withTimeout(
      this.auth.getAccessToken(true),
      AUTH_OPERATION_TIMEOUT_MS,
      'auth.token.timeout',
    );
    return withTimeout(this.refreshSession(true), AUTH_OPERATION_TIMEOUT_MS, 'auth.session.timeout');
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

  async sendPasswordResetEmail(identifier: string): Promise<void> {
    if (!this.auth.sendPasswordResetEmail) {
      throw new Error('Password reset is not supported by this auth client');
    }
    const resolvedIdentifier = await withTimeout(
      this.resolveLoginIdentifier(identifier),
      AUTH_OPERATION_TIMEOUT_MS,
      'auth.login_lookup.timeout',
    );
    await this.auth.sendPasswordResetEmail(resolvedIdentifier);
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
    this.stopPermissionRefresh();
    const session: UserSession = {
      authenticated: false,
      roles: [],
    };

    this.sessionState.set(session);
    return session;
  }

  private startPermissionRefresh(): void {
    if (this.refreshTimerId !== null || !isPlatformBrowser(this.platformId)) {
      return;
    }
    this.refreshTimerId = setInterval(() => {
      void this.refreshSession(true);
    }, this.sessionRefreshIntervalMs());
  }

  private stopPermissionRefresh(): void {
    if (this.refreshTimerId === null) {
      return;
    }
    clearInterval(this.refreshTimerId);
    this.refreshTimerId = null;
  }

  private sessionRefreshIntervalMs(): number {
    const configured = this.runtimeConfig.config().sessionRefreshIntervalMs;
    return typeof configured === 'number' && configured > 0
      ? configured
      : PERMISSION_REFRESH_INTERVAL_MS;
  }

  private async resolveLoginIdentifier(identifier: string): Promise<string> {
    const trimmed = identifier.trim();
    if (trimmed.includes('@')) {
      return trimmed;
    }
    const normalized = trimmed.toLowerCase();
    const response = await firstValueFrom(
      this.backend.post<{ resolvedIdentifier: string }, { identifier: string }>(
        '/public/auth/login-identifier/resolve',
        { identifier: normalized },
        { suppressShellFeedback: true },
      ),
    );
    return response.resolvedIdentifier;
  }
}

function isAccessDenied(err: unknown): boolean {
  return err instanceof HttpErrorResponse && (err.status === 401 || err.status === 403);
}

function withTimeout<T>(promise: Promise<T>, timeoutMs: number, message: string): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(message)), timeoutMs);
    promise.then(
      value => {
        clearTimeout(timer);
        resolve(value);
      },
      error => {
        clearTimeout(timer);
        reject(error);
      },
    );
  });
}

function normalizeRoles(roles: readonly string[] | undefined, space?: string | null): readonly UserRole[] {
  const normalized = (roles ?? [])
    .map(role => role.toUpperCase())
    .map(role => {
      if (role === 'ROLE_SUPER_ADMIN' || role === 'PLATFORM_ADMIN') return 'SUPER_ADMIN';
      if (role === 'ROLE_TENANT_OWNER') return 'TENANT_OWNER';
      if (role === 'ROLE_TENANT_ADMIN') return 'TENANT_ADMIN';
      if (role === 'TENANT_OWNER') return 'TENANT_OWNER';
      if (role === 'ROLE_CASHIER' || role === 'OPERATOR' || role === 'ACTOR_SELLER_TERMINAL') return 'CASHIER';
      return role;
    })
    .filter((role): role is UserRole => supportedRoles.includes(role as UserRole));

  const rolesFromSpace: UserRole[] = [];
  if (space === 'PLATFORM') rolesFromSpace.push('SUPER_ADMIN');
  if (space === 'ADMIN' && !normalized.includes('TENANT_OWNER')) rolesFromSpace.push('TENANT_ADMIN');
  if (space === 'CASHIER') rolesFromSpace.push('CASHIER');

  return Array.from(new Set([...normalized, ...rolesFromSpace]));
}

function normalizePermissions(permissions: readonly string[] | undefined): readonly string[] {
  return Array.from(new Set(
    (permissions ?? [])
      .map(permission => permission.trim().toLowerCase())
      .filter(Boolean),
  ));
}
