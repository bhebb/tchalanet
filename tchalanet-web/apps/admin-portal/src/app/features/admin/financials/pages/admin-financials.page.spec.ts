import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AdminFinancialsApi } from '../data-access/admin-financials-api.service';
import { AdminFinancialsPage } from './admin-financials.page';

describe('AdminFinancialsPage', () => {
  it('shows an empty state for a new tenant without projected financial data', () => {
    const api = { getBreakdown: vi.fn(() => of(emptyView())) };
    TestBed.configureTestingModule({
      imports: [AdminFinancialsPage],
      providers: [{ provide: AdminFinancialsApi, useValue: api }, provideNoopAnimations()],
    });

    const fixture = TestBed.createComponent(AdminFinancialsPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune donnée financière');
    expect(fixture.nativeElement.textContent).toContain("Ce tenant n'a pas encore de ventes");
  });
});

function emptyView() {
  return {
    from: '2026-06-25',
    to: '2026-06-25',
    summary: {
      ticketsSold: 0,
      grossSales: 0,
      winningsCalculated: 0,
      payoutsPaid: 0,
      sellerCommission: 0,
      buyerCharges: 0,
      sellerCharges: 0,
      tenantCharges: 0,
      waivedCharges: 0,
      promotionLines: 0,
      promotionPricedLines: 0,
      promotionPayoutBase: 0,
      promotionPotentialPayout: 0,
      netRevenueEstimated: 0,
      netRevenuePaidBasis: 0,
    },
    dailyRows: [],
    drawRows: [],
    sellerTerminalDrawRows: [],
    sellerTerminalDailyRows: [],
  };
}
