import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { DrawAdminApi, type DrawChannelSummary } from '../../data-access/draw-admin.api.service';

export type { DrawChannelSummary };

/**
 * Reusable picker for the tenant's active draw channels.
 *
 * - Loads channels once from the API on init.
 * - Filters locally as the user types (no extra API calls).
 * - Accepts an optional `preselectedId` to pre-fill the field (e.g., from a draw context).
 * - Emits `channelChange` whenever the selection changes (null = cleared).
 */
@Component({
  selector: 'tch-active-draw-channel-select',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <mat-form-field appearance="outline" class="adcs__field">
      <mat-label>Canal de tirage</mat-label>
      <input
        matInput
        [formControl]="query"
        [matAutocomplete]="auto"
        placeholder="Rechercher un canal..."
        autocomplete="off"
      />
      @if (loading()) {
        <mat-spinner matSuffix diameter="16" />
      }
      <mat-autocomplete
        #auto="matAutocomplete"
        [displayWith]="displayChannel"
        (optionSelected)="select($event.option.value)"
      >
        @for (ch of filtered(); track ch.id) {
          <mat-option [value]="ch">
            <span class="adcs__option">
              <span class="adcs__option-name">{{ ch.channelName }}</span>
              <span class="adcs__option-code">{{ ch.channelCode }}</span>
            </span>
          </mat-option>
        }
        @if (!loading() && filtered().length === 0) {
          <mat-option disabled>Aucun canal trouvé</mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
  styles: [`
    :host { display: block; }

    .adcs__field { width: 100%; }

    .adcs__option {
      display: flex;
      align-items: baseline;
      gap: 0.5rem;
    }

    .adcs__option-name {
      font-weight: 600;
      color: var(--tch-color-on-surface);
    }

    .adcs__option-code {
      font-size: 0.75rem;
      color: var(--tch-color-on-surface-variant);
    }

    .material-symbols-outlined {
      font-family: 'Material Symbols Outlined';
    }
  `],
})
export class ActiveDrawChannelSelectComponent implements OnInit {
  private readonly api = inject(DrawAdminApi);
  private readonly destroyRef = inject(DestroyRef);

  /** Pre-select a channel by its ID (e.g., from a draw's drawChannelId). */
  readonly preselectedId = input<string | null | undefined>(undefined);

  /** Emits the selected channel, or null when cleared. */
  readonly channelChange = output<DrawChannelSummary | null>();

  protected readonly query = new FormControl<string | DrawChannelSummary>('');
  protected readonly channels = signal<DrawChannelSummary[]>([]);
  protected readonly loading = signal(true);
  protected readonly filterText = signal('');

  protected readonly filtered = computed(() => {
    const text = this.filterText().toLowerCase().trim();
    const active = this.channels();
    if (!text) return active;
    return active.filter(
      c =>
        c.channelName.toLowerCase().includes(text) ||
        c.channelCode.toLowerCase().includes(text),
    );
  });

  constructor() {
    // When channels load and a preselectedId is known, auto-fill the field.
    effect(() => {
      const id = this.preselectedId();
      const chs = this.channels();
      if (!id || !chs.length) return;
      const found = chs.find(c => c.id === id);
      if (found) {
        this.query.setValue(found, { emitEvent: false });
        this.channelChange.emit(found);
      }
    });
  }

  ngOnInit(): void {
    this.api
      .listChannels()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: chs => {
          this.channels.set(chs.filter(c => c.active));
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });

    this.query.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      if (typeof value === 'string') {
        this.filterText.set(value);
        if (!value.trim()) {
          this.channelChange.emit(null);
        }
      }
    });
  }

  protected readonly displayChannel = (ch: DrawChannelSummary | string | null): string => {
    if (!ch || typeof ch === 'string') return '';
    return ch.channelName;
  };

  protected select(ch: DrawChannelSummary): void {
    this.filterText.set('');
    this.channelChange.emit(ch);
  }
}
