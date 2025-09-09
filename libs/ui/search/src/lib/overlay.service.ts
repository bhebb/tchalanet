import { Injectable, signal } from '@angular/core';

export interface SearchOverlayHandle {
  show: () => void;
  close: () => void;
}
@Injectable({ providedIn: 'root' })
export class OverlayService {
  private handle?: SearchOverlayHandle;
  private _open = signal(false);
  isOpen = this._open.asReadonly();

  register(handle: SearchOverlayHandle) {
    this.handle = handle;
    if (this._open()) this.handle.show();
  }

  show() {
    this._open.set(true);
    this.handle?.show();
  }

  close() {
    this._open.set(false);
    this.handle?.close();
    document.documentElement.classList.remove('search-open');
  }

  isRegistered() {
    return !!this.handle;
  }
}
