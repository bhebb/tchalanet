import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { OverlayContainer } from '@angular/cdk/overlay';
import { TchTheme } from '@tchl/types';

const STYLE_TAG_ID = 'tch-theme-vars';

@Injectable({ providedIn: 'root' })
export class ThemeDomApplier {
  constructor(private overlay: OverlayContainer, @Inject(DOCUMENT) private doc: Document) {}

  apply(theme: TchTheme, effMode: 'light' | 'dark', density: 0 | -1 | -2) {
    this.applyMaterialClass(theme.matClass);
    this.applyDensity(density);
    this.writeCssVars(theme, effMode);
    this.doc.documentElement.setAttribute('data-theme', effMode);
  }

  private applyMaterialClass(matClass?: string) {
    const body = this.doc.body.classList;
    const overlay = this.overlay.getContainerElement().classList;
    Array.from(body)
      .filter(c => c.startsWith('mat-'))
      .forEach(c => body.remove(c));
    Array.from(overlay)
      .filter(c => c.startsWith('mat-'))
      .forEach(c => overlay.remove(c));
    if (matClass) {
      body.add(matClass);
      overlay.add(matClass);
    }
  }

  private applyDensity(d: 0 | -1 | -2) {
    const cl = this.doc.documentElement.classList;
    ['mat-density-0', 'mat-density-1', 'mat-density-2'].forEach(c => cl.remove(c));
    cl.add(`mat-density-${d === 0 ? '0' : d === -1 ? '1' : '2'}`);
  }

  private writeCssVars(theme: TchTheme, effMode: 'light' | 'dark') {
    const vars = this.composeCssVars(theme, effMode);
    const css = `:root{\n${Object.entries(vars)
      .map(([k, v]) => `  ${k}:${v};`)
      .join('\n')}\n}`;
    let tag = this.doc.getElementById(STYLE_TAG_ID) as HTMLStyleElement | null;
    if (!tag) {
      tag = this.doc.createElement('style');
      tag.id = STYLE_TAG_ID;
      this.doc.head.appendChild(tag);
    }
    tag.innerHTML = css;
  }

  private composeCssVars(theme: TchTheme, effMode: 'light' | 'dark') {
    const p = theme.palette,
      t = theme.tokens ?? {};
    const base: Record<string, string> = {
      '--mat-sys-color-primary': p.primary,
      '--mat-sys-color-on-primary': p.onPrimary,
      '--mat-sys-color-surface': p.surface,
      '--mat-sys-color-on-surface': p.onSurface,
      '--mat-sys-color-outline': p.outline ?? 'rgba(0,0,0,.16)',
      '--tch-color-accent': p.accent ?? '#D84C51',
      '--tch-color-tertiary': p.tertiary ?? 'var(--tch-color-accent)',
      '--tch-header-bg': t.headerBg ?? 'var(--mat-sys-color-primary)',
      '--tch-header-fg': t.headerFg ?? 'var(--mat-sys-color-on-primary)',
    };
    if (p.surfaceContainer) base['--tch-surface-container'] = p.surfaceContainer;
    if (p.shape?.cornerRadius != null) base['--tch-shape-radius'] = `${p.shape.cornerRadius}px`;
    return { ...base, ...(theme.cssVars ?? {}) };
  }
}
