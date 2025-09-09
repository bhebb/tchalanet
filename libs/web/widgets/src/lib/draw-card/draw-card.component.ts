// src/app/widgets/draw-card/draw-card.component.ts
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, input, signal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

export interface DrawResult {
  lotteryId: string;
  lotteryName: string;
  drawTime: string;     // ISO
  numbers: number[];
  bonus?: number[];
}
export interface NextDrawInfo {
  lotteryId: string;
  lotteryName: string;
  nextDrawTime: string; // ISO future
}

@Component({
  selector: 'tchl-draw-card',
  standalone: true,
  imports: [MatCardModule, MatIconModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    :host { display:block; }
    mat-card { border-color: color-mix(in srgb, #000 12%, transparent); }
    .wrap { display:grid; gap:1rem; }
    .row { display:grid; gap:.25rem; }
    .meta { display:flex; gap:.5rem; align-items:baseline; flex-wrap:wrap; }
    .balls { display:flex; flex-wrap:wrap; gap:.35rem; }
    .ball { min-width:2rem; height:2rem; border-radius:999px; display:grid; place-items:center;
      background: var(--color-primary,#1e4dd8); color: var(--color-on-primary,#fff); font-weight:700; }
    .ball.bonus { background: var(--color-tertiary,#ff3b3b); }
    .cd { display:flex; align-items:center; gap:.5rem; padding:.5rem .75rem; border-radius:.75rem;
      background: color-mix(in srgb, var(--color-primary,#1e4dd8) 10%, transparent); }
    @media (min-width: 640px){ .row{grid-template-columns:1fr auto; align-items:center} }
  `],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>
          <mat-icon fontIcon="casino" aria-hidden="true"></mat-icon>
          <span class="ml-2">Derniers résultats</span>
        </mat-card-title>
        <mat-card-subtitle>
          {{ selectedName() }}
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <div class="wrap">
          @if ((results() ?? []).length === 0) {
            <div aria-busy="true">Chargement…</div>
          } @else {
            @for (r of results(); track r.lotteryId + '-' + r.drawTime) {
              <div class="row">
                <div class="meta">
                  <strong>{{ r.lotteryName }}</strong>
                  <span>{{ r.drawTime | date:'medium' }}</span>
                </div>
                <div class="balls">
                  @for (n of r.numbers; track $index) {
                    <span class="ball">{{ n }}</span>
                  }
                  @for (b of (r.bonus ?? []); track $index) {
                    <span class="ball bonus">{{ b }}</span>
                  }
                </div>
              </div>
            }
          }

          @if (countdown() !== null) {
            <div class="cd">
              <mat-icon fontIcon="schedule" aria-hidden="true"></mat-icon>
              <span>Prochain tirage dans <strong class="tabnums">{{ countdown() }}</strong></span>
            </div>
          }
        </div>
      </mat-card-content>
    </mat-card>
  `
})
export class DrawCardComponent implements OnInit, OnDestroy {
  results = input.required<DrawResult[]>();
  next    = input.required<NextDrawInfo | null>();
  moreHref = input('/results');

  private timerId: any;
  countdown = signal<string | null>(null);

  selectedName = computed(() => this.next()?.lotteryName ?? this.results()?.[0]?.lotteryName ?? '');

  ngOnInit() {
    this.timerId = setInterval(() => {
      const n = this.next();
      if (!n) { this.countdown.set(null); return; }
      const diff = Math.max(0, new Date(n.nextDrawTime).getTime() - Date.now());
      const hh = Math.floor(diff / 3_600_000);
      const mm = Math.floor((diff % 3_600_000) / 60_000);
      const ss = Math.floor((diff % 60_000) / 1_000);
      this.countdown.set([hh, mm, ss].map(v => String(v).padStart(2, '0')).join(':'));
    }, 1000);
  }
  ngOnDestroy() { clearInterval(this.timerId); }
}
