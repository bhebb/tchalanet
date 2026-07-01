import '@angular/compiler';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { PageModelApi, PageRuntimeResponse } from '@tch/page-model';
import { of } from 'rxjs';

import { PlatformDashboardPage } from './platform-dashboard.page';

describe('PlatformDashboardPage', () => {
  it('requests the route-selected PageModel logicalId', async () => {
    const api = {
      getPlatformPage: vi.fn(() => of(pageRuntime())),
    };

    await TestBed.configureTestingModule({
      imports: [PlatformDashboardPage],
      providers: [
        provideTranslateService(),
        { provide: PageModelApi, useValue: api },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              data: { pageModelLogicalId: 'private.dashboard.superadmin.ops' },
            },
          },
        },
      ],
    }).compileComponents();

    const fixture: ComponentFixture<PlatformDashboardPage> =
      TestBed.createComponent(PlatformDashboardPage);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(api.getPlatformPage).toHaveBeenCalledWith('private.dashboard.superadmin.ops');
  });
});

function pageRuntime(): PageRuntimeResponse {
  return {
    meta: {
      logicalId: 'private.dashboard.superadmin.ops',
      scope: 'private',
      slug: 'dashboard',
      schemaVersion: 1,
    },
    shell: {
      type: 'private',
      topAppBar: {},
      navigationDrawer: {},
    },
    content: {
      layout: {
        rows: [],
      },
      widgets: {},
    },
    dynamic: {
      widgets: {},
      errors: [],
    },
  };
}
