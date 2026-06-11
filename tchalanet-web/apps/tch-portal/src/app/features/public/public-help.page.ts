import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';



interface FaqItem {
  readonly q: string;
  readonly a: string;
  open: boolean;
}

interface HelpCategory {
  readonly icon: string;
  readonly labelKey: string;
}

const HELP_CATEGORIES: readonly HelpCategory[] = [
  { icon: 'payments',             labelKey: 'public.help.cat_payments' },
  { icon: 'account_balance_wallet', labelKey: 'public.help.cat_withdrawals' },
  { icon: 'security',             labelKey: 'public.help.cat_security' },
  { icon: 'casino',               labelKey: 'public.help.cat_games' },
];

@Component({
  selector: 'tch-public-help-page',
  imports: [TranslatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="help-page">
      <section class="help-page__hero">
        <h1>{{ 'public.help.hero_title' | translate }}</h1>
        <p>{{ 'public.help.hero_body' | translate }}</p>
        <label class="help-page__search">
          <span class="help-page__search-label">{{ 'public.help.search_label' | translate }}</span>
          <span class="material-symbols-outlined help-page__search-icon" aria-hidden="true">search</span>
          <input
            type="search"
            class="help-page__search-input"
            [placeholder]="'public.help.search_placeholder' | translate"
            [value]="query()"
            (input)="updateQuery($event)"
          />
        </label>
      </section>

      <nav class="help-page__categories" [attr.aria-label]="'public.help.cat_aria' | translate">
        @for (cat of categories; track cat.icon) {
          <button type="button" class="help-page__cat-tile">
            <span class="material-symbols-outlined" aria-hidden="true">{{ cat.icon }}</span>
            <span>{{ cat.labelKey | translate }}</span>
          </button>
        }
      </nav>

      <section class="help-page__faq" aria-labelledby="faq-title">
        <h2 id="faq-title">{{ 'public.help.faq_title' | translate }}</h2>
        @for (item of faqItems; track item.q; let i = $index) {
          <div class="help-page__faq-item">
            <button
              type="button"
              class="help-page__faq-trigger"
              [attr.aria-expanded]="item.open"
              [attr.aria-controls]="'faq-panel-' + i"
              (click)="toggleFaq(i)"
            >
              <span>{{ item.q }}</span>
              <span class="material-symbols-outlined help-page__faq-chevron" [class.help-page__faq-chevron--open]="item.open" aria-hidden="true">expand_more</span>
            </button>
            @if (item.open) {
              <div [id]="'faq-panel-' + i" class="help-page__faq-panel" role="region">
                <p>{{ item.a }}</p>
              </div>
            }
          </div>
        }
      </section>

      <section class="help-page__support" aria-labelledby="support-title">
        <h2 id="support-title" class="help-page__sr-only">{{ 'public.help.support_section_aria' | translate }}</h2>
        <div class="help-page__support-card help-page__support-card--featured">
          <div class="help-page__support-card-body">
            <h3>{{ 'public.help.support_direct_title' | translate }}</h3>
            <p>{{ 'public.help.support_direct_body' | translate }}</p>
            <a class="help-page__support-btn" routerLink="/public/contact">
              <span class="material-symbols-outlined" aria-hidden="true">support_agent</span>
              {{ 'public.help.support_direct_cta' | translate }}
            </a>
          </div>
          <span class="material-symbols-outlined help-page__support-watermark" aria-hidden="true">shield</span>
        </div>
        <div class="help-page__support-card">
          <h3>{{ 'public.help.support_ticket_title' | translate }}</h3>
          <p>{{ 'public.help.support_ticket_body' | translate }}</p>
          <a class="help-page__support-link" routerLink="/public/contact">
            {{ 'public.help.support_ticket_cta' | translate }}
            <span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
          </a>
        </div>
      </section>
    </div>
  `,
  styles: [
    `
      .help-page {
        display: grid;
        gap: clamp(1.5rem, 4vw, 2.5rem);
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 720px);
        margin: 0 auto;
        padding: clamp(1.5rem, 5vw, 3rem) 0 5rem;
      }

      .help-page h1, .help-page h2, .help-page h3, .help-page p { margin: 0; }

      .help-page__hero {
        display: grid;
        gap: 1rem;
      }

      .help-page h1 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        font-weight: 800;
        line-height: var(--tch-line-height-headline-mobile, 2rem);
      }

      .help-page__hero > p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .help-page__search {
        position: relative;
        display: grid;
        align-items: center;
      }

      .help-page__search-label {
        position: absolute;
        width: 1px; height: 1px;
        overflow: hidden; clip: rect(0 0 0 0);
      }

      .help-page__search-icon {
        position: absolute;
        left: 1rem;
        color: var(--tch-color-outline, var(--mat-sys-outline));
        pointer-events: none;
      }

      .help-page__search-input {
        min-height: var(--tch-touch-target, 48px);
        padding: 0 1rem 0 3rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font: inherit;
        width: 100%;
      }

      .help-page__categories {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 0.75rem;
        overflow-x: auto;
      }

      .help-page__cat-tile {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem 0.75rem;
        border-radius: var(--tch-radius-xl, 16px);
        border: none;
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        cursor: pointer;
        font: inherit;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        transition: background 0.15s;
      }

      .help-page__cat-tile:hover {
        background: var(--tch-color-surface-container-high, var(--mat-sys-surface-container-high));
      }

      .help-page__faq {
        display: grid;
        gap: 0.75rem;
      }

      .help-page h2 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-title-md, 1.125rem);
        font-weight: 700;
      }

      .help-page__faq-item {
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        overflow: hidden;
      }

      .help-page__faq-trigger {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        width: 100%;
        padding: 1rem;
        border: none;
        background: transparent;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        cursor: pointer;
        font: inherit;
        font-weight: 600;
        text-align: left;
      }

      .help-page__faq-chevron {
        flex-shrink: 0;
        color: var(--tch-color-outline, var(--mat-sys-outline));
        transition: transform 0.25s;
      }

      .help-page__faq-chevron--open {
        transform: rotate(180deg);
      }

      .help-page__faq-panel {
        padding: 0 1rem 1rem;
      }

      .help-page__faq-panel p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        line-height: 1.6;
      }

      .help-page__support {
        display: grid;
        gap: 1rem;
      }

      .help-page__sr-only {
        position: absolute;
        width: 1px; height: 1px;
        overflow: hidden; clip: rect(0 0 0 0);
      }

      .help-page__support-card {
        display: grid;
        gap: 0.75rem;
        padding: 1.5rem;
        border-radius: var(--tch-radius-xl, 16px);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface-tonal, var(--mat-sys-surface-container));
      }

      .help-page__support-card--featured {
        position: relative;
        overflow: hidden;
        background: var(--tch-color-primary-container, var(--mat-sys-primary-container, #2e3192));
        border-color: transparent;
        color: var(--tch-on-color-primary-container, #fff);
      }

      .help-page__support-card--featured h3,
      .help-page__support-card--featured p {
        color: #fff;
      }

      .help-page__support-card--featured p {
        opacity: 0.85;
      }

      .help-page__support-card-body {
        display: grid;
        gap: 0.75rem;
        position: relative;
        z-index: 1;
      }

      .help-page__support-watermark {
        position: absolute;
        right: -1rem;
        bottom: -1rem;
        font-size: 7rem;
        opacity: 0.1;
        color: #fff;
      }

      .help-page h3 {
        font-size: var(--tch-font-size-headline-mobile, 1.25rem);
        font-weight: 800;
      }

      .help-page__support-card p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .help-page__support-btn {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        min-height: var(--tch-touch-target, 48px);
        padding: 0 1.25rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        font: inherit;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        text-decoration: none;
        width: fit-content;
      }

      .help-page__support-link {
        display: inline-flex;
        align-items: center;
        gap: 0.375rem;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-weight: 700;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        text-decoration: none;
        border-bottom: 2px solid currentColor;
        padding-bottom: 0.125rem;
      }

      .help-page__support-link .material-symbols-outlined {
        font-size: 1rem;
      }

      @media (min-width: 760px) {
        .help-page h1 {
          font-size: var(--tch-font-size-display-lg, 2.5rem);
          line-height: var(--tch-line-height-display-lg, 3rem);
        }

        .help-page__support {
          grid-template-columns: repeat(2, 1fr);
        }
      }
    `,
  ],
})
export class PublicHelpPage {
  readonly categories = HELP_CATEGORIES;
  readonly query = signal('');

  faqItems: FaqItem[] = [
    {
      q: 'Comment encaisser mes gains ?',
      a: "Présentez votre fiche originale auprès d'un point de vente participant. Les gains sont vérifiés à partir du code public imprimé sur le reçu.",
      open: false,
    },
    {
      q: 'Quels sont les horaires des tirages ?',
      a: "Les tirages New York et Florida suivent les horaires officiels publiés. Consultez la section Résultats pour voir le statut en temps réel de chaque session.",
      open: false,
    },
    {
      q: 'Ma transaction est "En attente", pourquoi ?',
      a: "Un ticket peut être en attente durant la validation du tirage. Le statut est mis à jour dès que les résultats sont confirmés par les sources officielles.",
      open: false,
    },
    {
      q: 'Comment vérifier un ticket ?',
      a: "Rendez-vous sur la page Vérification et saisissez le code public figurant au bas de votre reçu imprimé. Vous pouvez aussi scanner le QR code.",
      open: false,
    },
  ];

  updateQuery(event: Event): void {
    this.query.set(event.target instanceof HTMLInputElement ? event.target.value : '');
  }

  toggleFaq(index: number): void {
    this.faqItems = this.faqItems.map((item, i) =>
      i === index ? { ...item, open: !item.open } : item,
    );
  }
}
