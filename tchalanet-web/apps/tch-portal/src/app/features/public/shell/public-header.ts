import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ActionItem, actionText } from '@tch/api';
import { LabelPipe, PublicShellRuntime } from '@tch/page-model';
import { TchActionButton, TchBrand, TchNav, TchOverlayNav } from '@tch/ui/components';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcher } from '../../../core/i18n';
import { Router } from '@angular/router';

const COMPACT_LABEL_MAP: Record<string, string> = {
  'public.nav.check_ticket': 'public.nav.verify_short',
  'public.nav.operators': 'public.nav.operators_short',
};

@Component({
  selector: 'tch-public-header',
  imports: [
    MatButtonModule,
    MatIconModule,
    LabelPipe,
    TchActionButton,
    TchBrand,
    TchNav,
    TchOverlayNav,
    LanguageSwitcher,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-header.html',
  styleUrls: ['./public-header.scss'],
})
export class PublicHeader {
  private readonly auth = inject(AuthSessionService);
  private readonly router = inject(Router);

  readonly shell = input<PublicShellRuntime | undefined>();
  readonly mobileMenuOpen = signal(false);
  readonly brand = computed(() => publicBrand(this.shell()));
  readonly nav = computed(() => publicHeaderNav(this.shell()));
  readonly navCompact = computed(() => toCompactNav(this.nav()));
  readonly loginAction = computed(() => publicLoginAction(this.shell()));

  login(): void {
    // Land on the /app dispatcher after Keycloak so the user is routed
    // to the dashboard matching their role, not back to the public page.
    // void this.auth.login(globalThis.location.origin + '/app');
    this.router.navigate(['/login']);
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(open => !open);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  protected readonly actionText = actionText;
}

function publicBrand(shell: PublicShellRuntime | undefined): ActionItem | undefined {
  const brand = shell?.header.brand;
  return {
    id: brand?.id ?? 'public-brand',
    ...brand,
    image: brand?.image ?? '/assets/brand/tchalanet-logo.svg',
    destination: brand?.destination ?? { kind: 'route', value: '/public' },
  };
}

function publicHeaderNav(shell: PublicShellRuntime | undefined): readonly ActionItem[] {
  return shell?.header.primary ?? [];
}

function publicLoginAction(shell: PublicShellRuntime | undefined): ActionItem | undefined {
  const actions = [...(shell?.header.actions ?? []), ...(shell?.header.secondary ?? [])];
  return actions.find(item => item.id === 'login') ?? actions[0];
}

function toCompactNav(items: readonly ActionItem[]): readonly ActionItem[] {
  return items.map(item => {
    const shortKey = item.labelKey ? COMPACT_LABEL_MAP[item.labelKey] : undefined;
    return shortKey ? { ...item, labelKey: shortKey } : item;
  });
}
