import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { UserRole, UserSession } from '../../shared/types';
import { PrivateRuntimeInitializer } from '../runtime';
import { AUTH_CLIENT } from './auth-client';

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
            };

            this.sessionState.set(session);
            return session;
        } catch {
            // Firebase OK, mais Tchalanet refuse: pas de AppUser mapping, rôle absent, tenant absent, etc.
            return this.setAnonymousSession();
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

function normalizeRoles(roles: readonly string[] | undefined): readonly UserRole[] {
    return (roles ?? [])
        .map((role) => role.toUpperCase())
        .filter((role): role is UserRole => supportedRoles.includes(role as UserRole));
}
