// src/app/widgets/draw-switcher/draw-switcher.widget.ts
import { ChangeDetectionStrategy, Component, computed, Input, OnInit, signal } from '@angular/core';
import { DrawCardComponent, DrawResult, NextDrawInfo } from '../draw-card/draw-card.component';
import { DrawSwitcherData, DrawSwitcherProps } from '@tchl/types';
import { LotteryCarouselComponent } from '../lottery-carousel/lottery-carousel.component';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'tchl-draw-switcher',
  standalone: true,
  imports: [LotteryCarouselComponent, DrawCardComponent, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [
    `
      :host {
        display: block;
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
      
      .stack {
        display: grid;
        gap: 0.75rem;
      }
    `,
  ],
  template: `
    <section class="block">
      <h2 class="block-title h2">
        <mat-icon class="material-symbols-outlined" aria-hidden="true">casino</mat-icon>
        <span>{{ properties.title || 'Derniers résultats' }}</span>
      </h2>


      @if (ready()) {
      <div class="stack">
        <tchl-lottery-carousel
          [lotteries]="data().lotteries"
          [selectedId]="selectedId()"
          [showIcons]="properties.config?.ui?.showIcons ?? true"
          (selectionChange)="selectedId.set($event)"
        >
        </tchl-lottery-carousel>

        <tchl-draw-card
          [results]="selectedResults()"
          [next]="selectedNext()"
          [moreHref]="'/results?lottery=' + (selectedId() || '')"
        >
        </tchl-draw-card>
      </div>
      } @else {
      <div aria-busy="true">Chargement…</div>
      }
    </section>
  `,
})
export class DrawSwitcherWidget implements OnInit {
  @Input({ required: true }) properties!: DrawSwitcherProps;

  selectedId = signal<string | null>(null);
  data = computed<DrawSwitcherData>(() => this.properties.data as DrawSwitcherData);
  ready = computed(
    () =>
      !!this.properties?.config &&
      !!this.properties?.data &&
      this.properties!.data!.lotteries?.length > 0,
  );

  ngOnInit() {
    if (!this.ready()) return;
    const first = this.data().lotteries?.[0]?.id ?? null;
    this.selectedId.set(this.properties.config?.initialLotteryId ?? first);
  }

  selectedResults = computed<DrawResult[]>(() => {
    const id = this.selectedId();
    if (!id) return [];
    return this.data().results?.[id] ?? [];
  });

  selectedNext = computed<NextDrawInfo | null>(() => {
    const id = this.selectedId();
    if (!id) return null;
    return this.data().next?.[id] ?? null;
  });
}
