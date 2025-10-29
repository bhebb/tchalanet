import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Meta, MetaDefinition, Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

@Component({
  standalone: true,
  selector: 'tch-public-stub-page',
  imports: [CommonModule, TranslateModule],
  template: `
    <section class="h-container page">
      <h1>{{ t(titleKey()) }}</h1>
      <p class="muted">{{ t(descKey()) }}</p>
      <div class="placeholder">
        <p>{{ t('seo.placeholder_coming_soon') }}</p>
      </div>
    </section>
  `,
  styles: [
    `
      .page {
        padding-block: clamp(20px, 4vw, 40px);
      }
      .muted {
        opacity: 0.8;
        margin-bottom: 1rem;
      }
      .placeholder {
        padding: 1rem;
        border: 1px dashed rgba(0, 0, 0, 0.2);
        border-radius: 8px;
      }
      :root[data-theme='dark'] .placeholder {
        border-color: rgba(255, 255, 255, 0.25);
      }
      .h-container {
        max-width: var(--tch-page-max, 1120px);
        margin: 0 auto;
        padding-inline: var(--tch-page-gutter, 24px);
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicStubPageComponent {
  private route = inject(ActivatedRoute);
  private titleSvc = inject(Title);
  private meta = inject(Meta);
  private i18n = inject(TranslateService);

  readonly titleKey = signal(this.route.snapshot.data['titleKey'] || 'seo.default_title');
  readonly descKey = signal(this.route.snapshot.data['descKey'] || 'seo.default_desc');
  readonly noIndex = signal(!!this.route.snapshot.data['robotsNoIndex']);

  // traduction util
  t = (k: string) => this.i18n.instant(k);

  // SEO effects
  readonly _seo = computed(() => {
    const title = this.t(this.titleKey());
    const desc = this.t(this.descKey());
    this.titleSvc.setTitle(title);
    const tags = [
      { name: 'description', content: desc },
      // robots
      this.noIndex()
        ? { name: 'robots', content: 'noindex, nofollow' }
        : { name: 'robots', content: 'index, follow' },
      // Open Graph minimal
      { property: 'og:title', content: title },
      { property: 'og:description', content: desc },
    ];
    tags.forEach(t => this.meta.updateTag(t as MetaDefinition));
    return true;
  });
}
