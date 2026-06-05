import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ThemeSwitcherComponent } from '@tch/shared/theme/runtime/theme-switcher.component';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcherComponent } from '../../../core/i18n';
import { PageDynamicPayload, PageShell } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import { isRecord, toPublicPath } from '../widget.contract';

interface PublicNavDestination {
  readonly id?: string;
  readonly label_key?: string;
  readonly path?: string;
}

interface PublicFooterColumn {
  readonly titleKey: string;
  readonly links: readonly PublicNavDestination[];
}

interface PublicShellFragment {
  readonly brand?: unknown;
  readonly primary?: readonly unknown[];
  readonly secondary?: readonly unknown[];
  readonly actions?: readonly unknown[];
  readonly social?: readonly unknown[];
}

const PUBLIC_HEADER_NAV: readonly PublicNavDestination[] = [
  { id: 'results', label_key: 'public.nav.results', path: '/public/results' },
  { id: 'check_ticket', label_key: 'public.nav.check_ticket', path: '/public/check-ticket' },
  { id: 'help', label_key: 'public.nav.help', path: '/public/help' },
  { id: 'operators', label_key: 'public.nav.operators', path: '/public/contact' },
];

const PUBLIC_HEADER_FALLBACK: PublicShellFragment = {
  brand: {
    id: 'tchalanet-public',
    label_key: 'app.brand',
    path: '/public',
  },
  primary: [
    {
      id: 'results',
      type: 'destination',
      label_key: 'public.nav.results',
      path: '/public/results',
      kind: 'internal',
    },
    {
      id: 'check_ticket',
      type: 'destination',
      label_key: 'public.nav.check_ticket',
      path: '/public/check-ticket',
      kind: 'internal',
    },
    {
      id: 'help',
      type: 'destination',
      label_key: 'public.nav.help',
      path: '/public/help',
      kind: 'internal',
    },
    {
      id: 'operators',
      type: 'destination',
      label_key: 'public.nav.operators',
      path: '/public/contact',
      kind: 'internal',
    },
  ],
  secondary: [
    {
      id: 'login',
      type: 'destination',
      label_key: 'public.nav.login',
      path: '/login',
      kind: 'internal',
    },
  ],
};

const PUBLIC_BOTTOM_NAV: readonly PublicNavDestination[] = [
  { id: 'results', label_key: 'public.nav.results', path: '/public/results' },
  { id: 'verify', label_key: 'public.nav.verify_short', path: '/public/check-ticket' },
  { id: 'help', label_key: 'public.nav.help', path: '/public/help' },
];

const PUBLIC_FOOTER_NAV: readonly PublicNavDestination[] = [
  { id: 'operators', label_key: 'public.footer.solutions.operators', path: '/public/contact' },
  { id: 'check_ticket', label_key: 'public.footer.solutions.check_ticket', path: '/public/check-ticket' },
  { id: 'results', label_key: 'public.footer.support.results', path: '/public/results' },
  { id: 'help', label_key: 'public.footer.support.help', path: '/public/help' },
  { id: 'privacy', label_key: 'public.footer.legal.privacy', path: '/public/privacy' },
  { id: 'terms', label_key: 'public.footer.legal.terms', path: '/public/terms' },
];

