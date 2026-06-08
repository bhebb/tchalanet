import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe } from '@tch/page-model';

export type PublicRuleGameId = 'borlette' | 'mariage' | 'lotto3' | 'lotto4';

export interface PublicRuleGame {
  readonly id: PublicRuleGameId;
  readonly icon: string;
  readonly titleKey: string;
  readonly bodyKey: string;
  readonly multiplier: number;
  readonly details: readonly PublicRuleDetail[];
}

export interface PublicRuleDetail {
  readonly labelKey: string;
  readonly valueKey: string;
}

export interface PublicTchalaEntry {
  readonly id: string;
  readonly icon: string;
  readonly termKey: string;
  readonly descriptionKey: string;
  readonly numbers: readonly string[];
  readonly keywords: readonly string[];
}

export const PUBLIC_RULE_GAMES: readonly PublicRuleGame[] = [
  {
    id: 'borlette',
    icon: 'confirmation_number',
    titleKey: 'public.rules.games.borlette.title',
    bodyKey: 'public.rules.games.borlette.body',
    multiplier: 50,
    details: [
      { labelKey: 'public.rules.games.borlette.detail1_label', valueKey: 'public.rules.games.borlette.detail1_value' },
      { labelKey: 'public.rules.games.borlette.detail2_label', valueKey: 'public.rules.games.borlette.detail2_value' },
      { labelKey: 'public.rules.games.borlette.detail3_label', valueKey: 'public.rules.games.borlette.detail3_value' },
    ],
  },
  {
    id: 'mariage',
    icon: 'join_inner',
    titleKey: 'public.rules.games.mariage.title',
    bodyKey: 'public.rules.games.mariage.body',
    multiplier: 1000,
    details: [
      { labelKey: 'public.rules.games.mariage.detail1_label', valueKey: 'public.rules.games.mariage.detail1_value' },
      { labelKey: 'public.rules.games.mariage.detail2_label', valueKey: 'public.rules.games.mariage.detail2_value' },
    ],
  },
  {
    id: 'lotto3',
    icon: 'filter_3',
    titleKey: 'public.rules.games.lotto3.title',
    bodyKey: 'public.rules.games.lotto3.body',
    multiplier: 500,
    details: [{ labelKey: 'public.rules.games.lotto3.detail1_label', valueKey: 'public.rules.games.lotto3.detail1_value' }],
  },
  {
    id: 'lotto4',
    icon: 'filter_5',
    titleKey: 'public.rules.games.lotto4.title',
    bodyKey: 'public.rules.games.lotto4.body',
    multiplier: 5000,
    details: [{ labelKey: 'public.rules.games.lotto4.detail1_label', valueKey: 'public.rules.games.lotto4.detail1_value' }],
  },
];

export const PUBLIC_TCHALA_ENTRIES: readonly PublicTchalaEntry[] = [
  {
    id: 'water',
    icon: 'water_drop',
    termKey: 'public.rules.tchala.entries.water.term',
    descriptionKey: 'public.rules.tchala.entries.water.description',
    numbers: ['45', '01'],
    keywords: ['eau', 'water', 'dlo'],
  },
  {
    id: 'dog',
    icon: 'pets',
    termKey: 'public.rules.tchala.entries.dog.term',
    descriptionKey: 'public.rules.tchala.entries.dog.description',
    numbers: ['12', '98'],
    keywords: ['chien', 'dog', 'chen'],
  },
  {
    id: 'money',
    icon: 'account_balance_wallet',
    termKey: 'public.rules.tchala.entries.money.term',
    descriptionKey: 'public.rules.tchala.entries.money.description',
    numbers: ['33', '67'],
    keywords: ['argent', 'money', 'lajan'],
  },
  {
    id: 'travel',
    icon: 'airplane_ticket',
    termKey: 'public.rules.tchala.entries.travel.term',
    descriptionKey: 'public.rules.tchala.entries.travel.description',
    numbers: ['04', '82'],
    keywords: ['voyage', 'travel', 'vwayaj'],
  },
];

