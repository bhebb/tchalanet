import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, of, throwError } from 'rxjs';

import { PlatformArchiveApi } from '../../data-access/platform-archive-api.service';
import { PlatformArchivePage } from './platform-archive.page';

describe('PlatformArchivePage', () => {
  it('shows the dev empty state when no archive run exists', () => {
    const api = archiveApi({ listRuns: of([]) });
    const fixture = createPage(api, 'overview');

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Aucun run d'archive");
    expect(fixture.nativeElement.textContent).toContain('Déclenchez un run manuel');
    expect(api.getOpsSummary).toHaveBeenCalledOnce();
    expect(api.listRuns).toHaveBeenCalledWith(50);
  });

  it('shows a loading state while recent runs are loading', () => {
    const pendingRuns = new Subject<never[]>();
    const api = archiveApi({ listRuns: pendingRuns.asObservable() });
    const fixture = createPage(api, 'recent');

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Chargement...');
  });

  it('shows the backend error title when recent runs fail to load', () => {
    const api = archiveApi({
      listRuns: throwError(() => ({ error: { title: 'Archive API unavailable' } })),
    });
    const fixture = createPage(api, 'recent');

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Archive API unavailable');
  });

  it('renders failed run rows in the raw issue view', () => {
    const api = archiveApi({
      listRuns: of([]),
      listFailedRuns: of([{ id: 'run-1', status: 'FAILED', reason: 'checksum mismatch' }]),
    });
    const fixture = createPage(api, 'failed');
    fixture.detectChanges();

    fixture.componentInstance.loadFailed();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('checksum mismatch');
    expect(fixture.nativeElement.textContent).toContain('1 résultat(s)');
    expect(api.listFailedRuns).toHaveBeenCalledWith(20);
  });

  it('renders invalid object rows in the raw issue view', () => {
    const api = archiveApi({
      listRuns: of([]),
      listInvalidObjects: of([{ table_name: 'sales_ticket', row_count: 3, status: 'INVALID' }]),
    });
    const fixture = createPage(api, 'invalid');
    fixture.detectChanges();

    fixture.componentInstance.loadInvalid();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('sales_ticket');
    expect(fixture.nativeElement.textContent).toContain('INVALID');
    expect(api.listInvalidObjects).toHaveBeenCalledWith(20);
  });

  it('loads active legal holds for the legal-holds route', () => {
    const api = archiveApi({
      listRuns: of([]),
      listActiveLegalHolds: of([{ id: 'hold-1', dataset_code: 'sales_ticket' }]),
    });
    const fixture = createPage(api, 'legal-holds');

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('sales_ticket');
    expect(api.listActiveLegalHolds).toHaveBeenCalledWith(50);
  });

  it('loads partition cleanup plan for the partitions route', () => {
    const api = archiveApi({
      listRuns: of([]),
      getPartitionCleanupPlan: of([{ partitionName: 'audit_log_2025_01', tableName: 'audit_log' }]),
    });
    const fixture = createPage(api, 'partitions');

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('audit_log_2025_01');
    expect(api.getPartitionCleanupPlan).toHaveBeenCalledWith('audit_log', expect.any(String));
  });

  it('renders the purge panel for the purges route', () => {
    const api = archiveApi();
    const fixture = createPage(api, 'purges');

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Dry run');
    expect(fixture.nativeElement.textContent).toContain('Cible');
    expect(fixture.nativeElement.textContent).toContain('Aucun dry-run exécuté');
  });
});

function createPage(api: ReturnType<typeof archiveApi>, archiveView = 'overview') {
  TestBed.configureTestingModule({
    imports: [PlatformArchivePage],
    providers: [
      { provide: PlatformArchiveApi, useValue: api },
      { provide: ActivatedRoute, useValue: { snapshot: { data: { archiveView } } } },
      { provide: MatDialog, useValue: { open: vi.fn() } },
      { provide: MatSnackBar, useValue: { open: vi.fn() } },
      provideNoopAnimations(),
    ],
  });

  return TestBed.createComponent(PlatformArchivePage);
}

function archiveApi(overrides: Partial<Record<keyof PlatformArchiveApi, unknown>> = {}) {
  return {
    listRuns: vi.fn(() => of([completedRun()])),
    triggerRun: vi.fn(),
    listFailedRuns: vi.fn(() => of([])),
    listInvalidObjects: vi.fn(() => of([])),
    listActiveLegalHolds: vi.fn(() => of([])),
    getPartitionCleanupPlan: vi.fn(() => of([])),
    getOpsSummary: vi.fn(() => of({
      failedRuns: 0,
      startedRuns: 0,
      completedRuns: 0,
      invalidObjects: 0,
      verifiedObjects: 0,
      pendingObjects: 0,
    })),
    restoreAuditLog: vi.fn(),
    purgeTickets: vi.fn(() => of({ mode: 'DRY_RUN', plan: { eligible: true } })),
    purgeDomain: vi.fn(() => of({ mode: 'DRY_RUN', plan: { eligible: true } })),
    ...Object.fromEntries(
      Object.entries(overrides).map(([key, value]) => [
        key,
        typeof value === 'function' ? value : vi.fn(() => value),
      ]),
    ),
  } as unknown as PlatformArchiveApi & Record<string, ReturnType<typeof vi.fn>>;
}

function completedRun() {
  return {
    id: '11111111-1111-1111-1111-111111111111',
    status: 'COMPLETED',
    strategy: 'MANUAL',
    triggerType: 'MANUAL',
    idempotencyKey: 'run:global:2026-06-01:2026-07-01',
    startedAt: '2026-06-28T08:00:00Z',
    completedAt: '2026-06-28T08:00:01Z',
    errorMessage: null,
  };
}
