import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PageShell } from '../../../shared/types';
import { LabelPipe } from '../../pagemodel/label.pipe';
import { isRecord, toPublicPath } from '../../pagemodel/widget.contract';

interface PublicNavDestination {
  readonly id?: string;
  readonly label_key: string;
  readonly path: string;
}

interface PublicFooterColumn {
  readonly titleKey: string;
  readonly links: readonly PublicNavDestination[];
}

interface FooterText {
  readonly descriptionKey: string;
  readonly statusKey: string;
  readonly copyrightKey: string;
}

const PUBLIC_FOOTER_COLUMNS: readonly PublicFooterColumn[] = [
  {
    titleKey: 'public.footer.solutions.title',
    links: [
      { id: 'operators', label_key: 'public.footer.solutions.operators', path: '/public/contact' },
      { id: 'check_ticket', label_key: 'public.footer.solutions.check_ticket', path: '/public/check-ticket' },
      { id: 'pos_management', label_key: 'public.footer.solutions.pos_management', path: '/public/contact' },
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

const PUBLIC_FOOTER_TEXT: FooterText = {
  descriptionKey: 'public.footer.description',
  statusKey: 'public.footer.status.operational',
  copyrightKey: 'app.footer.copyright',
};

@Component({
  selector: 'tch-public-footer',
  imports: [RouterLink, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer class="public-footer">
      <div class="public-footer__inner">
        <section class="public-footer__brand" aria-label="Tchalanet">
          <a class="public-footer__brand-link" routerLink="/public">
            <img src="/assets/brand/tchalanet-logo-inverse.svg" alt="Tchalanet"/>
          </a>
          <p>{{ text().descriptionKey | tchLabel }}</p>
          <div class="public-footer__status">
            <span aria-hidden="true"></span>
            {{ text().statusKey | tchLabel }}
          </div>
        </section>

        <nav class="public-footer__columns" aria-label="Pied de page public">
          @for (column of columns(); track column.titleKey) {
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
        <small>{{ text().copyrightKey | tchLabel }}</small>
      </div>
    </footer>
  `,
  styles: [
    `
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
        display: inline-flex;
        align-items: center;
        gap: 0.625rem;
        color: inherit;
        text-decoration: none;
        font-size: 1.25rem;
        font-weight: 900;
      }

      .public-footer__brand-link img {
        width: 11rem;
        height: auto;
        display: block;
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

      @media (max-width: 720px) {
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
      }
    `,
  ],
})
export class PublicFooter {
  readonly shell = input<PageShell | undefined>();
  readonly columns = computed(() => publicFooterColumns(this.shell()));
  readonly text = computed(() => publicFooterText(this.shell()));
}

function publicFooterText(shell: PageShell | undefined): FooterText {
  const props = shell?.footer?.props;
  return {
    descriptionKey: readString(props, 'description_key') ?? readString(props, 'descriptionKey') ?? PUBLIC_FOOTER_TEXT.descriptionKey,
    statusKey: readString(props, 'status_key') ?? readString(props, 'statusKey') ?? PUBLIC_FOOTER_TEXT.statusKey,
    copyrightKey: readString(props, 'copyright_key') ?? readString(props, 'copyrightKey') ?? PUBLIC_FOOTER_TEXT.copyrightKey,
  };
}

function publicFooterColumns(shell: PageShell | undefined): readonly PublicFooterColumn[] {
  const footerItems = normalizeDestinations([
    ...(shell?.footer?.nav?.primary ?? []),
    ...(shell?.footer?.nav?.secondary ?? []),
  ]).map(normalizePublicNav);

  return PUBLIC_FOOTER_COLUMNS.map((column) => ({
    ...column,
    links: column.links.map((link) => footerItems.find((item) => item.id === link.id) ?? link),
  }));
}

function normalizeDestinations(items: readonly unknown[]): readonly PublicNavDestination[] {
  return items.flatMap((item) => {
    if (!isRecord(item)) {
      return [];
    }
    const path = readString(item, 'path');
    const labelKey = readString(item, 'label_key') ?? readString(item, 'labelKey');
    return path && labelKey
      ? [{ id: readString(item, 'id'), label_key: labelKey, path: toPublicPath(path) }]
      : [];
  });
}

function normalizePublicNav(item: PublicNavDestination): PublicNavDestination {
  const id = normalizePublicId(item.id);
  if (id === 'results') {
    return { ...item, id, label_key: 'public.footer.support.results', path: '/public/results' };
  }
  if (id === 'check_ticket') {
    return { ...item, id, label_key: 'public.footer.solutions.check_ticket', path: '/public/check-ticket' };
  }
  if (id === 'operators') {
    return { ...item, id, label_key: 'public.footer.solutions.operators', path: '/public/contact' };
  }
  if (id === 'pos_management') {
    return { ...item, id, label_key: 'public.footer.solutions.pos_management', path: '/public/contact' };
  }
  if (id === 'help') {
    return { ...item, id, label_key: 'public.footer.support.help', path: '/public/help' };
  }
  if (id === 'rules') {
    return { ...item, id, label_key: 'public.footer.support.rules', path: '/public/rules' };
  }
  if (id === 'status') {
    return { ...item, id, label_key: 'public.footer.support.status', path: '/public/contact' };
  }
  if (id === 'contact') {
    return { ...item, id, label_key: 'public.footer.support.contact', path: '/public/contact' };
  }
  if (id === 'privacy') {
    return { ...item, id, label_key: 'public.footer.legal.privacy', path: '/public/privacy' };
  }
  if (id === 'terms') {
    return { ...item, id, label_key: 'public.footer.legal.terms', path: '/public/terms' };
  }
  if (id === 'compliance') {
    return { ...item, id, label_key: 'public.footer.legal.compliance', path: '/public/terms' };
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
  if (id === 'games') {
    return 'rules';
  }
  if (id === 'contact_demo') {
    return 'contact';
  }
  if (id === 'responsible_gaming') {
    return 'compliance';
  }
  return id ?? '';
}

function readString(source: unknown, key: string): string | undefined {
  return isRecord(source) && typeof source[key] === 'string' ? source[key] : undefined;
}
