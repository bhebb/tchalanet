import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { ConsoleTicketDrawCardComponent } from './console-ticket-draw-card.component';
import { ConsoleTicketSelectionsCardComponent } from './console-ticket-selections-card.component';

describe('console ticket detail cards', () => {
  it('renders the shared draw identity and facts inside the standard section card', () => {
    const fixture = TestBed.createComponent(ConsoleTicketDrawCardComponent);
    fixture.componentRef.setInput('title', 'Tiraj');
    fixture.componentRef.setInput('identity', {
      providerCode: 'GA',
      providerName: 'Georgia',
      channelName: 'Georgia · Late',
      slotKey: 'GA_LATE',
    });
    fixture.componentRef.setInput('facts', [
      { label: 'Kanal', value: 'Georgia · Late' },
      { label: 'Dat ak lè', value: '31/07/2026 23:34' },
    ]);
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelector('[data-testid="admin-detail-section"]'),
    ).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Georgia · Late');
    expect(fixture.nativeElement.textContent).toContain('31/07/2026 23:34');
  });

  it('renders Bòlèt, Maryaj and Maryaj gratis selections with the same line structure', () => {
    const fixture = TestBed.createComponent(ConsoleTicketSelectionsCardComponent);
    fixture.componentRef.setInput('title', 'Nimewo yo');
    fixture.componentRef.setInput('lines', [
      {
        lineNumber: 1,
        gameCode: 'HT_BOLET',
        gameLabel: 'Bòlèt',
        selection: '14',
        betTypeLabel: 'Dirèk',
        amountLabel: '25 HTG',
        pricingLabels: [{ value: 'Barèm x20', source: 'Vandè' }],
        pricingTooltip: 'Boul: Barèm x20 · Vandè',
      },
      {
        lineNumber: 2,
        gameCode: 'HT_MARYAJ',
        gameLabel: 'Maryaj',
        selection: '12-34',
        betTypeLabel: 'Maryaj',
        amountLabel: '50 HTG',
      },
      {
        lineNumber: 3,
        gameCode: 'HT_MARYAJ_GRATIS',
        gameLabel: 'Maryaj gratis',
        selection: '56-78',
        amountLabel: '0 HTG',
        promotional: true,
        promotionLabel: 'Maryaj gratis',
      },
    ]);
    fixture.detectChanges();

    expect(
      fixture.nativeElement.querySelectorAll('.console-ticket-selections-card__line'),
    ).toHaveLength(3);
    expect(
      fixture.nativeElement.querySelectorAll('.console-ticket-selections-card__group'),
    ).toHaveLength(3);
    expect(fixture.nativeElement.textContent).toContain('Bòlèt');
    expect(fixture.nativeElement.textContent).toContain('Maryaj gratis');
    const boletTitle = fixture.nativeElement.querySelector(
      '.console-ticket-selections-card__group-title',
    );
    expect(boletTitle.getAttribute('aria-label')).toContain('Barèm x20');
    expect(boletTitle.getAttribute('aria-label')).toContain('Vandè');
    expect(
      fixture.nativeElement.querySelectorAll('.console-ticket-selections-card__pricing-info'),
    ).toHaveLength(0);
    expect(fixture.nativeElement.querySelectorAll('tch-game-selection-chip')).toHaveLength(3);
  });

  it('moves shared pricing tooltip to the game title', () => {
    const fixture = TestBed.createComponent(ConsoleTicketSelectionsCardComponent);
    fixture.componentRef.setInput('title', 'Nimewo yo');
    fixture.componentRef.setInput('lines', [
      {
        lineNumber: 1,
        gameCode: 'HT_BOLET',
        gameLabel: 'Bòlèt',
        selection: '14',
        amountLabel: '25 HTG',
        pricingLabels: [{ value: 'Barèm x20', source: 'Vandè' }],
        pricingTooltip: 'Boul: Barèm x20 · Vandè',
      },
      {
        lineNumber: 2,
        gameCode: 'HT_BOLET',
        gameLabel: 'Bòlèt',
        selection: '15',
        amountLabel: '25 HTG',
        pricingLabels: [{ value: 'Barèm x20', source: 'Vandè' }],
        pricingTooltip: 'Boul: Barèm x20 · Vandè',
      },
    ]);
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector(
      '.console-ticket-selections-card__group-title',
    );
    expect(title.getAttribute('aria-label')).toBe('Boul: Barèm x20 · Vandè');
    expect(
      fixture.nativeElement.querySelectorAll('.console-ticket-selections-card__pricing-info'),
    ).toHaveLength(0);
    expect(fixture.nativeElement.querySelectorAll('mat-menu')).toHaveLength(1);
  });

  it('keeps line pricing tooltip when values differ inside a game', () => {
    const fixture = TestBed.createComponent(ConsoleTicketSelectionsCardComponent);
    fixture.componentRef.setInput('title', 'Nimewo yo');
    fixture.componentRef.setInput('lines', [
      {
        lineNumber: 1,
        gameCode: 'HT_BOLET',
        gameLabel: 'Bòlèt',
        selection: '14',
        amountLabel: '25 HTG',
        pricingLabels: [{ value: 'Barèm x20', source: 'Vandè' }],
        pricingTooltip: 'Boul: Barèm x20 · Vandè',
      },
      {
        lineNumber: 2,
        gameCode: 'HT_BOLET',
        gameLabel: 'Bòlèt',
        selection: '15',
        amountLabel: '25 HTG',
        pricingLabels: [{ value: 'Barèm x10', source: 'Tenant' }],
        pricingTooltip: 'Boul: Barèm x10 · Tenant',
      },
    ]);
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector(
      '.console-ticket-selections-card__group-title',
    );
    expect(title.getAttribute('aria-label')).toBeNull();
    expect(
      fixture.nativeElement.querySelectorAll('.console-ticket-selections-card__pricing-info'),
    ).toHaveLength(2);
  });

  it('prefers the shared game pricing summary over line pricing details', () => {
    const fixture = TestBed.createComponent(ConsoleTicketSelectionsCardComponent);
    fixture.componentRef.setInput('title', 'Nimewo yo');
    fixture.componentRef.setInput('lines', [
      {
        lineNumber: 1,
        gameCode: 'HT_BOLET',
        gameLabel: 'Bòlèt',
        selection: '14',
        amountLabel: '25 HTG',
        pricingTooltip: '1er lot: 65',
        pricingGroupTooltip: 'Barèm Vandè: 65-25-10',
      },
      {
        lineNumber: 2,
        gameCode: 'HT_BOLET',
        gameLabel: 'Bòlèt',
        selection: '15',
        amountLabel: '25 HTG',
        pricingTooltip: '2e lot: 25',
        pricingGroupTooltip: 'Barèm Vandè: 65-25-10',
      },
    ]);
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector(
      '.console-ticket-selections-card__group-title',
    );
    expect(title.getAttribute('aria-label')).toBe('Barèm Vandè: 65-25-10');
    expect(
      fixture.nativeElement.querySelectorAll('.console-ticket-selections-card__pricing-info'),
    ).toHaveLength(0);
  });
});
