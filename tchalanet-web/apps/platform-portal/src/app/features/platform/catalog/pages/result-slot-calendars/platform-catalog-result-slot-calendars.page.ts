import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import {
  CatalogId,
  CatalogResultSlotCalendarOverrideView,
  CatalogResultSlotView,
  PlatformCatalogApi,
} from '../../data-access/platform-catalog-api.service';

@Component({
  selector: 'tch-platform-catalog-result-slot-calendars-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './platform-catalog-result-slot-calendars.page.html',
})
export class PlatformCatalogResultSlotCalendarsPage {
  private readonly api = inject(PlatformCatalogApi);
  private readonly snackBar = inject(MatSnackBar);

  readonly columns = ['shape', 'available', 'reasonCode', 'reasonLabel', 'actions'];
  readonly slots = this.api.listResultSlotsResource({ suppressShellFeedback: true });
  readonly slotError = resourceErrorVm(this.slots, 'platform.catalog.resultSlotCalendars.slots');
  readonly slotRows = computed(() => this.slots.value() ?? []);

  readonly selectedSlotId = signal<string>('');
  readonly slotLocalDate = signal('');
  readonly recurringMd = signal('');
  readonly available = signal(false);
  readonly reasonCode = signal('NO_DRAW');
  readonly reasonLabel = signal('');
  readonly busy = signal(false);

  readonly overrides = this.api.listResultSlotCalendarOverridesResource(
    () => this.selectedSlotId() || undefined,
    { suppressShellFeedback: true },
  );
  readonly overridesError = resourceErrorVm(this.overrides, 'platform.catalog.resultSlotCalendars');
  readonly overrideRows = computed(() => this.overrides.value() ?? []);

  selectSlot(id: string): void {
    this.selectedSlotId.set(id);
  }

  reload(): void {
    this.slots.reload();
    this.overrides.reload();
  }

  create(): void {
    const slotId = this.selectedSlotId();
    const slotLocalDate = this.slotLocalDate().trim();
    const recurringMd = this.recurringMd().trim();
    const reasonCode = this.reasonCode().trim();
    if (!slotId || !reasonCode || (!!slotLocalDate === !!recurringMd)) {
      this.snackBar.open('Renseignez une date OU une récurrence MM-dd, avec un code raison.', 'OK', {
        duration: 5000,
      });
      return;
    }

    this.busy.set(true);
    this.api.createResultSlotCalendarOverride(slotId, {
      slotLocalDate: slotLocalDate || null,
      recurringMd: recurringMd || null,
      available: this.available(),
      reasonCode,
      reasonLabel: this.reasonLabel().trim() || null,
    }).subscribe({
      next: () => {
        this.busy.set(false);
        this.slotLocalDate.set('');
        this.recurringMd.set('');
        this.reasonLabel.set('');
        this.snackBar.open('Exception créée.', 'OK', { duration: 4000 });
        this.overrides.reload();
      },
      error: err => this.handleError(err),
    });
  }

  edit(row: CatalogResultSlotCalendarOverrideView): void {
    const slotId = this.selectedSlotId();
    const overrideId = idValue(row.id);
    if (!slotId || !overrideId) return;

    const reasonCode = window.prompt('Code raison', row.reasonCode);
    if (reasonCode == null) return;
    const reasonLabel = window.prompt('Libellé raison', row.reasonLabel ?? '') ?? row.reasonLabel;

    this.busy.set(true);
    this.api.updateResultSlotCalendarOverride(slotId, overrideId, {
      available: row.available,
      reasonCode: reasonCode.trim(),
      reasonLabel: reasonLabel?.trim() || null,
    }).subscribe({
      next: () => {
        this.busy.set(false);
        this.snackBar.open('Exception mise à jour.', 'OK', { duration: 4000 });
        this.overrides.reload();
      },
      error: err => this.handleError(err),
    });
  }

  toggle(row: CatalogResultSlotCalendarOverrideView): void {
    const slotId = this.selectedSlotId();
    const overrideId = idValue(row.id);
    if (!slotId || !overrideId) return;

    this.busy.set(true);
    this.api.updateResultSlotCalendarOverride(slotId, overrideId, {
      available: !row.available,
      reasonCode: row.reasonCode,
      reasonLabel: row.reasonLabel,
    }).subscribe({
      next: () => {
        this.busy.set(false);
        this.snackBar.open('Disponibilité mise à jour.', 'OK', { duration: 4000 });
        this.overrides.reload();
      },
      error: err => this.handleError(err),
    });
  }

  delete(row: CatalogResultSlotCalendarOverrideView): void {
    const slotId = this.selectedSlotId();
    const overrideId = idValue(row.id);
    if (!slotId || !overrideId) return;
    if (!confirm('Supprimer cette exception de calendrier ?')) return;

    this.busy.set(true);
    this.api.deleteResultSlotCalendarOverride(slotId, overrideId).subscribe({
      next: () => {
        this.busy.set(false);
        this.snackBar.open('Exception supprimée.', 'OK', { duration: 4000 });
        this.overrides.reload();
      },
      error: err => this.handleError(err),
    });
  }

  slotLabel(slot: CatalogResultSlotView): string {
    return `${slot.slotKey} - ${slot.drawTime}`;
  }

  shape(row: CatalogResultSlotCalendarOverrideView): string {
    return row.slotLocalDate ?? row.recurringMd ?? '-';
  }

  idText(id: CatalogId): string {
    return idValue(id);
  }

  private handleError(err: unknown): void {
    this.busy.set(false);
    this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', {
      duration: 5000,
    });
  }
}

function idValue(id: CatalogId): string {
  return typeof id === 'string' ? id : id.value;
}
