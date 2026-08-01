import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TchGameSelectionChip } from '@tch/ui/components';

import {
  PosGameView,
  PosTicketDraftLine,
  PosTicketLineInput,
} from '../../data-access/pos-sale.models';

@Component({
  selector: 'tch-pos-ticket-line-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    TchGameSelectionChip,
  ],
  templateUrl: './pos-ticket-line-editor.component.html',
  styleUrls: ['./pos-ticket-line-editor.component.scss'],
})
export class PosTicketLineEditorComponent {
  readonly lines = input<PosTicketDraftLine[]>([]);
  readonly selectedGameCode = input<string | null>(null);
  readonly games = input<PosGameView[]>([]);
  readonly readonly = input(false);
  readonly readonlyMessage = input("Ticket vendu. Créez un nouveau ticket pour ajouter d'autres numéros.");

  readonly lineAdded = output<PosTicketLineInput>();
  readonly lineRemoved = output<string>();
  readonly lineStakeUpdated = output<{ localId: string; stakeAmount: number }>();
  readonly linesCleared = output<void>();

  // Draft new line state
  readonly draftSelection = signal('');
  readonly draftMarriageFirst = signal('');
  readonly draftMarriageSecond = signal('');
  readonly draftBetType = signal('');
  readonly draftBetOption = signal<number | null>(null);
  readonly draftStake = signal<number | null>(null);
  readonly editingLineId = signal<string | null>(null);
  readonly editingStake = signal<number | null>(null);

  readonly selectedGame = computed(() => {
    const selectedCode = this.selectedGameCode();
    return this.games().find(game => game.gameCode === selectedCode) ?? null;
  });

  readonly betTypes = computed(() => this.selectedGame()?.betTypes ?? []);

  readonly selectedBetType = computed(() => {
    const current = this.draftBetType();
    const betTypes = this.betTypes();
    return betTypes.find(betType => betType.betType === current) ?? betTypes[0] ?? null;
  });

  readonly selectedBetTypeCode = computed(() => this.selectedBetType()?.betType ?? '');
  readonly selectedBetOption = computed(() => {
    const betType = this.selectedBetType();
    if (!betType?.requiresOption || betType.selectionPolicy === 'IMPLICIT_BEST_MATCH') return null;

    const current = this.draftBetOption();
    return betType.options.find(option => option.code === current) ?? betType.options[0] ?? null;
  });
  readonly selectedBetOptionCode = computed(() => {
    const code = this.selectedBetOption()?.code;
    return code == null ? '' : String(code);
  });
  readonly requiresBetOption = computed(() => {
    const betType = this.selectedBetType();
    return !!betType?.requiresOption && betType.selectionPolicy !== 'IMPLICIT_BEST_MATCH';
  });
  readonly selectionMaxLength = computed(() => {
    const betType = this.selectedBetType()?.betType ?? '';
    return this.selectionWidth(betType) ?? 12;
  });
  readonly isMarriageSelection = computed(() =>
    this.selectedBetType()?.betType === 'MARRIAGE_2D2D',
  );
  readonly selectionGuide = computed(() => {
    const betType = this.selectedBetType()?.betType ?? '';
    if (betType === 'MARRIAGE_2D2D') return 'Deux numéros de 2 chiffres';
    const width = this.selectionWidth(betType);
    if (width) return `${width} chiffre${width > 1 ? 's' : ''}`;
    return this.selectedBetType()?.selectionHint ?? 'Numéro';
  });
  readonly draftSelectionState = computed((): 'empty' | 'valid' | 'invalid' => {
    const betType = this.selectedBetType()?.betType ?? '';
    const raw = this.draftSelectionValue();
    if (this.isDraftSelectionEmpty()) return 'empty';
    return this.normalizeSelection(raw, betType) ? 'valid' : 'invalid';
  });
  readonly draftSelectionMessage = computed(() => {
    const betType = this.selectedBetType()?.betType ?? '';
    if (this.draftSelectionState() === 'valid') return 'Numéro valide';
    if (this.draftSelectionState() === 'invalid') return this.selectionErrorMessage(betType);
    return this.selectionGuide();
  });

  readonly canAdd = computed(() => {
    const stake = this.draftStake();
    const gameCode = this.selectedGameCode();
    return !this.readonly() &&
      !!this.normalizeSelection(this.draftSelectionValue(), this.selectedBetType()?.betType ?? '') &&
      stake !== null &&
      stake > 0 &&
      !!gameCode &&
      !!this.selectedBetType() &&
      (!this.requiresBetOption() || !!this.selectedBetOption());
  });

  readonly duplicateDraftLine = computed(() => {
    const gameCode = this.selectedGameCode();
    const betType = this.selectedBetType();
    const selection = this.normalizeSelection(this.draftSelectionValue(), betType?.betType ?? '');
    if (!gameCode || !betType || !selection) return null;

    return this.lines().find(line =>
      line.gameCode === gameCode &&
      line.betType === betType.betType &&
      (line.betOption ?? null) === (this.selectedBetOption()?.code ?? null) &&
      line.selection === selection,
    ) ?? null;
  });

  readonly projectedStakeAmount = computed(() => {
    const duplicate = this.duplicateDraftLine();
    const stake = this.draftStake();
    if (!duplicate || !stake || stake <= 0) return null;
    return duplicate.stakeAmount + stake;
  });

