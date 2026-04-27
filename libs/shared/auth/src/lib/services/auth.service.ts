import { EventTypes, LoginResponse, OidcSecurityService, PublicEventsService } from 'angular-auth-oidc-client';

import { firstValueFrom, Observable } from 'rxjs';
import { finalize, shareReplay, take } from 'rxjs/operators';

import { computed, inject, Injectable, signal } from '@angular/core';

export type TchClaim = {
  tenantId: string;
  plan: string;
  featureSetId: string;
  locale: string;
  roles: string[];
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oidc = inject(OidcSecurityService);
  private readonly events = inject(PublicEventsService);

  private readonly _isAuth = signal(false);
  private readonly _access = signal<string | null>(null);
  private readonly _id = signal<string | null>(null);
  private readonly _tch = signal<TchClaim | null>(null);

  // ===== Exposition =====
  isAuthenticated = computed(() => this._isAuth());
  accessToken(): string | null { return this._access(); }
  tch(): TchClaim | null { return this._tch(); }

  // ===== Single-flight holder =====
  private inflight$?: Observable<LoginResponse>;

  /** Boot (APP_INITIALIZER) */
  init(): Promise<void> {
    return firstValueFrom(this.checkAuth$().pipe(take(1))).then((r: LoginResponse | null | undefined) => this.hydrateFrom(r));
  }

  /** Login */
  login(target = '/app') {
    sessionStorage.setItem('login_target', target);
    this.oidc.authorize();
  }

  /** Logout */
  logout() {
    // Reset local state
    this._isAuth.set(false);
    this._access.set(null);
    this._id.set(null);
    this._tch.set(null);
    // Déclenche le logout OIDC (redirigera vers postLogoutRedirectUri)
    this.oidc.logoff();
  }

  // ===== Callback / Check =====
  /** checkAuth est idempotent; on le single-flight pour éviter les doublons. */
  checkAuth$(): Observable<LoginResponse> {
    this.inflight$ ??= this.oidc.checkAuth().pipe(
      shareReplay({ bufferSize: 1, refCount: false }),
      finalize(() => {
        this.inflight$ = undefined;
      }),
    );
    return this.inflight$;
  }

  /** Hydrate les signals à partir du résultat OIDC. Accepte un LoginResponse partiel. */
  hydrateFrom(login: Partial<LoginResponse> | null | undefined) {
    const isAuth = !!login?.isAuthenticated;
    this._isAuth.set(isAuth);
    this._access.set(login?.accessToken ?? null);
    this._id.set(login?.idToken ?? null);
    this._tch.set(this.parseTch(login?.idToken ?? null));
  }

  /** Parse le custom claim 'tch' depuis l'id_token. */
  parseTch(idToken: string | null): TchClaim | null {
    if (!idToken) return null;
    try {
      const payload = JSON.parse(atob(idToken.split('.')[1] || ''));
      return (payload?.['tch'] as TchClaim) ?? null;
    } catch {
      return null;
    }
  }

  /** Récupère puis consomme la cible de redirection post-login. */
  consumeLoginTarget(): string {
    const t = sessionStorage.getItem('login_target') || '/app';
    sessionStorage.removeItem('login_target');
    return t;
  }

  // ===== Roles (depuis tch.roles) =====
  hasRole(role: string): boolean {
    const roles = this._tch()?.roles ?? [];
    return roles.includes(role);
  }

  // ===== Brancher les événements (silent refresh, etc.) =====
  /** À appeler une seule fois (ex: dans AppComponent constructor) pour suivre les events OIDC. */
  wireOidcEvents() {
    // Helper pour résoudre une valeur qui peut être Observable<T> ou T directement
    const resolve = async <T>(val: T | Observable<T>): Promise<T> => {
      // Type guard pour Observable
      if (val && typeof val === 'object' && 'subscribe' in val) {
        return await firstValueFrom(val as Observable<T>);
      }
      return val as T;
    };
    this.events.registerForEvents().subscribe(e => {
      switch (e.type) {
        case EventTypes.CheckSessionReceived: {
          this.oidc.isAuthenticated$.pipe(take(1)).subscribe(async ({ isAuthenticated }) => {
            const rawAccess = this.oidc.getAccessToken();
            const rawId = this.oidc.getIdToken();
            const rawUser = this.oidc.getUserData();
            const accessToken: string = (await resolve<string>(rawAccess as string | Observable<string>)) || '';
            const idToken: string = (await resolve<string>(rawId as string | Observable<string>)) || '';
            const userData: unknown = await resolve<unknown>(rawUser as unknown | Observable<unknown>);
            this.hydrateFrom({
              isAuthenticated,
              accessToken: accessToken || undefined,
              idToken: idToken || undefined,
              userData: userData ?? undefined,
            });
          });
          break;
        }
        case EventTypes.NewAuthenticationResult: {
          (async () => {
            const rawAccess = this.oidc.getAccessToken();
            const rawId = this.oidc.getIdToken();
            const rawUser = this.oidc.getUserData();
            const accessToken: string = (await resolve<string>(rawAccess as string | Observable<string>)) || '';
            const idToken: string = (await resolve<string>(rawId as string | Observable<string>)) || '';
            const userData: unknown = await resolve<unknown>(rawUser as unknown | Observable<unknown>);
            this.hydrateFrom({
              isAuthenticated: true,
              accessToken: accessToken || undefined,
              idToken: idToken || undefined,
              userData: userData ?? undefined,
            });
          })();
          break;
        }
      }
    });
  }
}
