import { describe, expect, it } from 'vitest';

import {
  SELLER_TERMINAL_CREATE_FIELD_TARGETS,
  sellerTerminalServerFieldError,
} from './seller-terminal-error-targets';

describe('sellerTerminalServerFieldError', () => {
  const errors = {
    displayName: { value: 'Boutik Lakay', message: 'Ce nom est deja utilise.' },
  };

  it('keeps the server error on the rejected value', () => {
    expect(sellerTerminalServerFieldError(errors, 'displayName', 'Boutik Lakay')).toEqual({
      kind: 'server',
      message: 'Ce nom est deja utilise.',
    });
  });

  it('clears the server error as soon as the field changes', () => {
    expect(sellerTerminalServerFieldError(errors, 'displayName', 'Boutik Lakay 2')).toBeUndefined();
  });

  it('maps nested backend targets to the corresponding signal form field', () => {
    expect(SELLER_TERMINAL_CREATE_FIELD_TARGETS['sellerTerminal.address.city']).toBe(
      'address.city',
    );
  });
});
