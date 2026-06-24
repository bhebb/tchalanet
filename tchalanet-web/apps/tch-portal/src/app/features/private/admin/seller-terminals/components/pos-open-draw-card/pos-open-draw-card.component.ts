import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { RouterLink } from '@angular/router';

import { PosOpenDrawView } from '../../data-access/admin-seller-terminal-pos.models';

@Component({
  selector: 'tch-pos-open-draw-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatButtonModule, MatMenuModule, RouterLink],
  templateUrl: './pos-open-draw-card.component.html',
  styleUrls: ['./pos-open-draw-card.component.scss'],
})
export class PosOpenDrawCardComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  readonly draw = input<PosOpenDrawView | null>(null);
  readonly draws = input<PosOpenDrawView[]>([]);
  readonly drawSelected = output<PosOpenDrawView>();

  private readonly nowMs = signal(Date.now());

  readonly timeRemainingLabel = computed(() => {
    const closeAt = this.draw()?.cutoffAt;
    if (!closeAt) return null;
    const ms = new Date(closeAt).getTime() - this.nowMs();
    if (ms <= 0) return 'Vente fermée';
    const totalSecs = Math.floor(ms / 1000);
    const h = Math.floor(totalSecs / 3600);
    const m = Math.floor((totalSecs % 3600) / 60);
    const s = totalSecs % 60;
    if (h > 0) return `${h}h ${String(m).padStart(2, '0')}m`;
    return `${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`;
  });

  readonly isSalesOpen = computed(() => {
    const closeAt = this.draw()?.cutoffAt;
    if (!closeAt) return false;
    return new Date(closeAt).getTime() - this.nowMs() > 0;
  });

  readonly otherDraws = computed(() => {
    const current = this.draw();
    return this.draws().filter(d => d.drawId !== current?.drawId);
  });

  ngOnInit(): void {
    const id = setInterval(() => this.nowMs.set(Date.now()), 1000);
    this.destroyRef.onDestroy(() => clearInterval(id));
  }

  selectDraw(draw: PosOpenDrawView): void {
    this.drawSelected.emit(draw);
  }
}