const PUBLIC_FOOTER_FALLBACK: PublicShellFragment = {
  brand: {
    id: 'tchalanet-footer',
    label_key: 'app.brand',
    path: '/public',
  },
  primary: [
    {
      id: 'operators',
      type: 'destination',
      label_key: 'public.footer.solutions.operators',
      path: '/public/contact',
      kind: 'internal',
    },
    {
      id: 'check_ticket',
      type: 'destination',
      label_key: 'public.footer.solutions.check_ticket',
      path: '/public/check-ticket',
      kind: 'internal',
    },
    {
      id: 'pos_management',
      type: 'destination',
      label_key: 'public.footer.solutions.pos_management',
      path: '/public/contact',
      kind: 'internal',
    },
    {
      id: 'results',
      type: 'destination',
      label_key: 'public.footer.support.results',
      path: '/public/results',
      kind: 'internal',
    },
  ],
  secondary: [
    {
      id: 'help',
      type: 'destination',
      label_key: 'public.footer.support.help',
      path: '/public/help',
      kind: 'internal',
    },
    {
      id: 'status',
      type: 'destination',
      label_key: 'public.footer.support.status',
      path: '/public/contact',
      kind: 'internal',
    },
    {
      id: 'contact',
      type: 'destination',
      label_key: 'public.footer.support.contact',
      path: '/public/contact',
      kind: 'internal',
    },
    {
      id: 'privacy',
      type: 'destination',
      label_key: 'public.footer.legal.privacy',
      path: '/public/privacy',
      kind: 'internal',
    },
    {
      id: 'terms',
      type: 'destination',
      label_key: 'public.footer.legal.terms',
      path: '/public/terms',
      kind: 'internal',
    },
    {
      id: 'compliance',
      type: 'destination',
      label_key: 'public.footer.legal.compliance',
      path: '/public/terms',
      kind: 'internal',
    },
  ],
  social: [
    {
      id: 'linkedin',
      type: 'destination',
      label_key: 'footer.linkedin',
      path: 'https://www.linkedin.com/company/tchalanet',
      kind: 'external',
    },
  ],
};

const PUBLIC_FOOTER_COLUMNS: readonly PublicFooterColumn[] = [
  {
    titleKey: 'public.footer.solutions.title',
    links: [
      { id: 'operators', label_key: 'public.footer.solutions.operators', path: '/public/contact' },
      {
        id: 'check_ticket',
        label_key: 'public.footer.solutions.check_ticket',
        path: '/public/check-ticket',
      },
      {
        id: 'pos_management',
        label_key: 'public.footer.solutions.pos_management',
        path: '/public/contact',
      },
    ],
  },
  {
    titleKey: 'public.footer.support.title',
    links: [
      { id: 'help', label_key: 'public.footer.support.help', path: '/public/help' },
      { id: 'results', label_key: 'public.footer.support.results', path: '/public/results' },
      { id: 'rules', label_key: 'public.footer.support.rules', path: '/public/rules' },
      { id: 'status', label_key: 'public.footer.support.status', path: '/public/contact' },
      { id: 'contact', label_key: 'public.footer.support.contact', path: '/public/contact' },
    ],
  },
  {
    titleKey: 'public.footer.legal.title',
    links: [
      { id: 'privacy', label_key: 'public.footer.legal.privacy', path: '/public/privacy' },
      { id: 'terms', label_key: 'public.footer.legal.terms', path: '/public/terms' },
      { id: 'compliance', label_key: 'public.footer.legal.compliance', path: '/public/terms' },
    ],
  },
];

/**
 * Lightweight public shell: renders a top app bar and footer from the `shell` config, with the
 * existing language/theme switchers, and projects the page body. Not a generic shell engine —
 * just header + footer + content per the design's composition (top app bar, body, simple footer).
 */
