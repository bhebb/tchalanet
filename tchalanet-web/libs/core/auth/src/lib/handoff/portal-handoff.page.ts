import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { TchRuntimeConfigStore } from '@tch/shared-config';

import { AUTH_CLIENT } from '../auth-client';
import { AuthSessionService } from '../auth-session.service';
import { SupportAccessStore } from '../support/support-access.store';
import { PortalHandoffApi, PortalHandoffTarget } from './portal-handoff-api.service';

@Component({
  standalone: true,
  selector: 'tch-portal-handoff-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '',
})
export class PortalHandoffPage implements OnInit {
  private readonly auth = inject(AUTH_CLIENT);
  private readonly authSession = inject(AuthSessionService);
  private readonly document = inject(DOCUMENT);
  private readonly handoffs = inject(PortalHandoffApi);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly runtimeConfig = inject(TchRuntimeConfigStore);
  private readonly supportAccess = inject(SupportAccessStore);

  ngOnInit(): void {
    void this.consume();
  }

  private async consume(): Promise<void> {
    const parsed = this.readCode();
    this.stripFragment();

    if (!parsed || !this.auth.signInWithCustomToken) {
      await this.fail('invalid');
      return;
    }

    try {
      const targetPortal = this.targetPortal();
      const response = await firstValueFrom(
        this.handoffs.consume(parsed.handoffId, parsed.code, targetPortal, {
          suppressShellFeedback: true,
        }),
      );
      await this.auth.signInWithCustomToken(response.customToken);
      await this.authSession.refreshSession(true);
      await this.supportAccess.hydrateCurrent();
      await this.router.navigateByUrl(response.entryRoute, { replaceUrl: true });
    } catch {
      await this.fail('consume_failed');
    }
  }

  private readCode(): { handoffId: string; code: string } | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    const hash = this.document.defaultView?.location.hash ?? '';
    const params = new URLSearchParams(hash.startsWith('#') ? hash.slice(1) : hash);
    const raw = params.get('code');
    const separator = raw?.indexOf('.') ?? -1;
    if (!raw || separator <= 0 || separator === raw.length - 1) {
      return null;
    }
    return {
      handoffId: raw.slice(0, separator),
      code: raw.slice(separator + 1),
    };
  }

  private stripFragment(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const window = this.document.defaultView;
    if (!window) return;
    window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}`);
  }

  private targetPortal(): PortalHandoffTarget {
    return this.runtimeConfig.config().appId === 'platform-portal' ? 'PLATFORM' : 'ADMIN';
  }

  private fail(code: string): Promise<boolean> {
    return this.router.navigate(['/login'], {
      queryParams: { handoff: code },
      replaceUrl: true,
    });
  }
}
