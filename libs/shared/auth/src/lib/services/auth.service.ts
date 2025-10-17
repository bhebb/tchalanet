import { computed, inject, Injectable, signal } from '@angular/core';
import {
  EventTypes,
  LoginResponse,
  OidcSecurityService,
  PublicEventsService,
} from 'angular-auth-oidc-client';
import { Observable } from 'rxjs';
import { finalize, shareReplay, take } from 'rxjs/operators';

export type TchClaim = {
  tenantId: string;
  plan: string;
  featureSetId: string;
  locale: string;
  roles: string[];
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private oidc = inject(OidcSecurityService);
  private events = inject(PublicEventsService);

  private _isAuth = signal(false);
  private _access = signal<string | null>(null);
  private _id = signal<string | null>(null);
  private _tch = signal<TchClaim | null>(null);

  // ===== Exposition =====
  isAuthenticated = computed(() => this._isAuth());
  isAuthenticated$ = this.oidc.isAuthenticated$;
  accessToken(): string | null {
    return this._access();
  }
  idToken(): string | null {
    return this._id();
  }
  tch(): TchClaim | null {
    return this._tch();
  }

  // ===== Single-flight holder =====
  private inflight$?: Observable<LoginResponse>;

  // ===== Boot (APP_INITIALIZER) =====
  /** Appelé au démarrage: restaure la session si tokens présents, sinon ne fait rien. */
  init(): Promise<void> {
    return this.checkAuth$()
      .pipe(take(1))
      .toPromise()
      .then(r => this.hydrateFrom(r));
  }

  // ===== Login/Logout =====
  /** Démarre le flow OIDC; target sera utilisée après callback. */
  login(target: string = '/app') {
    sessionStorage.setItem('login_target', target);
    this.oidc.authorize();
  }

  logout() {
    this.oidc.logoffAndRevokeTokens().subscribe();
    this._isAuth.set(false);
    this._access.set(null);
    this._id.set(null);
    this._tch.set(null);
  }

  // ===== Callback / Check =====
  /** checkAuth est idempotent; on le single-flight pour éviter les doublons. */
  checkAuth$(): Observable<LoginResponse> {
    if (!this.inflight$) {
      this.inflight$ = this.oidc.checkAuth().pipe(
        shareReplay({ bufferSize: 1, refCount: false }),
        finalize(() => {
          this.inflight$ = undefined;
        }),
      );
    }
    return this.inflight$;
  }

  /** Hydrate les signals à partir du résultat OIDC. */
  hydrateFrom(login: LoginResponse | null | undefined) {
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
    this.events.registerForEvents().subscribe(e => {
      switch (e.type) {
        case EventTypes.CheckSessionReceived: {
          // rafraîchissement de session / reconnexion implicite
          // on réhydrate l'état à partir de l'oidc (accès/ID tokens actuels)
          this.oidc.isAuthenticated$.pipe(take(1)).subscribe(({ isAuthenticated }) => {
            this.hydrateFrom({
              isAuthenticated,
              accessToken: this.oidc.getAccessToken(),
              idToken: this.oidc.getIdToken(),
            } as any);
          });
          break;
        }
        case EventTypes.NewAuthenticationResult: {
          // silent refresh réussi → mettre à jour tokens/signals
          this.hydrateFrom({
            isAuthenticated: true,
            accessToken: this.oidc.getAccessToken(),
            idToken: this.oidc.getIdToken(),
          } as any);
          break;
        }
        // tu peux aussi écouter RefreshSessionError, TokenExpired, etc. selon besoins
      }
    });
  }
}
