import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AdminSectionCardComponent, AdminStatusPillComponent } from '@tch/ui/console';

import { ConsoleDrawSlotIdentityComponent } from '../draw-slots/console-draw-slot-identity.component';
import { ConsoleEntityDetailComponent } from '../entity-detail/console-entity-detail.component';
import {
  ConsoleEntityDetailActionEvent,
  ConsoleEntityDetailError,
  ConsoleEntityDetailState,
} from '../entity-detail/console-entity-detail.models';
import { ConsoleFactsComponent } from '../entity-detail/console-facts.component';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';
import {
  ConsoleDrawDetailSection,
  ConsoleDrawDetailView,
} from './console-draw-detail.models';

export interface ConsoleDrawDetailSectionActionEvent {
  readonly section: ConsoleDrawDetailSection;
  readonly action: ConsoleRowAction;
}

@Component({
  selector: 'tch-console-draw-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    ConsoleDrawSlotIdentityComponent,
    ConsoleEntityDetailComponent,
    ConsoleFactsComponent,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './console-draw-detail.component.html',
  styleUrls: ['./console-draw-detail.component.scss'],
})
export class ConsoleDrawDetailComponent {
  readonly view = input<ConsoleDrawDetailView | null>(null);
  readonly state = input.required<ConsoleEntityDetailState>();
  readonly loadingLabel = input('Chargement du tirage...');
  readonly error = input<ConsoleEntityDetailError | null>(null);
  readonly actions = input<readonly ConsoleRowAction[]>([]);

  readonly retry = output<void>();
  readonly action = output<ConsoleEntityDetailActionEvent>();
  readonly sectionAction = output<ConsoleDrawDetailSectionActionEvent>();

  readonly title = computed(() => this.view()?.title ?? 'Détail du tirage');
  readonly description = computed(() => this.view()?.description ?? '');
  readonly meta = computed(() => this.view()?.meta ?? []);

  emitSectionAction(section: ConsoleDrawDetailSection, action: ConsoleRowAction): void {
    this.sectionAction.emit({ section, action });
  }
}
