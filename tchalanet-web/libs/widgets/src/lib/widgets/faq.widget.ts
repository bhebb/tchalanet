import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { isRecord, stringProp, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface FaqItem {
  id: string;
  qKey: string;
  aKey: string;
}

@Component({
  selector: 'tch-faq-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="faq">
      <h2 class="faq__title">{{ titleKey() | tchLabel }}</h2>

      <dl class="faq__list">
        @for (item of items(); track item.id) {
          <div class="faq__item" [class.faq__item--open]="isOpen(item.id)">
            <dt>
              <button
                class="faq__question"
                type="button"
                [attr.aria-expanded]="isOpen(item.id)"
                (click)="toggle(item.id)"
              >
                <span>{{ item.qKey | tchLabel }}</span>
                <span class="faq__chevron material-symbols-outlined" aria-hidden="true">expand_more</span>
              </button>
            </dt>
            @if (isOpen(item.id)) {
              <dd class="faq__answer">{{ item.aKey | tchLabel }}</dd>
            }
          </div>
        }
      </dl>
    </section>
  `,
  styles: [
    `
      @use 'mixins' as tch;
      @use 'breakpoints' as bp;

      .faq {
        padding: 3rem 1.25rem;

        @include bp.up(medium) {
          padding: 4rem 2rem;
        }

        @include bp.up(expanded) {
          padding: 5rem clamp(2rem, 6vw, 5rem);
          max-width: 860px;
          margin: 0 auto;
        }
      }

      .faq__title {
        margin: 0 0 2rem;
        text-align: center;
        font-size: clamp(1.5rem, 3vw, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .faq__list {
        display: grid;
        gap: 0;
        border-top: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .faq__item {
        border-bottom: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }

      .faq__question {
        all: unset;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        width: 100%;
        box-sizing: border-box;
        padding: 1.375rem 0;
        font-size: 1rem;
        font-weight: 600;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        @include tch.focus-visible;
      }

      .faq__chevron {
        font-size: 1.375rem;
        flex-shrink: 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        transition: transform 0.2s ease;
      }

      .faq__item--open .faq__chevron {
        transform: rotate(180deg);
      }

      .faq__answer {
        margin: 0;
        padding: 0 0 1.375rem;
        font-size: 0.9375rem;
        line-height: 1.75;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        max-width: 72ch;
      }
    `,
  ],
})
export class FaqWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  private readonly openId = signal<string | null>(null);

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');

  readonly items = computed<FaqItem[]>(() => {
    const raw = this.config().props?.['items'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((i) => ({
      id: String(i['id'] ?? ''),
      qKey: String(i['qKey'] ?? ''),
      aKey: String(i['aKey'] ?? ''),
    }));
  });

  isOpen(id: string): boolean {
    return this.openId() === id;
  }

  toggle(id: string): void {
    this.openId.update((current) => (current === id ? null : id));
  }
}
