import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ActionItem, actionText } from '@tch/api';
import { LabelPipe, PublicShellRuntime } from '@tch/page-model';
import { TchActionButton, TchBrand, TchNav, TchOverlayNav } from '@tch/ui/components';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcher } from '../../../core/i18n';

const COMPACT_LABEL_MAP: Record<string, string> = {
  'public.nav.check_ticket': 'public.nav.verify_short',
  'public.nav.operators':    'public.nav.operators_short',
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
  template: `
    <header class="public-header">
      <div class="public-header__inner">
        <button
          type="button"
          mat-icon-button
          class="public-header__menu-button"
          [attr.aria-expanded]="mobileMenuOpen()"
          [attr.aria-label]="(mobileMenuOpen() ? 'public.nav.menu_close' : 'public.nav.menu_open') | tchLabel"
          (click)="toggleMobileMenu()"
          aria-controls="public-overlay-nav"
        >
          <mat-icon>{{ mobileMenuOpen() ? 'close' : 'menu' }}</mat-icon>
        </button>

        <tch-brand class="public-header__brand" [brand]="brand()" [showName]="false" />

        <!-- Tablet nav: compact labels, hidden on mobile and desktop -->
        <tch-nav
          class="public-header__nav public-header__nav--short"
          [items]="navCompact()"
          [ariaLabel]="'public.nav.main' | tchLabel"
        />

        <!-- Desktop nav: full labels, hidden on mobile and tablet -->
        <tch-nav
          class="public-header__nav public-header__nav--long"
          [items]="nav()"
          [ariaLabel]="'public.nav.main' | tchLabel"
        />

        <div class="public-header__actions">
          <div class="public-header__tools">
            <tch-language-switcher />
          </div>
          @if (loginAction(); as loginItem) {
            <button tch-action type="button" (click)="login()">
              {{ actionText(loginItem) | tchLabel }}
            </button>
          }
        </div>
      </div>

      <tch-overlay-nav
        id="public-overlay-nav"
        [open]="mobileMenuOpen()"
        [items]="nav()"
        [ariaLabel]="'public.nav.main' | tchLabel"
        (requestClose)="closeMobileMenu()"
      />
    </header>
  `,
  styles: [`
    :host {
      display: block;
      position: sticky;
      top: 0;
      z-index: var(--tch-z-header, 30);
    }

    .public-header {
      --comp-header-bg: var(--tch-color-surface-container-lowest);
      --comp-header-fg: var(--tch-color-on-surface);
      --comp-header-border: var(--tch-color-outline-variant);
      width: 100%;
      background: color-mix(in oklab, var(--comp-header-bg) 92%, transparent);
      color: var(--comp-header-fg);
      border-bottom: 1px solid var(--comp-header-border);
      backdrop-filter: blur(16px);
    }

    .public-header__inner {
      min-height: 3.5rem;
      width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
      margin: 0 auto;
      display: flex;
      align-items: center;
      gap: 0.375rem;
    }

    .public-header__menu-button {
      flex: 0 0 auto;
      color: var(--tch-color-primary);
    }

    .public-header__brand {
      display: inline-flex;
      align-items: center;
      gap: 0.625rem;
      color: var(--tch-color-primary);
      text-decoration: none;
      font-weight: var(--tch-weight-extra-bold, 800);
      min-width: 0;
      flex: 1 1 auto;
      /* clip to icon mark only on narrow mobile */
      max-width: 2.5rem;
      overflow: hidden;
    }

    @media (min-width: 480px) {
      .public-header__brand {
        max-width: none;
        overflow: visible;
      }
    }

    /* Both nav variants hidden on mobile */
    .public-header__nav {
      display: none;
    }

    .public-header__actions {
      flex: 0 0 auto;
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      margin-left: auto;
    }

    .public-header__tools {
      display: flex;
      align-items: center;
    }

    /* Compact login button on mobile */
    .public-header__actions .tch-action {
      font-size: 0.875rem;
      padding: 0 0.875rem;
      min-height: 2.75rem;
      border-radius: 14px;
    }

    /* Gold accent on login CTA — secondary-container is lavender in M3 palette */
    .tch-action {
      --comp-action-bg: var(--tch-color-accent);
      --comp-action-fg: var(--tch-on-color-accent, #1a1b4b);
    }

    /* ── Tablet ≥ 768px: compact nav, brand with text, burger hidden ── */
    @media (min-width: 768px) {
      .public-header__inner {
        min-height: 4rem;
        gap: clamp(0.5rem, 1.5vw, 1rem);
        width: min(100% - 2 * var(--tch-page-margin-desktop, 32px), 1120px);
      }

      .public-header__menu-button {
        display: none;
      }

      .public-header__brand {
        flex: 0 0 auto;
      }

      .public-header__nav--short {
        display: flex;
        flex: 1 1 auto;
        flex-wrap: nowrap;
        justify-content: flex-start;
        min-width: 0;
        --comp-nav-hover-bg: color-mix(in srgb, var(--tch-color-accent) 14%, transparent);
        --comp-nav-active: var(--tch-color-primary);
        --comp-nav-active-indicator: var(--tch-color-accent);
      }

      .public-header__nav--long {
        display: none;
      }

      .public-header__actions {
        gap: 0.5rem;
      }

      .public-header__tools {
        gap: 0.25rem;
      }

      .public-header__actions .tch-action {
        font-size: 1rem;
        padding: 0 1.25rem;
        min-height: var(--tch-touch-target, 48px);
        border-radius: var(--tch-radius-md, 8px);
      }
    }

    /* ── Desktop ≥ 1024px: full-label nav ── */
    @media (min-width: 1024px) {
      .public-header__inner {
        gap: clamp(0.75rem, 2vw, 1.5rem);
      }

      .public-header__nav--short {
        display: none;
      }

      .public-header__nav--long {
        display: flex;
        flex: 1 1 auto;
        flex-wrap: nowrap;
        justify-content: flex-start;
        min-width: 0;
        --comp-nav-hover-bg: color-mix(in srgb, var(--tch-color-accent) 14%, transparent);
        --comp-nav-active: var(--tch-color-primary);
        --comp-nav-active-indicator: var(--tch-color-accent);
      }

      .public-header__actions {
        gap: 0.75rem;
      }
    }
  `],
})
export class PublicHeader {
  private readonly auth = inject(AuthSessionService);

  readonly shell = input<PublicShellRuntime | undefined>();
  readonly mobileMenuOpen = signal(false);
  readonly brand = computed(() => publicBrand(this.shell()));
  readonly nav = computed(() => publicHeaderNav(this.shell()));
  readonly navCompact = computed(() => toCompactNav(this.nav()));
  readonly loginAction = computed(() => publicLoginAction(this.shell()));

  login(): void {
    void this.auth.login();
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update((open) => !open);
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
  const actions = [
    ...(shell?.header.actions ?? []),
    ...(shell?.header.secondary ?? []),
  ];
  return actions.find((item) => item.id === 'login') ?? actions[0];
}

function toCompactNav(items: readonly ActionItem[]): readonly ActionItem[] {
  return items.map(item => {
    const shortKey = item.labelKey ? COMPACT_LABEL_MAP[item.labelKey] : undefined;
    return shortKey ? { ...item, labelKey: shortKey } : item;
  });
}
