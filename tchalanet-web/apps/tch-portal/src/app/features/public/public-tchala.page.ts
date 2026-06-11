import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';

import { TchPage } from '@tch/api';
import { PublicTchalaService, TchalaEntry, TchalaSuggestionRequest } from './public-tchala.service';

// ── Display model ─────────────────────────────────────────────────────────────

interface TchalaDisplayEntry {
  readonly id: string;
  readonly icon: string;
  readonly term: string;
  readonly description: string;
  readonly numbers: readonly string[];
}

function apiEntryToDisplay(e: TchalaEntry): TchalaDisplayEntry {
  return {
    id: e.id,
    icon: 'auto_stories',
    term: e.dream,
    description: e.source === 'IMPORT' ? '' : (e.note ?? ''),
    numbers: e.numbers.map(n => String(n).padStart(2, '0')),
  };
}

const PAGE_SIZE = 24;

type FormState = 'idle' | 'submitting' | 'success' | 'error' | 'limit_exceeded';

@Component({
  selector: 'tch-public-tchala-page',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tchala-page">
      <section class="tchala-page__hero">
        <p class="tchala-page__eyebrow">{{ 'public.tchala.eyebrow' | translate }}</p>
        <h1>{{ 'public.tchala.title' | translate }}</h1>
        <p class="tchala-page__lead">{{ 'public.tchala.subtitle' | translate }}</p>
      </section>

      <div class="tchala-page__search-wrap">
        <label class="tchala-page__search">
          <span class="tchala-page__search-label">{{ 'public.tchala.search_label' | translate }}</span>
          <input
            type="search"
            class="tchala-page__search-input"
            [placeholder]="'public.tchala.search_placeholder' | translate"
            [value]="query()"
            (input)="onQueryInput($event)"
          />
          <span class="material-symbols-outlined tchala-page__search-icon" aria-hidden="true">auto_awesome</span>
        </label>
      </div>

      @if (resource.isLoading()) {
        <p class="tchala-page__status">{{ 'public.tchala.searching' | translate }}</p>
      } @else if (resource.error()) {
        <p class="tchala-page__status tchala-page__status--error">{{ 'public.tchala.search_error' | translate }}</p>
      } @else if (entries().length > 0) {
        <div class="tchala-page__grid" role="list">
          @for (entry of entries(); track entry.id) {
            <article class="tchala-page__card" role="listitem">
              <div class="tchala-page__card-icon">
                <span class="material-symbols-outlined" aria-hidden="true">{{ entry.icon }}</span>
              </div>
              <div class="tchala-page__card-body">
                <h2 class="tchala-page__card-term">{{ entry.term }}</h2>
                @if (entry.description) {
                  <p class="tchala-page__card-desc">{{ entry.description }}</p>
                }
              </div>
              <div class="tchala-page__numbers" [attr.aria-label]="'public.tchala.numbers_label' | translate">
                @for (num of entry.numbers; track num) {
                  <span>{{ num }}</span>
                }
              </div>
            </article>
          }
        </div>

        @if (totalPages() > 1) {
          <nav class="tchala-page__pagination" [attr.aria-label]="'public.tchala.pagination_aria' | translate">
            <button
              type="button"
              class="tchala-page__pager-btn"
              [disabled]="currentPage() === 0"
              (click)="goToPage(currentPage() - 1)"
            >
              <span class="material-symbols-outlined" aria-hidden="true">chevron_left</span>
              {{ 'public.tchala.prev_page' | translate }}
            </button>
            <span class="tchala-page__page-indicator">{{ currentPage() + 1 }} / {{ totalPages() }}</span>
            <button
              type="button"
              class="tchala-page__pager-btn"
              [disabled]="currentPage() >= totalPages() - 1"
              (click)="goToPage(currentPage() + 1)"
            >
              {{ 'public.tchala.next_page' | translate }}
              <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
            </button>
          </nav>
        }
      } @else {
        <p class="tchala-page__empty">{{ 'public.tchala.empty' | translate }}</p>
      }

      <!-- ── Suggestion section ── -->
      @if (!suggestionStatusResource.isLoading()) {
        <section class="tchala-page__suggest" aria-labelledby="suggest-title">

          @if (!suggestionOpen()) {
            <!-- Boîte pleine : barre compacte non cliquable -->
            <div class="tchala-page__suggest-bar tchala-page__suggest-bar--locked">
              <span class="material-symbols-outlined" aria-hidden="true">lock</span>
              <span>{{ 'public.tchala.suggest.closed' | translate }}</span>
            </div>

          } @else if (formState() === 'success') {
            <!-- Succès : barre compacte avec lien reset -->
            <div class="tchala-page__suggest-bar tchala-page__suggest-bar--success">
              <span class="material-symbols-outlined" aria-hidden="true">check_circle</span>
              <span>{{ 'public.tchala.suggest.success' | translate }}</span>
              <button type="button" class="tchala-page__suggest-again" (click)="resetForm()">
                {{ 'public.tchala.suggest.suggest_again' | translate }}
              </button>
            </div>

          } @else {
            <!-- Barre trigger / formulaire inline -->
            <button
              type="button"
              class="tchala-page__suggest-bar tchala-page__suggest-bar--toggle"
              [class.tchala-page__suggest-bar--open]="formExpanded()"
              [attr.aria-expanded]="formExpanded()"
              aria-controls="suggest-form-body"
              (click)="formExpanded.set(!formExpanded())"
            >
              <span class="material-symbols-outlined" aria-hidden="true">lightbulb</span>
              <span id="suggest-title" class="tchala-page__suggest-bar-label">
                {{ 'public.tchala.suggest.lead_short' | translate }}
              </span>
              <span class="tchala-page__suggest-bar-sub">{{ 'public.tchala.suggest.title' | translate }}</span>
              <span
                class="material-symbols-outlined tchala-page__suggest-chevron"
                aria-hidden="true"
              >{{ formExpanded() ? 'expand_less' : 'expand_more' }}</span>
            </button>

            @if (formExpanded()) {
              <div id="suggest-form-body" class="tchala-page__suggest-body">
                <form class="tchala-page__suggest-form" (submit)="onSubmit($event)" novalidate>
                  <div class="tchala-page__suggest-fields">
                    <div class="tchala-page__suggest-field">
                      <label class="tchala-page__suggest-label" for="suggest-dream">
                        {{ 'public.tchala.suggest.dream_label' | translate }}<span aria-hidden="true"> *</span>
                      </label>
                      <input
                        id="suggest-dream"
                        type="text"
                        class="tchala-page__suggest-input"
                        [placeholder]="'public.tchala.suggest.dream_placeholder' | translate"
                        [value]="dreamInput()"
                        (input)="dreamInput.set(getInputValue($event))"
                        required
                        maxlength="80"
                        autofocus
                      />
                    </div>

                    <div class="tchala-page__suggest-field">
                      <label class="tchala-page__suggest-label" for="suggest-numbers">
                        {{ 'public.tchala.suggest.numbers_label' | translate }}<span aria-hidden="true"> *</span>
                      </label>
                      <input
                        id="suggest-numbers"
                        type="text"
                        class="tchala-page__suggest-input"
                        [placeholder]="'public.tchala.suggest.numbers_placeholder' | translate"
                        [value]="numbersInput()"
                        (input)="numbersInput.set(getInputValue($event))"
                        required
                        maxlength="40"
                      />
                      <span class="tchala-page__suggest-hint">{{ 'public.tchala.suggest.numbers_hint' | translate }}</span>
                    </div>
                  </div>

                  <div class="tchala-page__suggest-field">
                    <label class="tchala-page__suggest-label" for="suggest-note">
                      {{ 'public.tchala.suggest.note_label' | translate }}
                    </label>
                    <textarea
                      id="suggest-note"
                      class="tchala-page__suggest-textarea"
                      [placeholder]="'public.tchala.suggest.note_placeholder' | translate"
                      [value]="noteInput()"
                      (input)="noteInput.set(getInputValue($event))"
                      rows="2"
                      maxlength="300"
                    ></textarea>
                  </div>

                  <div class="tchala-page__suggest-footer">
                    @if (formState() === 'error') {
                      <p class="tchala-page__suggest-error" role="alert">
                        {{ 'public.tchala.suggest.error' | translate }}
                      </p>
                    }
                    <button
                      type="submit"
                      class="tchala-page__suggest-submit"
                      [disabled]="formState() === 'submitting' || !canSubmit()"
                    >
                      @if (formState() === 'submitting') {
                        <span class="material-symbols-outlined tchala-page__suggest-spinner" aria-hidden="true">autorenew</span>
                      }
                      {{ 'public.tchala.suggest.submit' | translate }}
                    </button>
                  </div>
                </form>
              </div>
            }
          }

        </section>
      }

      <!-- ── Note ── -->
      <section class="tchala-page__note" aria-labelledby="tchala-note-title">
        <span class="material-symbols-outlined tchala-page__note-icon" aria-hidden="true">info</span>
        <div>
          <h2 id="tchala-note-title">{{ 'public.tchala.note_title' | translate }}</h2>
          <p>{{ 'public.tchala.note_body' | translate }}</p>
        </div>
      </section>
    </div>
  `,
  styles: [
    `
      .tchala-page {
        display: grid;
        gap: clamp(1.5rem, 4vw, 2.5rem);
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 960px);
        margin: 0 auto;
        padding: clamp(1.5rem, 5vw, 3rem) 0 5rem;
      }

      .tchala-page__hero,
      .tchala-page__note {
        display: grid;
        gap: 0.75rem;
      }

      .tchala-page h1,
      .tchala-page h2,
      .tchala-page p {
        margin: 0;
      }

      .tchala-page__eyebrow {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .tchala-page h1 {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
      }

      .tchala-page__lead {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        max-width: 52ch;
      }

      /* ── Search ── */

      .tchala-page__search {
        position: relative;
        display: grid;
        align-items: center;
      }

      .tchala-page__search-label {
        position: absolute;
        width: 1px; height: 1px;
        overflow: hidden;
        clip: rect(0 0 0 0);
      }

      .tchala-page__search-input {
        min-height: var(--tch-touch-target, 48px);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        padding: 0 3rem 0 1rem;
        font: inherit;
        width: 100%;
      }

      .tchala-page__search-icon {
        position: absolute;
        right: 1rem;
        color: var(--tch-color-accent, var(--mat-sys-tertiary));
        pointer-events: none;
      }

      /* ── Status / empty ── */

      .tchala-page__status {
        padding: 1rem;
        text-align: center;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-style: italic;
        font-size: var(--tch-font-size-body-sm, 0.875rem);
      }

      .tchala-page__status--error {
        color: var(--tch-color-error, var(--mat-sys-error));
        font-style: normal;
      }

      .tchala-page__empty {
        padding: 2rem 1rem;
        text-align: center;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      /* ── Grid ── */

      .tchala-page__grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: var(--tch-page-gutter, 16px);
      }

      .tchala-page__card {
        display: grid;
        gap: 0.5rem;
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-bottom: 3px solid var(--tch-color-accent, var(--mat-sys-tertiary));
      }

      .tchala-page__card-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2.5rem; height: 2.5rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-primary-fixed, var(--mat-sys-primary-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .tchala-page__card-term {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 800;
        line-height: 1.25;
      }

      .tchala-page__card-desc {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: 1.4;
      }

      .tchala-page__numbers {
        display: flex;
        flex-wrap: wrap;
        gap: 0.375rem;
      }

      .tchala-page__numbers span {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 2.25rem; min-height: 2rem;
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        font-family: var(--tch-font-family-mono, monospace);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }

      /* ── Pagination ── */

      .tchala-page__pagination {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 1rem;
      }

      .tchala-page__pager-btn {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        min-height: var(--tch-touch-target, 48px);
        padding: 0 1rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-pill, 9999px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        cursor: pointer;
        font: inherit;
        font-size: var(--tch-font-size-label-md, 0.875rem);
        font-weight: 600;
        transition: background 0.15s, opacity 0.15s;
      }

      .tchala-page__pager-btn:disabled {
        opacity: 0.38;
        cursor: not-allowed;
      }

      .tchala-page__pager-btn:not(:disabled):hover {
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
      }

      .tchala-page__page-indicator {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        min-width: 4rem;
        text-align: center;
      }

      /* ── Suggestion section ── */

      .tchala-page__suggest {
        border-radius: var(--tch-radius-xl, 24px);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        overflow: hidden;
      }

      /* Barre compacte partagée entre les 3 états */
      .tchala-page__suggest-bar {
        display: flex;
        align-items: center;
        gap: 0.625rem;
        width: 100%;
        padding: 0.75rem 1rem;
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font: inherit;
        font-size: var(--tch-font-size-body-sm, 0.875rem);
        border: none;
        text-align: left;
      }

      .tchala-page__suggest-bar--toggle {
        cursor: pointer;
        transition: background 0.15s;
      }

      .tchala-page__suggest-bar--toggle:hover,
      .tchala-page__suggest-bar--open {
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
      }

      .tchala-page__suggest-bar--success {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        flex-wrap: wrap;
      }

      .tchala-page__suggest-bar-label {
        font-weight: 700;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .tchala-page__suggest-bar-sub {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        display: none;
      }

      .tchala-page__suggest-chevron {
        margin-left: auto;
        flex-shrink: 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .tchala-page__suggest-again {
        background: none;
        border: none;
        padding: 0;
        font: inherit;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        cursor: pointer;
        text-decoration: underline;
        margin-left: auto;
      }

      /* Formulaire inline (sous la barre) */
      .tchala-page__suggest-body {
        padding: 1.25rem 1rem;
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        border-top: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .tchala-page__suggest-form {
        display: grid;
        gap: 1rem;
      }

      .tchala-page__suggest-fields {
        display: grid;
        gap: 1rem;
      }

      .tchala-page__suggest-field {
        display: grid;
        gap: 0.375rem;
      }

      .tchala-page__suggest-label {
        font-size: var(--tch-font-size-label-md, 0.875rem);
        font-weight: 600;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .tchala-page__suggest-input,
      .tchala-page__suggest-textarea {
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        padding: 0.625rem 1rem;
        font: inherit;
        font-size: var(--tch-font-size-body-md, 1rem);
        width: 100%;
        box-sizing: border-box;
        transition: border-color 0.15s;
      }

      .tchala-page__suggest-input {
        min-height: var(--tch-touch-target, 48px);
      }

      .tchala-page__suggest-input:focus,
      .tchala-page__suggest-textarea:focus {
        outline: 2px solid var(--tch-color-primary, var(--mat-sys-primary));
        outline-offset: 2px;
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .tchala-page__suggest-textarea {
        resize: vertical;
      }

      .tchala-page__suggest-hint {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .tchala-page__suggest-footer {
        display: flex;
        align-items: center;
        gap: 1rem;
        flex-wrap: wrap;
      }

      .tchala-page__suggest-error {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-error, var(--mat-sys-error));
        font-weight: 600;
        flex: 1 1 100%;
      }

      .tchala-page__suggest-submit {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        min-height: var(--tch-touch-target, 48px);
        padding: 0 1.5rem;
        border-radius: var(--tch-radius-pill, 9999px);
        border: none;
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        font: inherit;
        font-weight: 700;
        cursor: pointer;
        transition: opacity 0.15s;
      }

      .tchala-page__suggest-submit:disabled {
        opacity: 0.38;
        cursor: not-allowed;
      }

      @keyframes spin { to { transform: rotate(360deg); } }
      .tchala-page__suggest-spinner {
        animation: spin 1s linear infinite;
      }

      /* ── Note ── */

      .tchala-page__note {
        grid-template-columns: auto 1fr;
        align-items: flex-start;
        padding: 1rem 1.25rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container, var(--mat-sys-surface-container));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .tchala-page__note-icon {
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .tchala-page__note h2 {
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-title-md, 1rem);
        font-weight: 700;
      }

      .tchala-page__note p {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: 1.5;
        margin-top: 0.25rem;
      }

      @media (min-width: 600px) {
        .tchala-page__suggest-bar-sub {
          display: inline;
        }

        .tchala-page__suggest-fields {
          grid-template-columns: 1fr 1fr;
        }
      }

      @media (min-width: 760px) {
        .tchala-page h1 {
          font-size: var(--tch-font-size-display-lg, 2.5rem);
          line-height: var(--tch-line-height-display-lg, 3rem);
        }

        .tchala-page__grid {
          grid-template-columns: repeat(3, 1fr);
        }

        .tchala-page__suggest-body {
          padding: 1.5rem;
        }
      }
    `,
  ],
})
export class PublicTchalaPage {
  private readonly tchalaService = inject(PublicTchalaService);

  // ── Catalogue ────────────────────────────────────────────────────────────────

  readonly query = signal('');
  readonly currentPage = signal(0);

  readonly resource = rxResource({
    params: () => ({
      lang: 'ht', // catalogue Tchala en créole haïtien uniquement
      q: this.query().trim() || undefined,
      page: this.currentPage(),
      size: PAGE_SIZE,
    }),
    stream: ({ params }): import('rxjs').Observable<TchPage<TchalaEntry>> =>
      this.tchalaService.search(params.lang, params.q, params.page, params.size),
  });

  readonly entries = computed((): readonly TchalaDisplayEntry[] =>
    (this.resource.value()?.items ?? []).map(apiEntryToDisplay),
  );

  readonly totalPages = computed(() => this.resource.value()?.totalPages ?? 0);

  onQueryInput(event: Event): void {
    this.query.set(event.target instanceof HTMLInputElement ? event.target.value : '');
    this.currentPage.set(0);
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // ── Suggestions ──────────────────────────────────────────────────────────────

  readonly suggestionStatusResource = rxResource({
    params: () => ({}),
    stream: () => this.tchalaService.suggestionStatus(),
  });

  readonly suggestionOpen = computed(() => this.suggestionStatusResource.value()?.open ?? false);

  readonly formExpanded = signal(false);
  readonly dreamInput = signal('');
  readonly numbersInput = signal('');
  readonly noteInput = signal('');
  readonly formState = signal<FormState>('idle');

  readonly canSubmit = computed(
    () => this.dreamInput().trim().length >= 2 && this.numbersInput().trim().length >= 1,
  );

  getInputValue(event: Event): string {
    return event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement
      ? event.target.value
      : '';
  }

  onSubmit(event: Event): void {
    event.preventDefault();
    if (!this.canSubmit() || this.formState() === 'submitting') return;

    this.formState.set('submitting');

    const body: TchalaSuggestionRequest = {
      lang: 'ht',
      dream: this.dreamInput().trim(),
      numbers: this.numbersInput().trim(),
      note: this.noteInput().trim() || undefined,
    };

    this.tchalaService.submitSuggestion(body).subscribe({
      next: () => {
        this.formState.set('success');
        this.formExpanded.set(false);
      },
      error: () => this.formState.set('error'),
    });
  }

  resetForm(): void {
    this.dreamInput.set('');
    this.numbersInput.set('');
    this.noteInput.set('');
    this.formState.set('idle');
    this.formExpanded.set(false);
    this.suggestionStatusResource.reload();
  }
}
