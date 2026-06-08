import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Data, RouterLink } from '@angular/router';

import { LabelPipe } from '@tch/page-model';

type PublicInfoKind = 'check-ticket' | 'results' | 'rules' | 'help' | 'contact' | 'privacy' | 'terms';

@Component({
  selector: 'tch-public-info-page',
  imports: [LabelPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="info">
        <p class="info__eyebrow">{{ eyebrowKey() | tchLabel }}</p>
        <h1>{{ titleKey() | tchLabel }}</h1>
        <p class="info__lead">{{ descriptionKey() | tchLabel }}</p>

        @switch (kind()) {
          @case ('check-ticket') {
            <form class="info__card">
              <label for="public-check-code">{{ 'public.ticket.code_label' | tchLabel }}</label>
              <input
                id="public-check-code"
                name="code"
                autocomplete="off"
                [placeholder]="'public.ticket.placeholder' | tchLabel"
              />
              <button type="button">{{ 'public.ticket.cta' | tchLabel }}</button>
              <a routerLink="/public/help">{{ 'public.ticket.help' | tchLabel }}</a>
            </form>
          }
          @case ('results') {
            <div class="info__card">
              <h2>{{ 'public.results.latest_title' | tchLabel }}</h2>
              <p>{{ 'public.results.empty' | tchLabel }}</p>
            </div>
          }
          @case ('rules') {
            <div class="info__card">
              <span class="info__badge">{{ 'public.rules.badge' | tchLabel }}</span>
              <h2>{{ 'public.rules.simulation_title' | tchLabel }}</h2>
              <p>{{ 'public.rules.simulation_unavailable' | tchLabel }}</p>
              <p class="info__muted">{{ 'public.rules.disclaimer' | tchLabel }}</p>
            </div>
          }
          @case ('help') {
            <div class="info__card">
              <h2>{{ 'public.help.payment_question' | tchLabel }}</h2>
              <p>{{ 'public.help.payment_answer' | tchLabel }}</p>
            </div>
          }
          @default {
            <div class="info__card">
              <p>{{ bodyKey() | tchLabel }}</p>
            </div>
          }
        }
      </section>
  `,
  styles: [
    `
      .info {
        display: grid;
        gap: 1rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 960px);
        margin: 0 auto;
        padding: clamp(2rem, 8vw, 4rem) 0;
      }
      .info__eyebrow,
      .info h1,
      .info h2,
      .info p {
        margin: 0;
      }
      .info__eyebrow {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        text-transform: uppercase;
      }
      .info h1 {
        font-size: var(--tch-font-size-headline-lg, 2rem);
        line-height: var(--tch-line-height-headline-lg, 2.5rem);
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }
      .info__lead,
      .info__muted {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }
      .info__card {
        display: grid;
        gap: 0.75rem;
        padding: 1rem;
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }
      .info__card label {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-weight: 700;
      }
      .info__card input,
      .info__card button {
        min-height: var(--tch-touch-target, 48px);
        border-radius: var(--tch-radius-control, 8px);
      }
      .info__card input {
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface-bright, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        padding: 0 0.875rem;
        font-family: var(--tch-font-family-mono, monospace);
      }
      .info__card button {
        border: 0;
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        font-weight: 800;
      }
      .info__card a {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-weight: 800;
      }
      .info__badge {
        justify-self: start;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 0.25rem 0.625rem;
        background: var(--tch-color-accent, var(--mat-sys-tertiary));
        color: var(--tch-on-color-accent, var(--mat-sys-on-tertiary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
      }
    `,
  ],
})
export class PublicInfoPage {
  private readonly route = inject(ActivatedRoute);
  private readonly data = toSignal(this.route.data, { initialValue: this.route.snapshot.data });

  readonly kind = computed(() => readKind(this.data()));
  readonly titleKey = computed(() => `public.pages.${this.kind()}.title`);
  readonly eyebrowKey = computed(() => `public.pages.${this.kind()}.eyebrow`);
  readonly descriptionKey = computed(() => `public.pages.${this.kind()}.description`);
  readonly bodyKey = computed(() => `public.pages.${this.kind()}.body`);
}

function readKind(data: Data): PublicInfoKind {
  const value = data['kind'];
  return isPublicInfoKind(value) ? value : 'help';
}

function isPublicInfoKind(value: unknown): value is PublicInfoKind {
  return (
    value === 'check-ticket' ||
    value === 'results' ||
    value === 'rules' ||
    value === 'help' ||
    value === 'contact' ||
    value === 'privacy' ||
    value === 'terms'
  );
}
