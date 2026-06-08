import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ActionItem, actionText } from '@tch/api';
import { LabelPipe, PublicShellRuntime } from '@tch/page-model';
import { TchActionButton, TchBrand, TchNav, TchOverlayNav } from '@tch/ui/components';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcherComponent } from '../../../core/i18n';

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
    LanguageSwitcherComponent,
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

        <tch-nav
          class="public-header__nav"
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
    .public-header {
      --comp-header-bg: var(--tch-color-surface-container-lowest);
      --comp-header-fg: var(--tch-color-on-surface);
      --comp-header-border: var(--tch-color-outline-variant);
      position: sticky;
      top: 0;
      z-index: var(--tch-z-header, 30);
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
      flex: 1 1 auto;
    }

    .public-header__brand img {
      width: 8.75rem;
      height: auto;
      display: block;
    }

    .public-header__nav {
      display: none;
    }

    .public-header__actions {
      flex: 0 0 auto;
      display: flex;
      align-items: center;
      gap: 0;
    }

    .public-header__tools {
      display: none;
    }

    /* Force gold accent on login CTA — secondary-container is lavender in M3 palette */
    .tch-action {
      --comp-action-bg: var(--tch-color-accent);
      --comp-action-fg: var(--tch-on-color-accent, #1a1b4b);
    }

    @media (min-width: 840px) {
      .public-header__inner {
        min-height: 4rem;
        gap: clamp(0.5rem, 2vw, 1rem);
      }

      .public-header__menu-button {
        display: none;
      }

      .public-header__brand {
        flex: 0 0 auto;
      }

      .public-header__brand img {
        width: 10rem;
      }

      .public-header__nav {
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
        gap: 0.5rem;
      }

      .public-header__tools {
        display: flex;
        align-items: center;
        gap: 0.375rem;
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
