import { ChangeDetectionStrategy, Component, Input, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { NewsBannerProps } from '@tchl/types';

@Component({
  selector: 'tchl-news-banner',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [
    `
      :host {
        display: block;
      }

      .label {
        display: none;
        font-weight: 600;
        color: var(--color-primary, #1e4dd8);
      }

      .track {
        display: flex;
        gap: 1.25rem;
        overflow: auto;
        padding-inline: 0.25rem;
        scroll-snap-type: x mandatory;
        -webkit-overflow-scrolling: touch;
        mask-image: linear-gradient(
          to right,
          transparent 0,
          black 1rem,
          black calc(100% - 1rem),
          transparent 100%
        );
        white-space: nowrap;
      }

      .item {
        scroll-snap-align: center;
        text-decoration: none;
        color: inherit;
      }

      .item:hover {
        text-decoration: underline;
      }

      /* replace styles in the component with these additions/changes */
      .block {
        display: grid;
        gap: .5rem;
        padding-inline: 1rem;
      }

      .block-title {
        display: flex;
        align-items: center;
        gap: .5rem;
        margin: .25rem 0;
      }

      .nb {
        background: var(--color-surface-variant, #eef3ff);
        border: 1px solid color-mix(in srgb, #000 10%, transparent);
        border-radius: .75rem;
      }

      .row {
        padding: .5rem 1rem;
      }

      @media (min-width: 640px) {
        .row {
          grid-template-columns: auto 1fr auto;
        }

        .label {
          display: flex;
          align-items: center;
          gap: 0.35rem;
        }
      }
    `,
  ],
  template: `
    <section class="block">
      <!-- Block title from backend -->
      <!-- before .block-title div -->
      <h2 class="block-title h2">
        <mat-icon class="material-symbols-outlined" aria-hidden="true">campaign</mat-icon>
        <span>{{ properties.title || 'Actualités' }}</span>
      </h2>


      <div
        class="nb"
        role="region"
        [attr.aria-label]="properties.labelKey || properties.title || 'Actualités'"
      >
        <div class="row">
          @if (properties.labelKey; as lk) {
            <div class="label">
              <mat-icon class="material-symbols-outlined">campaign</mat-icon>
              <span>{{ lk }}</span>
            </div>
          }
          <div
            class="track"
            [class.paused]="paused()"
            (mouseenter)="properties.pauseOnHover ? paused.set(true) : null"
            (mouseleave)="properties.pauseOnHover ? paused.set(false) : null"
          >
            @if ((properties?.data?.items?.length ?? 0) === 0) {
              <span>{{
                  properties.emptyStateKey || 'Aucune actualité disponible pour le moment.'
                }}</span>
            } @else {
              @for (n of properties.data!.items; track n.id) {
                <a class="item" [href]="n.href || '#'" target="_blank" rel="noopener">
                  <span>{{ n.title }}</span>
                  @if (n.source) {
                    <span> · {{ n.source }}</span>
                  }
                </a>
              }
            }
          </div>

          <!-- optional right-side controls area (hidden on mobile) -->
          <div aria-hidden="true"></div>
        </div>
      </div>
    </section>
  `,
})
export class NewsBannerWidget {
  @Input({ required: true }) properties!: NewsBannerProps;
  paused = signal(false);
}
