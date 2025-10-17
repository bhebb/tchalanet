import { ChangeDetectionStrategy, Component, ElementRef, ViewChild, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export interface LotteryLite { id: string; name: string; icon?: string; }

@Component({
  selector: 'tchl-lottery-carousel',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    :host { display:block; }
    .wrap { display:grid; grid-template-columns: 1fr auto; gap:.5rem; align-items:center; }
    .track {
      display:flex; gap:.5rem; overflow-x:auto; padding:.25rem .25rem;
      scroll-snap-type:x mandatory; -webkit-overflow-scrolling:touch;
      mask-image: linear-gradient(to right, transparent 0, black 1rem, black calc(100% - 1rem), transparent 100%);
    }
    .chip {
      scroll-snap-align:center; border-radius:999px; padding:.375rem .75rem;
      display:inline-flex; align-items:center; gap:.35rem; border:1px solid color-mix(in srgb, #000 14%, transparent);
      background: var(--color-surface,#fff); white-space:nowrap; font-weight:600;
    }
    .chip[aria-current="true"] {
      background: var(--color-primary,#1e4dd8); color: var(--color-on-primary,#fff);
      border-color: transparent;
    }
    .ctrl { display:none; gap:.25rem; }
    .ctrl button { border:1px solid color-mix(in srgb, #000 14%, transparent); border-radius:.5rem; padding:.25rem .5rem; }
    @media (min-width: 640px){ .ctrl{display:flex} }
  `],
  template: `
    <div class="wrap" role="tablist" aria-label="Changer de loterie">
      <div #track class="track">
        @for (l of lotteries(); track l.id) {
          <button
            type="button"
            class="chip"
            role="tab"
            [attr.aria-selected]="selectedId() === l.id"
            [attr.aria-current]="selectedId() === l.id"
            (click)="select(l.id)">
            @if (showIcons() && l.icon) { <mat-icon [fontIcon]="l.icon" aria-hidden="true"></mat-icon> }
            <span>{{ l.name }}</span>
          </button>
        }
      </div>

      <div class="ctrl" aria-hidden="true">
        <button type="button" (click)="scroll(-1)"><mat-icon fontIcon="chevron_left"></mat-icon></button>
        <button type="button" (click)="scroll(1)"><mat-icon fontIcon="chevron_right"></mat-icon></button>
      </div>
    </div>
  `
})
export class LotteryCarouselComponent {
  lotteries = input.required<LotteryLite[]>();
  selectedId = input<string | null>(null);
  showIcons = input(true);

  selectionChange = output<string>();

  @ViewChild('track', { static: true }) track!: ElementRef<HTMLElement>;

  select(id: string) { this.selectionChange.emit(id); }
  scroll(dir: -1 | 1) {
    const el = this.track.nativeElement;
    el.scrollBy({ left: dir * (el.clientWidth * 0.8), behavior: 'smooth' });
  }
}
