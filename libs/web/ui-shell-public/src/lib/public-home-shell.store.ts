import { Injectable, signal } from '@angular/core';
import { FooterModel, HeaderModel } from 'shared/types';


@Injectable()
export class PublicHomeShellStore {
  readonly header = signal<HeaderModel>({
    logoUrl: '/assets/brand/logo.svg',
    menu: [],
    langs: ['fr','en'],
    currentLang: 'fr',
  });

  readonly footer = signal<FooterModel>({
    columns: [],
    note: '© 2025 Tchalanet',
  });

  // constructor(private i18n: I18nService) {}

  setHeader(h: HeaderModel) { this.header.set(h); }
  setFooter(f: FooterModel) { this.footer.set(f); }

  onLangChange(lang: string) {
    // this.i18n.set(lang as any);
    const h = this.header();
    this.header.set({ ...h, currentLang: lang });
  }
}
