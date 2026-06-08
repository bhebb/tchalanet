import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcherComponent } from '../../../core/i18n';
import { LabelPipe, PublicShellRuntime } from '@tch/page-model';
import { ActionItem, actionText } from '@tch/api';
import { TchBrand, TchNav, TchOverlayNav } from '@tch/ui/components';

@Component({
  selector: 'tch-public-header',
  imports: [LanguageSwitcherComponent, LabelPipe, TchBrand, TchNav, TchOverlayNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="public-header">
      <div class="public-header__inner">
        <tch-brand class="public-header__brand" [brand]="brand()" [showName]="false" />

        <tch-nav class="public-header__nav" [items]="nav()" ariaLabel="Navigation publique" />

        <div class="public-header__actions">
          <div class="public-header__tools" aria-label="Langue">
            <tch-language-switcher />
          </div>
          @if (loginAction(); as loginItem) {
            <button type="button" class="public-header__login" (click)="login()">
              {{ actionText(loginItem) | tchLabel }}
            </button>
          }
        </div>

        <button
          type="button"
          class="public-header__burger"
          [attr.aria-expanded]="mobileMenuOpen()"
          [attr.aria-label]="'public.nav.menu' | tchLabel"
          (click)="toggleMobileMenu()"
          aria-controls="public-overlay-nav"
        >
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
        </button>
      </div>

      <tch-overlay-nav
        id="public-overlay-nav"
        [open]="mobileMenuOpen()"
        [items]="nav()"
        ariaLabel="Navigation publique mobile"
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
      gap: 0.75rem;
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

    /* Mobile-first: nav and tools hidden on compact */
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

    .public-header__login {
      min-height: 2.5rem;
      padding: 0 0.875rem;
      border-radius: var(--tch-radius-md, 8px);
      border: 0;
      background: var(--tch-color-accent);
      color: var(--tch-on-color-accent);
      cursor: pointer;
      font-weight: var(--tch-weight-extra-bold, 800);
      font-size: 0.875rem;
    }

    /* Burger: visible on compact */
    .public-header__burger {
      flex: 0 0 auto;
      width: var(--tch-touch-target, 48px);
      height: var(--tch-touch-target, 48px);
      display: grid;
      place-items: center;
      gap: 0;
      border: 1px solid var(--tch-color-outline-variant);
      border-radius: var(--tch-radius-md, 8px);
      background: transparent;
      color: var(--tch-color-primary);
      cursor: pointer;
    }

    .public-header__burger span {
      width: 1.25rem;
      height: 2px;
      display: block;
      border-radius: 9999px;
      background: currentColor;
    }

    /* Expanded layout ≥ 840px */
    @media (min-width: 840px) {
      .public-header__inner {
        min-height: 4.5rem;
        gap: clamp(0.75rem, 2vw, 1.25rem);
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
        gap: 0.25rem;
        min-width: 0;
      }

      .public-header__nav a {
        min-height: 2.5rem;
        display: inline-flex;
        align-items: center;
        padding: 0 0.625rem;
        border-radius: var(--tch-radius-pill, 9999px);
        color: var(--tch-color-on-surface-variant);
        text-decoration: none;
        font-weight: var(--tch-weight-extra-bold, 800);
        font-size: 0.875rem;
        white-space: nowrap;
      }

      .public-header__nav a:hover {
        background: var(--tch-color-surface-container);
        color: var(--tch-color-primary);
      }

      .public-header__actions {
        gap: 0.625rem;
      }

      .public-header__tools {
        display: flex;
        align-items: center;
        gap: 0.375rem;
      }

      .public-header__login {
        min-height: var(--tch-touch-target, 48px);
        padding: 0 1.125rem;
      }

      .public-header__burger {
        display: none;
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