@Component({
  selector: 'tch-public-rules-page',
  imports: [DecimalPipe, LabelPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rules-page">
        <section class="rules-page__hero">
          <div class="rules-page__hero-copy">
            <p class="rules-page__eyebrow">{{ 'public.pages.rules.eyebrow' | tchLabel }}</p>
            <h1>{{ 'public.rules.title' | tchLabel }}</h1>
            <p class="rules-page__lead">{{ 'public.rules.subtitle' | tchLabel }}</p>
            <div class="rules-page__actions">
              <a class="rules-page__primary-link" routerLink="/public/check-ticket">
                <span class="material-symbols-outlined" aria-hidden="true">fact_check</span>
                {{ 'public.rules.verify_link' | tchLabel }}
              </a>
              <a class="rules-page__secondary-link" href="#tchala">
                <span class="material-symbols-outlined" aria-hidden="true">auto_stories</span>
                {{ 'public.rules.tchala.jump_link' | tchLabel }}
              </a>
            </div>
          </div>
          <figure class="rules-page__media">
            <img src="/assets/public/rules-tchala-preview.png" [alt]="'public.rules.hero_image_alt' | tchLabel" />
            <figcaption>
              <span class="material-symbols-outlined" aria-hidden="true">info</span>
              {{ 'public.rules.hero_caption' | tchLabel }}
            </figcaption>
          </figure>
        </section>

        <section class="rules-page__layout" aria-labelledby="rules-games-title">
          <div class="rules-page__rules">
            <div class="rules-page__section-heading">
              <p class="rules-page__eyebrow">{{ 'public.rules.games.eyebrow' | tchLabel }}</p>
              <h2 id="rules-games-title">{{ 'public.rules.games.title' | tchLabel }}</h2>
            </div>

            <div class="rules-page__game-grid">
              @for (game of games; track game.id) {
                <article class="rules-page__game">
                  <div class="rules-page__game-icon">
                    <span class="material-symbols-outlined" aria-hidden="true">{{ game.icon }}</span>
                  </div>
                  <h3>{{ game.titleKey | tchLabel }}</h3>
                  <p>{{ game.bodyKey | tchLabel }}</p>
                  <dl>
                    @for (detail of game.details; track detail.labelKey) {
                      <div>
                        <dt>{{ detail.labelKey | tchLabel }}</dt>
                        <dd>{{ detail.valueKey | tchLabel }}</dd>
                      </div>
                    }
                  </dl>
                </article>
              }
            </div>
          </div>

          <aside class="rules-page__simulation" [attr.aria-label]="'public.rules.simulation_title' | tchLabel">
            <span class="rules-page__badge">{{ 'public.rules.badge' | tchLabel }}</span>
            <h2>{{ 'public.rules.simulation_title' | tchLabel }}</h2>

            <div class="rules-page__game-select" role="group" [attr.aria-label]="'public.rules.calc_game_aria' | tchLabel">
              @for (game of games; track game.id) {
                <button
                  type="button"
                  class="rules-page__game-pill"
                  [class.rules-page__game-pill--active]="selectedGame() === game.id"
                  [attr.aria-pressed]="selectedGame() === game.id"
                  (click)="selectGame(game.id)"
                >
                  <span class="material-symbols-outlined" aria-hidden="true">{{ game.icon }}</span>
                  {{ game.titleKey | tchLabel }}
                  <span class="rules-page__multiplier">×{{ game.multiplier }}</span>
                </button>
              }
            </div>

            <label class="rules-page__amount-label">
              <span>{{ 'public.rules.calc_amount_label' | tchLabel }}</span>
              <div class="rules-page__amount-wrap">
                <span class="rules-page__currency" aria-hidden="true">HTG</span>
                <input
                  type="number"
                  min="1"
                  max="999999"
                  step="10"
                  [value]="betAmount()"
                  (input)="updateBetAmount($event)"
                />
              </div>
            </label>

            <div
              class="rules-page__fich"
              aria-live="polite"
              [attr.aria-label]="('public.rules.calc_gain_label' | tchLabel) + ': ' + (potentialGain() | number:'1.0-0') + ' HTG'"
            >
              <div class="rules-page__fich-punch rules-page__fich-punch--left" aria-hidden="true"></div>
              <div class="rules-page__fich-punch rules-page__fich-punch--right" aria-hidden="true"></div>
              <span class="rules-page__fich-eyebrow">{{ 'public.rules.calc_gain_label' | tchLabel }}</span>
              <span class="rules-page__fich-amount" aria-hidden="true">
                {{ potentialGain() | number:'1.0-0' }}<small> HTG</small>
              </span>
              <span class="rules-page__fich-note">{{ 'public.rules.calc_note' | tchLabel }}</span>
            </div>

            <p class="rules-page__disclaimer">{{ 'public.rules.disclaimer' | tchLabel }}</p>
          </aside>
        </section>

        <section class="rules-page__tchala" id="tchala" aria-labelledby="rules-tchala-title">
          <div class="rules-page__section-heading">
            <p class="rules-page__eyebrow">{{ 'public.rules.tchala.eyebrow' | tchLabel }}</p>
            <h2 id="rules-tchala-title">{{ 'public.rules.tchala.title' | tchLabel }}</h2>
            <p>{{ 'public.rules.tchala.subtitle' | tchLabel }}</p>
          </div>

          <label class="rules-page__search">
            <span class="material-symbols-outlined" aria-hidden="true">search</span>
            <span class="rules-page__search-label">{{ 'public.rules.tchala.search_label' | tchLabel }}</span>
            <input
              type="search"
              [placeholder]="'public.rules.tchala.search_placeholder' | tchLabel"
              [value]="query()"
              (input)="updateQuery($event)"
            />
          </label>

          <div class="rules-page__tchala-grid">
            @for (entry of filteredEntries(); track entry.id) {
              <article class="rules-page__dream">
                <span class="material-symbols-outlined rules-page__dream-icon" aria-hidden="true">{{ entry.icon }}</span>
                <div>
                  <h3>{{ entry.termKey | tchLabel }}</h3>
                  <p>{{ entry.descriptionKey | tchLabel }}</p>
                </div>
                <div class="rules-page__numbers" [attr.aria-label]="'public.rules.tchala.numbers_label' | tchLabel">
                  @for (number of entry.numbers; track number) {
                    <span>{{ number }}</span>
                  }
                </div>
              </article>
            } @empty {
              <p class="rules-page__empty">{{ 'public.rules.tchala.empty' | tchLabel }}</p>
            }
          </div>
        </section>
    </div>
  `,
  styles: [
    `
      .rules-page {
        display: grid;
        gap: clamp(2rem, 6vw, 4rem);
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1180px);
        margin: 0 auto;
        padding: clamp(1.5rem, 5vw, 3rem) 0 5rem;
      }

      .rules-page__hero,
      .rules-page__layout,
      .rules-page__tchala {
        display: grid;
        gap: var(--tch-page-gutter, 16px);
      }

      .rules-page__hero {
        align-items: center;
      }

      .rules-page__hero-copy,
      .rules-page__section-heading,
      .rules-page__simulation,
      .rules-page__game,
      .rules-page__dream {
        display: grid;
        gap: 0.75rem;
      }

      .rules-page__eyebrow,
      .rules-page h1,
      .rules-page h2,
      .rules-page h3,
      .rules-page p,
      .rules-page figure,
      .rules-page dl,
      .rules-page dt,
      .rules-page dd {
        margin: 0;
      }

      .rules-page__eyebrow {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
      }

      .rules-page h1 {
        max-width: 13ch;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
      }

      .rules-page h2 {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
      }

      .rules-page h3 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-title-md, 1.125rem);
        line-height: var(--tch-line-height-title-md, 1.5rem);
      }

      .rules-page__lead,
      .rules-page__section-heading p,
      .rules-page__game p,
      .rules-page__dream p,
      .rules-page__disclaimer,
      .rules-page__simulation-note,
      .rules-page__empty {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .rules-page__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
        padding-top: 0.5rem;
      }

      .rules-page__primary-link,
      .rules-page__secondary-link {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        min-height: var(--tch-touch-target, 48px);
        border-radius: var(--tch-radius-control, 8px);
        padding: 0 1rem;
        font-weight: 800;
        text-decoration: none;
      }

      .rules-page__primary-link {
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
      }

      .rules-page__secondary-link {
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .rules-page__media {
        overflow: hidden;
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .rules-page__media img {
        display: block;
        width: 100%;
        aspect-ratio: 16 / 10;
        object-fit: cover;
      }

      .rules-page__media figcaption {
        display: flex;
        gap: 0.5rem;
        align-items: center;
        padding: 0.75rem 1rem;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
      }

      .rules-page__game-grid,
      .rules-page__tchala-grid {
        display: grid;
        gap: var(--tch-page-gutter, 16px);
      }

      .rules-page__game,
      .rules-page__simulation,
      .rules-page__dream {
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
      }

      .rules-page__game {
        padding: 1rem;
      }

      .rules-page__game-icon {
        display: inline-grid;
        place-items: center;
        width: 3rem;
        height: 3rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
      }

      .rules-page__game dl {
        display: grid;
        gap: 0.5rem;
        padding-top: 0.5rem;
      }

      .rules-page__game dl div {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
        padding-top: 0.5rem;
        border-top: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .rules-page__game dt {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
      }

      .rules-page__game dd {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-family: var(--tch-font-family-mono, monospace);
        font-weight: 800;
        text-align: right;
      }

      .rules-page__simulation {
        align-content: start;
        padding: clamp(1rem, 4vw, 1.5rem);
        background: var(--tch-color-surface-tonal, var(--mat-sys-surface-container));
      }

      .rules-page__badge {
        justify-self: start;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.625rem;
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }

      .rules-page__disclaimer {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: var(--tch-line-height-label-sm, 1rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .rules-page__game-select {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 0.5rem;
      }

      .rules-page__game-pill {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.25rem;
        padding: 0.625rem 0.5rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        cursor: pointer;
        font: inherit;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        line-height: 1.2;
        text-align: center;
        transition: background 0.15s, border-color 0.15s, color 0.15s;
      }

      .rules-page__game-pill--active {
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
        background: var(--tch-color-primary-fixed, var(--mat-sys-primary-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .rules-page__multiplier {
        font-family: var(--tch-font-family-mono, monospace);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        color: var(--tch-color-accent, var(--mat-sys-tertiary));
      }

      .rules-page__game-pill--active .rules-page__multiplier {
        color: inherit;
      }

      .rules-page__amount-label {
        display: grid;
        gap: 0.375rem;
      }

      .rules-page__amount-label > span {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .rules-page__amount-wrap {
        display: flex;
        align-items: stretch;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        overflow: hidden;
      }

      .rules-page__currency {
        display: flex;
        align-items: center;
        padding: 0 0.75rem;
        border-right: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
      }

      .rules-page__amount-wrap input {
        flex: 1;
        min-height: var(--tch-touch-target, 48px);
        border: none;
        background: transparent;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        padding: 0 1rem;
        font: inherit;
        font-weight: 700;
      }

      .rules-page__amount-wrap input:focus {
        outline: none;
      }

      .rules-page__fich {
        position: relative;
        overflow: hidden;
        display: grid;
        gap: 0.375rem;
        padding: 1.25rem 2rem;
        border: 2px dashed var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background-image: radial-gradient(circle, var(--tch-color-outline-variant, rgba(0,0,0,.12)) 1px, transparent 1px);
        background-size: 18px 18px;
        background-color: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        text-align: center;
      }

      .rules-page__fich-punch {
        position: absolute;
        top: 50%;
        width: 1.5rem;
        height: 1.5rem;
        border-radius: 50%;
        border: 2px dashed var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface-tonal, var(--mat-sys-surface-container));
        transform: translateY(-50%);
      }

      .rules-page__fich-punch--left { left: -0.75rem; }
      .rules-page__fich-punch--right { right: -0.75rem; }

      .rules-page__fich-eyebrow {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .rules-page__fich-amount {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: clamp(1.5rem, 4vw, 2rem);
        font-weight: 800;
        animation: rules-fich-pulse 2.5s ease-in-out infinite;
      }

      .rules-page__fich-amount small {
        font-size: 0.75em;
        font-weight: 700;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .rules-page__fich-note {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      @keyframes rules-fich-pulse {
        0%, 100% { opacity: 1; }
        50% { opacity: 0.65; }
      }

      .rules-page__search {
        position: relative;
        display: grid;
        align-items: center;
      }

      .rules-page__search .material-symbols-outlined {
        position: absolute;
        left: 1rem;
        color: var(--tch-color-outline, var(--mat-sys-outline));
      }

      .rules-page__search-label {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip: rect(0 0 0 0);
      }

      .rules-page__search input {
        min-height: var(--tch-touch-target, 48px);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        padding: 0 1rem 0 3rem;
        font: inherit;
      }

      .rules-page__dream {
        grid-template-columns: auto 1fr;
        align-items: start;
        padding: 1rem;
      }

      .rules-page__dream-icon {
        display: inline-grid;
        place-items: center;
        width: 2.5rem;
        height: 2.5rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-primary-fixed, var(--mat-sys-primary-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .rules-page__numbers {
        grid-column: 1 / -1;
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
      }

      .rules-page__numbers span {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 2.75rem;
        min-height: 2.5rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        font-family: var(--tch-font-family-mono, monospace);
        font-weight: 800;
      }

      .rules-page__empty {
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
      }

      @media (min-width: 760px) {
        .rules-page h1 {
          font-size: var(--tch-font-size-display-lg, 2.5rem);
          line-height: var(--tch-line-height-display-lg, 3rem);
        }

        .rules-page__hero {
          grid-template-columns: minmax(0, 1fr) minmax(18rem, 0.8fr);
        }

        .rules-page__layout {
          grid-template-columns: minmax(0, 1fr) minmax(18rem, 0.45fr);
          align-items: start;
        }

        .rules-page__game-grid,
        .rules-page__tchala-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .rules-page__simulation {
          position: sticky;
          top: calc(var(--tch-touch-target, 48px) + 1rem);
        }
      }
    `,
  ],
})
export class PublicRulesPage {
  readonly games = PUBLIC_RULE_GAMES;
  readonly query = signal('');
  readonly selectedGame = signal<PublicRuleGameId>('borlette');
  readonly betAmount = signal(100);

  readonly filteredEntries = computed(() => filterTchalaEntries(PUBLIC_TCHALA_ENTRIES, this.query()));

  readonly potentialGain = computed(() => {
    const game = this.games.find(g => g.id === this.selectedGame());
    return this.betAmount() * (game?.multiplier ?? 50);
  });

  updateQuery(event: Event): void {
    const target = event.target;
    this.query.set(target instanceof HTMLInputElement ? target.value : '');
  }

  selectGame(id: PublicRuleGameId): void {
    this.selectedGame.set(id);
  }

  updateBetAmount(event: Event): void {
    const value = parseInt((event.target as HTMLInputElement).value, 10);
    if (!isNaN(value) && value > 0) {
      this.betAmount.set(value);
    }
  }
}

export function filterTchalaEntries(entries: readonly PublicTchalaEntry[], query: string): readonly PublicTchalaEntry[] {
  const normalized = normalizeQuery(query);
  if (!normalized) {
    return entries;
  }

  return entries.filter(entry => entry.keywords.some(keyword => normalizeQuery(keyword).includes(normalized)));
}

function normalizeQuery(value: string): string {
  return value
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .trim()
    .toLowerCase();
}
