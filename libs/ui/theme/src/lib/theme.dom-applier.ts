import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { OverlayContainer } from '@angular/cdk/overlay';

@Injectable({ providedIn: 'root' })
export class ThemeDomApplier {
  constructor(private readonly overlay: OverlayContainer, @Inject(DOCUMENT) private doc: Document) {}

  applyPresetCss(presetId: string, css: string) {
    // 1. injecter/mettre à jour le <style id="tch-theme-base">
    let tag = this.doc.getElementById('tch-theme-base') as HTMLStyleElement | null;
    if (!tag) {
      tag = this.doc.createElement('style');
      tag.id = 'tch-theme-base';
      this.doc.head.appendChild(tag);
    }
    tag.innerHTML = css;

    // 2. marquer le root
    const root = this.getRoot();
    root.setAttribute('data-preset', presetId);
  }

  applyRuntimeState(opts: {
    mode: 'light' | 'dark';
    density: 0 | -1 | -2;
    overrides?: {
      vars?: Record<string, string>;
      fontHref?: string;
    };
  }) {
    const { mode, density, overrides } = opts;

    // mode light/dark
    const root = this.getRoot();
    root.classList.toggle('dark', mode === 'dark');
    this.overlay.getContainerElement().classList.toggle('dark', mode === 'dark');
    this.doc.documentElement.setAttribute('data-theme', mode);

    // density
    const cl = this.doc.documentElement.classList;
    ['mat-density-0', 'mat-density-1', 'mat-density-2'].forEach(c => cl.remove(c));
    cl.add(`mat-density-${density === 0 ? '0' : density === -1 ? '1' : '2'}`);

    // overrides.vars -> <style id="tch-theme-overrides">
    this.applyOverrides(overrides?.vars);

    // overrides.fontHref -> inject link if needed
    if (overrides?.fontHref) {
      this.ensureFontLoaded(overrides.fontHref);
    }
  }

  private applyOverrides(vars?: Record<string, string>) {
    let tag = this.doc.getElementById('tch-theme-overrides') as HTMLStyleElement | null;
    if (!vars || Object.keys(vars).length === 0) {
      if (tag) tag.innerHTML = '';
      return;
    }

    // séparer normal et dark
    const lightVars: Record<string, string> = {};
    const darkVars: Record<string, string> = {};
    for (const [key, value] of Object.entries(vars)) {
      if (key.startsWith('--dark:')) {
        darkVars[key.slice(7)] = value;
      } else {
        lightVars[key] = value;
      }
    }

    const lightCss = Object.entries(lightVars)
      .map(([k, v]) => `  ${k}: ${v};`)
      .join('\n');

    const darkCss = Object.entries(darkVars)
      .map(([k, v]) => `  ${k}: ${v};`)
      .join('\n');

    const cssOut = `.tch-theme {\n${lightCss}\n}\n` + `.tch-theme.dark {\n${darkCss}\n}`;

    if (!tag) {
      tag = this.doc.createElement('style');
      tag.id = 'tch-theme-overrides';
      this.doc.head.appendChild(tag);
    }
    tag.innerHTML = cssOut;
  }

  private ensureFontLoaded(href: string) {
    const id = 'tch-theme-font';
    if (this.doc.getElementById(id)) return;
    const link = this.doc.createElement('link');
    link.id = id;
    link.rel = 'stylesheet';
    link.href = href;
    this.doc.head.appendChild(link);
  }

  private getRoot(): HTMLElement {
    return (
      (this.doc.getElementById('theme-root') as HTMLElement) ||
      (this.doc.querySelector('.tch-theme') as HTMLElement) ||
      this.doc.body
    );
  }
}
