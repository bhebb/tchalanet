import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { GeneratedDrawGroup, GeneratedDrawView, DrawLifecycleAction } from '../../data-access/admin-generated-draws.models';
import { GeneratedDrawStatusBadgeComponent } from '../generated-draw-status-badge/generated-draw-status-badge.component';

@Component({
  selector: 'tch-generated-draws-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatButtonModule, MatIconModule, MatMenuModule, GeneratedDrawStatusBadgeComponent],
  templateUrl: './generated-draws-table.component.html',
  styleUrls: ['./generated-draws-table.component.scss'],
})
export class GeneratedDrawsTableComponent {
  readonly groups        = input.required<GeneratedDrawGroup[]>();
  readonly todayDate     = input<string>('');
  readonly totalElements = input<number>(0);
  readonly page          = input<number>(0);
  readonly pageSize      = input<number>(20);

  readonly enterResult   = output<GeneratedDrawView>();
  readonly viewResult    = output<GeneratedDrawView>();
  readonly viewDetails   = output<GeneratedDrawView>();
  readonly verifySource  = output<GeneratedDrawView>();
  readonly lockDraw      = output<GeneratedDrawView>();
  readonly unlockDraw    = output<GeneratedDrawView>();
  readonly cancelDraw    = output<GeneratedDrawView>();
  readonly archiveDraw   = output<GeneratedDrawView>();
  readonly nextPage      = output<void>();
  readonly prevPage      = output<void>();

  get hasPrev(): boolean { return this.page() > 0; }
  get hasNext(): boolean { return (this.page() + 1) * this.pageSize() < this.totalElements(); }
  get rangeStart(): number { return this.page() * this.pageSize() + 1; }
  get rangeEnd(): number {
    return Math.min((this.page() + 1) * this.pageSize(), this.totalElements());
  }

  groupDateLabel(date: string): string {
    const d = new Date(date + 'T00:00:00');
    const today = this.todayDate();
    const tomorrow = new Date(today + 'T00:00:00');
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowStr = tomorrow.toISOString().slice(0, 10);

    const label = d.toLocaleDateString('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).toUpperCase();

    if (date === today)       return `${label} (AUJOURD'HUI)`;
    if (date === tomorrowStr) return `${label} (DEMAIN)`;
    return label;
  }

  accentColor(draw: GeneratedDrawView): string {
    if (draw.resultStatus === 'CONFIRMED')    return 'ready';
    if (draw.resultStatus === 'SOURCE_ERROR') return 'error';
    if (draw.resultStatus === 'MISSING')      return 'error';
    if (draw.salesStatus === 'OPEN')          return 'open';
    return 'neutral';
  }

  scheduledTime(scheduledAt: string): string {
    return scheduledAt.slice(11, 16);
  }

  lifecycleActions(draw: GeneratedDrawView): DrawLifecycleAction[] {
    switch (draw.lifecycleStatus) {
      case 'SCHEDULED': return ['lock', 'cancel'];
      case 'OPEN':      return ['lock', 'cancel'];
      case 'LOCKED':    return ['unlock', 'cancel'];
      case 'CLOSED':    return ['cancel'];
      case 'RESULTED':
      case 'SETTLED':   return ['archive'];
      default:          return [];
    }
  }

  lifecycleLabel(action: DrawLifecycleAction): string {
    switch (action) {
      case 'lock':    return 'Verrouiller';
      case 'unlock':  return 'Déverrouiller';
      case 'cancel':  return 'Annuler';
      case 'archive': return 'Archiver';
    }
  }

  lifecycleIcon(action: DrawLifecycleAction): string {
    switch (action) {
      case 'lock':    return 'lock';
      case 'unlock':  return 'lock_open';
      case 'cancel':  return 'cancel';
      case 'archive': return 'inventory_2';
    }
  }

  onLifecycleAction(draw: GeneratedDrawView, action: DrawLifecycleAction): void {
    switch (action) {
      case 'lock':    this.lockDraw.emit(draw);    break;
      case 'unlock':  this.unlockDraw.emit(draw);  break;
      case 'cancel':  this.cancelDraw.emit(draw);  break;
      case 'archive': this.archiveDraw.emit(draw); break;
    }
  }
}