  readonly addButtonLabel = computed(() =>
    this.duplicateDraftLine() ? 'Ajouter à la ligne' : 'Ajouter',
  );

  readonly addError = signal<string | null>(null);

  addLine(): void {
    const sel = this.draftSelectionValue().trim();
    const stake = this.draftStake();
    const gameCode = this.selectedGameCode();
    const betType = this.selectedBetType();
    const betOption = this.selectedBetOption();
    const selection = this.normalizeSelection(sel, betType?.betType ?? '');

    if (this.readonly()) { this.addError.set(this.readonlyMessage()); return; }
    if (!sel) { this.addError.set('Le numéro est requis.'); return; }
    if (!stake || stake <= 0) { this.addError.set('La mise doit être supérieure à 0 HTG.'); return; }
    if (!gameCode) { this.addError.set('Sélectionnez un jeu.'); return; }
    if (!betType) { this.addError.set('Aucun type de pari disponible pour ce jeu.'); return; }
    if (this.requiresBetOption() && !betOption) { this.addError.set('Sélectionnez une option de pari.'); return; }
    if (!selection) { this.addError.set(this.selectionErrorMessage(betType.betType)); return; }

    this.addError.set(null);
    this.lineAdded.emit({
      gameCode,
      selection,
      betType: betType.betType,
      betTypeLabel: betType.label,
      betOption: betOption?.code ?? null,
      betOptionLabel: betOption?.label ?? null,
      stakeAmount: stake,
    });
    this.draftSelection.set('');
    this.draftMarriageFirst.set('');
    this.draftMarriageSecond.set('');
    this.draftStake.set(null);
  }

  onDraftKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') this.addLine();
  }

  onBetTypeChange(betType: string): void {
    this.draftBetType.set(betType);
    this.draftSelection.set('');
    this.draftMarriageFirst.set('');
    this.draftMarriageSecond.set('');
    const next = this.betTypes().find(item => item.betType === betType);
    this.draftBetOption.set(
      next?.requiresOption && next.selectionPolicy !== 'IMPLICIT_BEST_MATCH'
        ? (next.options[0]?.code ?? null)
        : null,
    );
  }

  setDraftSelection(value: string): void {
    this.draftSelection.set(value.replace(/\D/g, ''));
  }

  setMarriageFirst(value: string): void {
    this.draftMarriageFirst.set(value.replace(/\D/g, '').slice(0, 2));
  }

  setMarriageSecond(value: string): void {
    this.draftMarriageSecond.set(value.replace(/\D/g, '').slice(0, 2));
  }

  removeLine(localId: string): void {
    if (this.readonly()) return;
    this.lineRemoved.emit(localId);
  }

  startEdit(line: PosTicketDraftLine): void {
    if (this.readonly()) return;
    this.editingLineId.set(line.localId);
    this.editingStake.set(line.stakeAmount);
  }

  cancelEdit(): void {
    this.editingLineId.set(null);
    this.editingStake.set(null);
  }

  saveEdit(line: PosTicketDraftLine): void {
    const stake = this.editingStake();
    if (this.readonly() || stake === null || stake <= 0) return;
    this.lineStakeUpdated.emit({ localId: line.localId, stakeAmount: stake });
    this.cancelEdit();
  }

  private normalizeSelection(selection: string, betType: string): string | null {
    if (betType === 'MARRIAGE_2D2D') {
      const match = /^(\d{2})-(\d{2})$/.exec(selection.trim());
      if (!match) return null;
      return `${match[1]}-${match[2]}`;
    }

    const digits = selection.replace(/\D/g, '');
    const width = this.selectionWidth(betType);
    if (!width) return selection.trim();
    if (this.isLotoBetType(betType) && digits.length !== width) return null;
    if (digits.length === 0 || digits.length > width) return null;
    return digits.padStart(width, '0');
  }

  private selectionWidth(betType: string): number | null {
    switch (betType) {
      case 'MATCH_1_2D':
      case 'MATCH_2_2D':
      case 'MATCH_3_2D':
        return 2;
      case 'LOTTO3_3D':
        return 3;
      case 'LOTTO4_PATTERN':
        return 4;
      case 'LOTTO5_PATTERN':
        return 5;
      default:
        return null;
    }
  }

  private selectionErrorMessage(betType: string): string {
    if (betType === 'MARRIAGE_2D2D') {
      return 'Entrez deux numéros de 2 chiffres.';
    }
    const width = this.selectionWidth(betType);
    if (!width) return 'La sélection est invalide.';
    return `Entrez un numéro de ${width} chiffre${width > 1 ? 's' : ''}.`;
  }

  private draftSelectionValue(): string {
    if (this.isMarriageSelection()) {
      return `${this.draftMarriageFirst()}-${this.draftMarriageSecond()}`;
    }

    return this.draftSelection();
  }

  private isDraftSelectionEmpty(): boolean {
    if (this.isMarriageSelection()) {
      return !this.draftMarriageFirst() && !this.draftMarriageSecond();
    }

    return !this.draftSelection().trim();
  }

  private isLotoBetType(betType: string): boolean {
    return betType === 'LOTTO3_3D' ||
      betType === 'LOTTO4_PATTERN' ||
      betType === 'LOTTO5_PATTERN';
  }
}
