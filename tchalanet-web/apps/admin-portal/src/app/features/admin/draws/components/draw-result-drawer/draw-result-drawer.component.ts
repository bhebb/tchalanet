import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AdminGeneratedDrawsApiService } from '../../data-access/admin-generated-draws-api.service';
import {
  DrawResultSaveMode,
  GeneratedDrawView,
  SaveDrawResultRequest,
} from '../../data-access/admin-generated-draws.models';
import { GeneratedDrawStatusBadgeComponent } from '../generated-draw-status-badge/generated-draw-status-badge.component';

export type DrawerMode = 'saisie' | 'lecture' | 'source-error' | 'modification';
type DrawerState = 'ready' | 'saving' | 'success' | 'error';

const TZ_ABBREV: Record<string, string> = {
  'America/New_York':   'EDT',
  'America/Chicago':    'CDT',
  'America/Los_Angeles':'PDT',
  'America/Denver':     'MDT',
  'America/Phoenix':    'MST',
};

const SALES_LABELS: Record<string, string> = {
  OPEN: 'Ouverte', CLOSED: 'Fermée', UPCOMING: 'À venir', CANCELLED: 'Annulé',
};

@Component({
  selector: 'tch-draw-result-drawer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    GeneratedDrawStatusBadgeComponent,
  ],
  templateUrl: './draw-result-drawer.component.html',
  styleUrls: ['./draw-result-drawer.component.scss'],
})
export class DrawResultDrawerComponent implements OnInit {
  private readonly api = inject(AdminGeneratedDrawsApiService);

  readonly draw = input.required<GeneratedDrawView>();
  readonly closed = output<void>();
  readonly resultSaved = output<GeneratedDrawView>();

  readonly mode  = signal<DrawerMode>('saisie');
  readonly state = signal<DrawerState>('ready');
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage   = signal<string | null>(null);

  readonly n2Ref = viewChild<ElementRef<HTMLInputElement>>('n2Input');
  readonly n3Ref = viewChild<ElementRef<HTMLInputElement>>('n3Input');

  readonly form = new FormGroup({
    n1: new FormControl('', [Validators.required, Validators.pattern(/^\d{2}$/)]),
    n2: new FormControl('', [Validators.required, Validators.pattern(/^\d{2}$/)]),
    n3: new FormControl('', [Validators.required, Validators.pattern(/^\d{2}$/)]),
    note: new FormControl(''),
  });

  readonly isSaisie     = computed(() => this.mode() === 'saisie');
  readonly isLecture    = computed(() => this.mode() === 'lecture');
  readonly isSourceError = computed(() => this.mode() === 'source-error');
  readonly isModifying  = computed(() => this.mode() === 'modification');
  readonly isEditing    = computed(() => this.isSaisie() || this.isModifying());

  readonly noteRequired = computed(() => this.mode() === 'modification' || this.mode() === 'source-error');

  readonly title = computed<string>(() => {
    switch (this.mode()) {
      case 'saisie':       return 'Saisir le résultat';
      case 'lecture':      return 'Résultat confirmé';
      case 'source-error': return 'Vérifier la source';
      case 'modification': return 'Modifier le résultat';
    }
  });

  readonly subtitle = computed<string>(() => {
    const d = this.draw();
    const date = new Date(d.businessDate + 'T00:00:00').toLocaleDateString('fr-FR', {
      day: 'numeric', month: 'long', year: 'numeric',
    });
    const time = d.scheduledAt.slice(11, 16);
    const tz = TZ_ABBREV[d.timezone] ?? '';
    return `${d.slotLabel} · ${date} · ${time}${tz ? ` ${tz}` : ''}`;
  });

  readonly salesStatusLabel = computed(() => SALES_LABELS[this.draw().salesStatus] ?? this.draw().salesStatus);

  readonly isProvisional = computed(() =>
    this.mode() === 'lecture' && this.draw().resultStatus === 'PROVISIONAL',
  );

  ngOnInit(): void {
    const d = this.draw();
    if (d.resultStatus === 'CONFIRMED' || d.resultStatus === 'PROVISIONAL') {
      this.mode.set('lecture');
      if (d.numbers?.length === 3) {
        this.form.patchValue({ n1: d.numbers[0], n2: d.numbers[1], n3: d.numbers[2] });
      }
      this.form.disable();
    } else if (d.resultStatus === 'SOURCE_ERROR') {
      this.mode.set('source-error');
    } else {
      this.mode.set('saisie');
    }
  }

  onModify(): void {
    this.mode.set('modification');
    this.form.enable();
  }

  onEnterManually(): void {
    this.mode.set('saisie');
    this.form.reset();
    this.form.enable();
  }

  onSave(saveMode: DrawResultSaveMode): void {
    this.form.markAllAsTouched();

    const noteVal = (this.form.value.note ?? '').trim();
    if (this.noteRequired() && !noteVal) {
      this.form.get('note')!.setErrors({ required: true });
      return;
    }
    if (this.form.get('n1')!.invalid || this.form.get('n2')!.invalid || this.form.get('n3')!.invalid) {
      return;
    }

    const v = this.form.value;
    const req: SaveDrawResultRequest = {
      drawId: this.draw().drawId,
      numbers: [v.n1!, v.n2!, v.n3!],
      note: noteVal,
      mode: saveMode,
    };

    this.state.set('saving');
    this.errorMessage.set(null);

    this.api.saveDrawResult(req).subscribe({
      next: updated => {
        this.state.set('success');
        this.successMessage.set(
          saveMode === 'confirmed'
            ? 'Résultat confirmé et publié.'
            : 'Résultat enregistré en provisoire.',
        );
        this.resultSaved.emit(updated);
      },
      error: () => {
        this.state.set('error');
        this.errorMessage.set('Impossible d\'enregistrer le résultat.');
      },
    });
  }

  onClose(): void { this.closed.emit(); }

  onNumberInput(event: Event, next: ElementRef<HTMLInputElement> | undefined): void {
    const el = event.target as HTMLInputElement;
    const clean = el.value.replace(/\D/g, '').slice(0, 2);
    if (el.value !== clean) el.value = clean;
    if (clean.length >= 2 && next) {
      next.nativeElement.focus();
      next.nativeElement.select();
    }
  }

  fieldError(name: string): string | null {
    const ctrl = this.form.get(name);
    if (!ctrl?.touched || !ctrl.errors) return null;
    if (ctrl.errors['required']) {
      return name === 'note'
        ? 'Une raison est requise pour modifier un résultat confirmé.'
        : 'Le numéro est requis.';
    }
    if (ctrl.errors['pattern']) return 'Format invalide — 2 chiffres, ex : 05 ou 23.';
    return null;
  }
}
