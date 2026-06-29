import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { PrivateShellLayoutComponent } from './private-shell-layout.component';

describe('PrivateShellLayoutComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideTranslateService()] });
  });

  it('keeps the drawer closed by default and toggles it explicitly', () => {
    const component = TestBed.createComponent(PrivateShellLayoutComponent).componentInstance;

    expect(component.drawerOpen()).toBe(false);

    component.toggleDrawer();
    expect(component.drawerOpen()).toBe(true);

    component.closeDrawer();
    expect(component.drawerOpen()).toBe(false);
  });

  it('emits a theme toggle request without owning theme state', () => {
    const component = TestBed.createComponent(PrivateShellLayoutComponent).componentInstance;
    let emitted = 0;

    component.themeToggled.subscribe(() => {
      emitted += 1;
    });

    component.themeToggled.emit();

    expect(emitted).toBe(1);
    expect(component.themeIcon()).toBe('dark_mode');
  });
});
