import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import { stringProp } from '../widget.contract';

interface HeroDynamic {
  readonly title_key?: string;
  readonly subtitle_key?: string;
  readonly cta_key?: string;
  readonly cta_path?: string;
}

/**
 * `HeroWidget`: strong hero with a primary action. Title/subtitle/CTA come from the widget props
 * (`*_key`) with an optional dynamic (json_file) override. Styled only via theme tokens.
 */
@Component({
  selector: 'tch-hero-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="hero">
      <h1 class="hero__title">{{ titleKey() | tchLabel }}</h1>
      @if (subtitleKey()) {
        <p class="hero__subtitle">{{ subtitleKey() | tchLabel }}</p>
      }
      @if (ctaKey()) {
        <a class="hero__cta" [attr.href]="ctaPath()">{{ ctaKey() | tchLabel }}</a>
      }
    </section>
  `,
  styles: [
    `
      .hero {
        display: grid;
        gap: 1rem;
        padding: 3rem 2rem;
        border-radius: var(--tch-radius-control, 12px);
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: var(--tch-color-foreground, var(--mat-sys-on-surface));
      }
      .hero__title {
        margin: 0;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: clamp(1.75rem, 4vw, 3rem);
      }
      .hero__subtitle {
        margin: 0;
        max-width: 60ch;
      }
      .hero__cta {
        justify-self: start;
        padding: 0.75rem 1.25rem;
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-primary-contrast, var(--mat-sys-on-primary));
        text-decoration: none;
        font-weight: 600;
      }
    `,
  ],
})
export class HeroWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  private readonly data = computed<HeroDynamic>(() => (this.dynamic() as HeroDynamic) ?? {});

  readonly titleKey = computed(
    () => this.data().title_key ?? stringProp(this.config(), 'title_key') ?? 'home.hero.title',
  );
  readonly subtitleKey = computed(
    () => this.data().subtitle_key ?? stringProp(this.config(), 'subtitle_key'),
  );
  readonly ctaKey = computed(() => this.data().cta_key ?? stringProp(this.config(), 'cta_key'));
  readonly ctaPath = computed(
    () => this.data().cta_path ?? stringProp(this.config(), 'cta_path') ?? '#',
  );
}
