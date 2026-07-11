import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PosGameView } from '../../data-access/pos-sale.models';
import { PosTicketLineEditorComponent } from './pos-ticket-line-editor.component';

describe(PosTicketLineEditorComponent.name, () => {
  let fixture: ComponentFixture<PosTicketLineEditorComponent>;
  let component: PosTicketLineEditorComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PosTicketLineEditorComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(PosTicketLineEditorComponent);
    component = fixture.componentInstance;
  });

  it('hides bet option selector when policy is implicit best match', () => {
    setGames([gameWithPolicy('IMPLICIT_BEST_MATCH')]);

    fixture.detectChanges();

    expect(optionSelect()).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Exact');
    expect(fixture.nativeElement.textContent).not.toContain('Permuté');
  });

  it('shows exact and permute options when policy is explicit', () => {
    setGames([gameWithPolicy('EXPLICIT_ONLY')]);

    fixture.detectChanges();

    const select = optionSelect();
    expect(select).not.toBeNull();
    expect(optionLabels(select!)).toEqual(['Exact', 'Permuté']);
  });

  it('emits no bet option for implicit best match lines', () => {
    const emitted: unknown[] = [];
    setGames([gameWithPolicy('IMPLICIT_BEST_MATCH')]);
    component.lineAdded.subscribe(value => emitted.push(value));
    component.draftMarriageFirst.set('12');
    component.draftMarriageSecond.set('34');
    component.draftStake.set(25);

    component.addLine();

    expect(emitted).toHaveLength(1);
    expect(emitted[0]).toMatchObject({
      selection: '12-34',
      betOption: null,
      betOptionLabel: null,
    });
  });

  function setGames(games: PosGameView[]): void {
    fixture.componentRef.setInput('selectedGameCode', games[0]?.gameCode ?? null);
    fixture.componentRef.setInput('games', games);
  }

  function optionSelect(): HTMLSelectElement | null {
    return fixture.nativeElement.querySelector('select[aria-label="Option de pari"]');
  }

  function optionLabels(select: HTMLSelectElement): string[] {
    return Array.from(select.options).map(option => option.textContent?.trim() ?? '');
  }

  function gameWithPolicy(selectionPolicy: PosGameView['selectionPolicy']): PosGameView {
    return {
      gameCode: 'HT_MARYAJ',
      label: 'Maryaj',
      enabled: true,
      betType: 'MARRIAGE_2D2D',
      betTypeLabel: 'Maryaj',
      requiresOption: true,
      selectionPolicy,
      options: [
        { code: 1, label: 'Exact' },
        { code: 2, label: 'Permuté' },
      ],
      betTypes: [
        {
          betType: 'MARRIAGE_2D2D',
          label: 'Maryaj',
          requiresOption: true,
          selectionPolicy,
          options: [
            { code: 1, label: 'Exact' },
            { code: 2, label: 'Permuté' },
          ],
        },
      ],
    };
  }
});