@Component({
  selector: 'tch-page-shell',
  imports: [RouterLink, LanguageSwitcherComponent, ThemeSwitcherComponent, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="shell__skip" href="#public-content">{{ 'public.nav.skip' | tchLabel }}</a>

    <header class="public-header">
      <div class="public-header__inner">
        <a class="public-header__brand" routerLink="/public" aria-label="Tchalanet">
          <img src="/assets/brand/tchalanet-logo.svg" alt="Tchalanet" />
        </a>

        <button
          type="button"
          class="public-header__burger"
          [attr.aria-expanded]="mobileMenuOpen()"
          aria-controls="public-mobile-menu"
          (click)="toggleMobileMenu()"
        >
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
          <span class="public-header__sr">{{ 'public.nav.menu' | tchLabel }}</span>
        </button>

        <nav class="public-header__nav" aria-label="Navigation publique">
          @for (item of headerNav(); track item.path) {
            <a [routerLink]="item.path">{{ item.label_key | tchLabel }}</a>
          }
        </nav>

        <div class="public-header__actions">
          <div class="public-header__tools" aria-label="Langue et thème">
            <tch-language-switcher />
            <tch-theme-switcher />
          </div>
          <button type="button" class="public-header__login" (click)="login()">
            {{ 'public.nav.login' | tchLabel }}
          </button>
        </div>
      </div>

      <div
        id="public-mobile-menu"
        class="public-header__mobile-panel"
        [class.public-header__mobile-panel--open]="mobileMenuOpen()"
      >
        <nav class="public-header__mobile-nav" aria-label="Navigation publique mobile">
          @for (item of headerNav(); track item.path) {
            <a [routerLink]="item.path" (click)="closeMobileMenu()">{{ item.label_key | tchLabel }}</a>
          }
        </nav>
        <div class="public-header__mobile-tools">
          <tch-language-switcher />
          <tch-theme-switcher />
        </div>
      </div>
    </header>

    <main id="public-content" class="shell__body">
      <ng-content />
    </main>

    <footer class="public-footer">
      <div class="public-footer__inner">
        <section class="public-footer__brand" aria-label="Tchalanet">
          <a class="public-footer__brand-link" routerLink="/public">
            <img src="/assets/brand/tchalanet-logo-inverse.svg" alt="Tchalanet" />
          </a>
          <p>{{ 'public.footer.description' | tchLabel }}</p>
          <div class="public-footer__status">
            <span aria-hidden="true"></span>
            {{ 'public.footer.status.operational' | tchLabel }}
          </div>
        </section>

        <nav class="public-footer__columns" aria-label="Pied de page public">
          @for (column of footerColumns(); track column.titleKey) {
            <section class="public-footer__column">
              <h2>{{ column.titleKey | tchLabel }}</h2>
              @for (item of column.links; track item.id ?? item.path) {
                <a [routerLink]="item.path">{{ item.label_key | tchLabel }}</a>
              }
            </section>
          }
        </nav>
      </div>
      <div class="public-footer__bottom">
        <small>{{ 'app.footer.copyright' | tchLabel }}</small>
      </div>
    </footer>

    <nav class="shell__bottom-nav" aria-label="Public">
      @for (item of bottomNav(); track item.id ?? item.path) {
        <a [routerLink]="item.path">{{ item.label_key | tchLabel }}</a>
      }
    </nav>
  `,
  styles: [
    `
      :host {
        display: grid;
        min-height: 100vh;
        grid-template-rows: auto 1fr auto;
        background: var(--tch-color-background, var(--mat-sys-background));
        color: var(--tch-color-foreground, var(--mat-sys-on-background));
      }
      .shell__skip {
        position: fixed;
        z-index: 100;
        left: 1rem;
        top: 1rem;
        transform: translateY(-5rem);
        padding: 0.625rem 0.875rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        text-decoration: none;
        font-weight: 800;
      }
      .shell__skip:focus {
        transform: translateY(0);
      }
      .public-header {
        position: sticky;
        top: 0;
        z-index: 30;
        background: color-mix(
          in oklab,
          var(--tch-color-surface-container-lowest, #fff) 92%,
          transparent
        );
        border-bottom: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        backdrop-filter: blur(16px);
      }
      .public-header__inner {
        min-height: 4.5rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
        margin: 0 auto;
        display: flex;
        align-items: center;
        gap: clamp(0.75rem, 2vw, 1.25rem);
      }
      .public-header__brand,
      .public-footer__brand-link {
        display: inline-flex;
        align-items: center;
        gap: 0.625rem;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        text-decoration: none;
        font-weight: 900;
      }
      .public-header__brand img {
        width: 10rem;
        height: auto;
        display: block;
      }
      .public-footer__brand-link img {
        width: 11rem;
        height: auto;
        display: block;
      }
      .public-header__burger {
        display: none;
      }
      .public-header__sr {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
        white-space: nowrap;
        border: 0;
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
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        text-decoration: none;
        font-weight: 800;
        font-size: 0.875rem;
        white-space: nowrap;
      }
      .public-header__nav a:hover {
        background: var(--tch-color-surface-tonal, var(--mat-sys-surface-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }
      .public-header__actions {
        flex: 0 0 auto;
        display: flex;
        align-items: center;
        justify-content: flex-end;
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
        border-radius: var(--tch-radius-control, 8px);
        border: 0;
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        cursor: pointer;
        font-weight: 900;
      }
      .public-header__mobile-panel {
        display: none;
      }
      .public-footer {
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }
      .public-footer__inner {
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
        margin: 0 auto;
        display: grid;
        grid-template-columns: minmax(16rem, 1.2fr) 2fr;
        gap: clamp(2rem, 6vw, 4rem);
        padding: clamp(2.5rem, 8vw, 4rem) 0;
      }
      .public-footer__brand {
        display: grid;
        gap: 1rem;
        align-content: start;
      }
      .public-footer__brand-link {
        color: inherit;
        font-size: 1.25rem;
      }
      .public-footer__brand p {
        margin: 0;
        max-width: 22rem;
        color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
      }
      .public-footer__status {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
        font-weight: 800;
      }
      .public-footer__status span {
        width: 0.625rem;
        height: 0.625rem;
        border-radius: 9999px;
        background: var(--tch-color-status-ready, #10b981);
      }
      .public-footer__columns {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1.5rem;
      }
      .public-footer__column {
        display: grid;
        align-content: start;
        gap: 0.625rem;
      }
      .public-footer__column h2 {
        margin: 0 0 0.25rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: var(--tch-line-height-label-sm, 1rem);
        text-transform: uppercase;
        color: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
      }
      .public-footer__column a {
        color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
        text-decoration: none;
      }
      .public-footer__column a:hover {
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
      }
      .public-footer__bottom {
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
        margin: 0 auto;
        padding: 1rem 0;
        border-top: 1px solid color-mix(in oklab, currentColor 18%, transparent);
        color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
      }
      .shell__bottom-nav {
        display: none;
      }
      @media (max-width: 720px) {
        .public-header__inner {
          min-height: auto;
          gap: 0.75rem;
          padding: 0.75rem 0;
        }
        .public-header__brand {
          flex: 1 1 auto;
        }
        .public-header__brand img {
          width: 8.75rem;
        }
        .public-header__nav {
          display: none;
        }
        .public-header__burger {
          flex: 0 0 auto;
          width: var(--tch-touch-target, 48px);
          height: var(--tch-touch-target, 48px);
          display: grid;
          place-items: center;
          gap: 0;
          border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
          border-radius: var(--tch-radius-control, 8px);
          background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
          color: var(--tch-color-primary, var(--mat-sys-primary));
        }
        .public-header__burger span:not(.public-header__sr) {
          width: 1.25rem;
          height: 2px;
          display: block;
          border-radius: 9999px;
          background: currentColor;
        }
        .public-header__actions {
          gap: 0;
        }
        .public-header__tools {
          display: none;
        }
        .public-header__login {
          min-height: 2.5rem;
          padding: 0 0.875rem;
        }
        .public-header__mobile-panel {
          width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
          margin: 0 auto;
          padding: 0 0 0.875rem;
        }
        .public-header__mobile-panel--open {
          display: grid;
          gap: 0.75rem;
        }
        .public-header__mobile-nav {
          display: grid;
          gap: 0.375rem;
          padding: 0.75rem;
          border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
          border-radius: var(--tch-radius-lg, 12px);
          background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
          box-shadow: var(--mat-sys-level2, 0 8px 24px rgba(0, 0, 0, 0.12));
        }
        .public-header__mobile-nav a {
          min-height: var(--tch-touch-target, 48px);
          display: flex;
          align-items: center;
          padding: 0 0.875rem;
          border-radius: var(--tch-radius-control, 8px);
          color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
          text-decoration: none;
          font-weight: 850;
        }
        .public-header__mobile-nav a:nth-child(2) {
          background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
          color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        }
        .public-header__mobile-tools {
          display: flex;
          gap: 0.5rem;
          align-items: center;
          justify-content: space-between;
          padding: 0.75rem;
          border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
          border-radius: var(--tch-radius-lg, 12px);
          background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        }
        .public-footer__inner {
          grid-template-columns: 1fr;
          padding-bottom: 2rem;
        }
        .public-footer__columns {
          grid-template-columns: 1fr;
        }
        .public-footer__bottom {
          padding-bottom: calc(5rem + env(safe-area-inset-bottom, 0px));
        }
        .shell__body {
          padding-bottom: calc(4.5rem + env(safe-area-inset-bottom, 0px));
        }
        .shell__bottom-nav {
          position: fixed;
          z-index: 20;
          left: 0;
          right: 0;
          bottom: 0;
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 0.25rem;
          padding: 0.5rem var(--tch-page-margin-mobile, 16px)
            calc(0.5rem + env(safe-area-inset-bottom, 0px));
          background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
          border-top: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
          box-shadow: var(--mat-sys-level2, 0 -4px 16px rgba(0, 0, 0, 0.12));
        }
        .shell__bottom-nav a {
          min-height: var(--tch-touch-target, 48px);
          display: grid;
          place-items: center;
          border-radius: var(--tch-radius-control, 8px);
          color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
          text-decoration: none;
          font-weight: 700;
        }
        .shell__bottom-nav a:nth-child(2) {
          background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
          color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        }
      }
    `,
  ],
})
export class PageShellComponent {
  private readonly auth = inject(AuthSessionService);

  readonly shell = input<PageShell>();
  readonly dynamic = input<PageDynamicPayload>();

  readonly headerNav = computed(() => publicHeaderNav(this.headerFragment()));
  readonly footerColumns = computed(() => publicFooterColumns(this.footerFragment()));
  readonly bottomNav = computed(() => PUBLIC_BOTTOM_NAV);
  readonly mobileMenuOpen = signal(false);
  private readonly headerFragment = computed(() =>
    publicShellFragment(this.dynamic(), 'shell.header', PUBLIC_HEADER_FALLBACK),
  );
  private readonly footerFragment = computed(() =>
    publicShellFragment(this.dynamic(), 'shell.footer', PUBLIC_FOOTER_FALLBACK),
  );

  login(): void {
    void this.auth.login();
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(open => !open);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}

function publicShellFragment(
  dynamic: PageDynamicPayload | undefined,
  key: string,
  fallback: PublicShellFragment,
): PublicShellFragment | undefined {
  const value = dynamic?.widgets?.[key];
  return mergeShellFragment(isRecord(value) ? value : undefined, fallback);
}

function mergeShellFragment(
  backend: PublicShellFragment | undefined,
  fallback: PublicShellFragment,
): PublicShellFragment {
  return {
    brand: backend?.brand ?? fallback.brand,
    primary: mergeDestinations(backend?.primary, fallback.primary),
    secondary: mergeDestinations(backend?.secondary, fallback.secondary),
    actions: mergeDestinations(backend?.actions, fallback.actions),
    social: mergeDestinations(backend?.social, fallback.social),
  };
}

function mergeDestinations(
  backendItems: readonly unknown[] | undefined,
  fallbackItems: readonly unknown[] | undefined,
): readonly unknown[] {
  const backend = backendItems ?? [];
  const fallback = fallbackItems ?? [];
  const backendIds = new Set(
    backend.flatMap(item => isRecord(item) && typeof item['id'] === 'string' ? [item['id']] : []),
  );
  return [...backend, ...fallback.filter(item => isRecord(item) && !backendIds.has(String(item['id'])))];
}

function publicHeaderNav(fragment: PublicShellFragment | undefined): readonly PublicNavDestination[] {
  const normalized = normalizeDestinations(fragment?.primary).filter(item =>
    ['draw_results', 'results', 'check_ticket', 'help', 'operators'].includes(normalizePublicId(item.id)),
  );
  return mergePublicNav(PUBLIC_HEADER_NAV, normalized.map(normalizePublicNav));
}

function publicFooterNav(fragment: PublicShellFragment | undefined): readonly PublicNavDestination[] {
  const normalized = normalizeDestinations([
    ...(fragment?.primary ?? []),
    ...(fragment?.secondary ?? []),
  ]).filter(item =>
    [
      'results',
      'check_ticket',
      'games',
      'contact_demo',
      'support',
      'help',
      'status',
      'contact',
      'privacy',
      'terms',
      'operators',
      'pos_management',
      'responsible_gaming',
      'compliance',
    ].includes(normalizePublicId(item.id)),
  );
  return mergePublicNav(PUBLIC_FOOTER_NAV, normalized.map(normalizePublicNav));
}

function publicFooterColumns(fragment: PublicShellFragment | undefined): readonly PublicFooterColumn[] {
  const nav = publicFooterNav(fragment);
  return PUBLIC_FOOTER_COLUMNS.map(column => ({
    ...column,
    links: column.links.map(link => nav.find(item => item.id === link.id) ?? link),
  }));
}

function normalizeDestinations(items: readonly unknown[] | undefined): readonly PublicNavDestination[] {
  if (!items) {
    return [];
  }
  return items.flatMap(item => {
    if (!isRecord(item)) {
      return [];
    }
    const path = typeof item['path'] === 'string' ? toPublicPath(item['path']) : undefined;
    const labelKey = typeof item['label_key'] === 'string' ? item['label_key'] : undefined;
    return path && labelKey
      ? [{ id: typeof item['id'] === 'string' ? item['id'] : undefined, label_key: labelKey, path }]
      : [];
  });
}

function normalizePublicNav(item: PublicNavDestination): PublicNavDestination {
  if (item.id === 'draw_results' || item.id === 'results') {
    return { ...item, id: 'results', label_key: 'public.nav.results', path: '/public/results' };
  }
  if (item.id === 'check_ticket') {
    return { ...item, label_key: 'public.nav.check_ticket', path: '/public/check-ticket' };
  }
  if (item.id === 'support') {
    return { ...item, id: 'help', label_key: 'public.nav.help', path: '/public/help' };
  }
  if (item.id === 'help') {
    return { ...item, label_key: 'public.nav.help', path: '/public/help' };
  }
  if (item.id === 'games') {
    return { ...item, id: 'rules', label_key: 'public.footer.support.rules', path: '/public/rules' };
  }
  if (item.id === 'contact_demo' || item.id === 'contact') {
    return { ...item, id: 'contact', label_key: 'public.footer.support.contact', path: '/public/contact' };
  }
  if (item.id === 'status') {
    return { ...item, label_key: 'public.footer.support.status', path: '/public/contact' };
  }
  if (item.id === 'operators') {
    return { ...item, label_key: 'public.nav.operators', path: '/public/contact' };
  }
  if (item.id === 'pos_management') {
    return { ...item, label_key: 'public.footer.solutions.pos_management', path: '/public/contact' };
  }
  if (item.id === 'privacy') {
    return { ...item, label_key: 'public.footer.legal.privacy', path: '/public/privacy' };
  }
  if (item.id === 'terms') {
    return { ...item, label_key: 'public.footer.legal.terms', path: '/public/terms' };
  }
  if (item.id === 'responsible_gaming' || item.id === 'compliance') {
    return { ...item, id: 'compliance', label_key: 'public.footer.legal.compliance', path: '/public/terms' };
  }
  return item;
}

function normalizePublicId(id: string | undefined): string {
  if (id === 'draw_results') {
    return 'results';
  }
  if (id === 'support') {
    return 'help';
  }
  if (id === 'contact_demo') {
    return 'contact';
  }
  if (id === 'responsible_gaming') {
    return 'compliance';
  }
  return id ?? '';
}

function mergePublicNav(
  requiredItems: readonly PublicNavDestination[],
  backendItems: readonly PublicNavDestination[],
): readonly PublicNavDestination[] {
  return requiredItems.map(
    (required) => backendItems.find((item) => item.id === required.id) ?? required,
  );
}
