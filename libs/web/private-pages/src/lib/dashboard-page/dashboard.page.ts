import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { toSignal } from '@angular/core/rxjs-interop';
import { selectPage } from '@tchl/data-access/page';
import { GridLayoutComponent } from '@tchl/ui/layout';

@Component({
  standalone: true,
  selector: 'tchl-dashboard-page',
  imports: [CommonModule, GridLayoutComponent],
  template: `
    @if (page(); as p) {
    <tchl-grid-layout [layout]="p.layout" />
    } @else {
    <div style="padding:16px; opacity:.7">Chargement…</div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  private store = inject(Store);
  page = toSignal(this.store.select(selectPage));
}
