import { TestBed } from '@angular/core/testing';

import { App } from './app';
import { AppRuntimeStore } from './core/runtime';

describe('App', () => {
  const runtime = {
    initPublicRuntime: vi.fn(),
  };

  beforeEach(async () => {
    runtime.initPublicRuntime.mockClear();

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: AppRuntimeStore, useValue: runtime }],
    }).compileComponents();
  });

  it('initializes public runtime and renders the router outlet', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(runtime.initPublicRuntime).toHaveBeenCalledOnce();
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });
});
