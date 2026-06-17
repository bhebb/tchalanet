import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'maskSensitive', standalone: true, pure: true })
export class SensitiveDataMaskPipe implements PipeTransform {
  transform(
    value: string | null | undefined,
    type: 'phone' | 'email' | 'amount' = 'phone',
  ): string {
    if (value == null || value === '') return '';

    switch (type) {
      case 'phone': {
        // +509 3712 8899 → +509 37xx-xxxx
        const stripped = value.replace(/\s/g, '');
        if (stripped.length >= 7) {
          const prefix = stripped.slice(0, stripped.length - 6);
          const visible = stripped.slice(stripped.length - 6, stripped.length - 4);
          return `${prefix}${visible}xx-xxxx`;
        }
        return value;
      }
      case 'email': {
        const atIdx = value.indexOf('@');
        if (atIdx < 0) return value;
        const local = value.slice(0, atIdx);
        const domain = value.slice(atIdx);
        const visible = local.slice(0, Math.min(2, local.length));
        return `${visible}***${domain}`;
      }
      case 'amount':
        return '****';
      default:
        return value;
    }
  }
}
