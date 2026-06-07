import { DOCUMENT } from '@angular/common';
import { OverlayContainer } from '@angular/cdk/overlay';
import { Injectable, inject } from '@angular/core';

import { RuntimeTheme, ThemeDensity } from './theme-types';

const presetStyleElementId = 'tch-theme-preset';
const overrideStyleElementId = 'tch-theme-overrides';

@Injectable({ providedIn: 'root' })
export class ThemeDomApplier {
  private readonly document = inject(DOCUMENT);
  private readonly overlay = inject(OverlayContainer);

  apply(theme: RuntimeTheme, presetCss: string): void {
    const root = this.document.documentElement;
    const body = this.document.body;
    const overlayElement = this.overlay.getContainerElement();

    this.replaceStyle(presetStyleElementId, presetCss);
    this.replaceStyle(overrideStyleElementId, toOverrideCss(theme));

    root.dataset['theme'] = theme.effectiveMode;
    root.dataset['themePreference'] = theme.mode;
    root.dataset['themePreset'] = theme.activePresetKey;
    root.dataset['themeDensity'] = theme.density;
    root.classList.add('tch-theme');
    root.classList.toggle('dark', theme.effectiveMode === 'dark');
    applyDensity(root, theme.density);
    body.classList.add('tch-theme');
    body.dataset['preset'] = theme.activePresetKey;
    body.classList.toggle('dark', theme.effectiveMode === 'dark');
    applyDensity(body, theme.density);
    overlayElement.classList.add('tch-theme');
    overlayElement.dataset['preset'] = theme.activePresetKey;
    overlayElement.classList.toggle('dark', theme.effectiveMode === 'dark');
    applyDensity(overlayElement, theme.density);
  }

  private replaceStyle(id: string, css: string): void {
    let style = this.document.getElementById(id) as HTMLStyleElement | null;

    if (!style) {
      style = this.document.createElement('style');
      style.id = id;
      this.document.head.appendChild(style);
    }

    style.textContent = css;
  }
}

function applyDensity(element: HTMLElement, density: ThemeDensity): void {
  element.classList.toggle('tch-density-compact', density === 'compact');
  element.classList.toggle('tch-density-dense', density === 'dense');
}

function toOverrideCss(theme: RuntimeTheme): string {
  const tokens = Object.entries(theme.tokens);
  if (tokens.length === 0) {
    return '';
  }

  return `
    .tch-theme[data-preset='${escapeCssString(theme.activePresetKey)}'] {
      ${tokens.map(([key, value]) => `${key}: ${value};`).join('\n')}
    }
  `;
}

function escapeCssString(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}
