import {
  ConsoleActorIdentity,
  consoleActorPrimaryLabel,
  consoleActorSecondaryLabel,
  consoleActorStatusBadge,
  consoleActorTenantLabel,
  consoleSellerTerminalActorIdentity,
} from './console-actor-identity';

describe('consoleActorIdentity', () => {
  const actor = (overrides: Partial<ConsoleActorIdentity>): ConsoleActorIdentity => ({
    kind: 'TENANT_ADMIN',
    id: 'user-1',
    displayName: 'Marie Jean',
    email: 'marie@example.com',
    phone: null,
    status: 'ACTIVE',
    ...overrides,
  });

  it('uses display name, email, code, then id as stable primary label fallback', () => {
    expect(consoleActorPrimaryLabel(actor({ displayName: 'Marie Jean' }))).toBe('Marie Jean');
    expect(consoleActorPrimaryLabel(actor({ displayName: null, email: 'a@b.test' }))).toBe('a@b.test');
    expect(consoleActorPrimaryLabel(actor({ displayName: null, email: null, code: 'TERM-1' }))).toBe('TERM-1');
    expect(consoleActorPrimaryLabel(actor({ displayName: null, email: null, code: null }))).toBe('user-1');
  });

  it('builds seller-terminal secondary labels from code, email, and phone', () => {
    expect(consoleActorSecondaryLabel(actor({
      kind: 'SELLER_TERMINAL',
      code: 'TERM-1',
      email: 'terminal@example.com',
      phone: '+509',
    }))).toBe('TERM-1 · terminal@example.com · +509');
  });

  it('shows the username before the email for an administrator account', () => {
    expect(consoleActorSecondaryLabel(actor({ username: 'test.family' }))).toBe(
      'test.family · marie@example.com',
    );
  });

  it('resolves tenant labels without blanks', () => {
    expect(consoleActorTenantLabel(actor({ tenantName: 'Tchalanet', tenantCode: 'tch' }))).toBe('Tchalanet');
    expect(consoleActorTenantLabel(actor({ tenantName: null, tenantCode: 'tch' }))).toBe('tch');
  });

  it('maps actor status badges consistently', () => {
    expect(consoleActorStatusBadge('ACTIVE')).toBe('ready');
    expect(consoleActorStatusBadge('BLOCKED')).toBe('blocked');
    expect(consoleActorStatusBadge('DISABLED')).toBe('blocked');
    expect(consoleActorStatusBadge('PENDING')).toBe('pending');
    expect(consoleActorStatusBadge('SUSPENDED')).toBe('warning');
    expect(consoleActorStatusBadge('ARCHIVED')).toBe('missing');
  });

  it('builds seller-terminal identities from POS and admin DTO shapes', () => {
    expect(consoleSellerTerminalActorIdentity({
      sellerTerminalId: 'terminal-1',
      terminalCode: 'POS-1',
      displayName: 'Boutique Centre',
      status: 'ACTIVE',
    })).toEqual(expect.objectContaining({
      kind: 'SELLER_TERMINAL',
      id: 'terminal-1',
      code: 'POS-1',
      displayName: 'Boutique Centre',
    }));

    expect(consoleSellerTerminalActorIdentity({
      id: { value: 'terminal-2' },
      terminalCode: 'POS-2',
      displayName: 'Boutique Nord',
      status: 'BLOCKED',
    }).id).toBe('terminal-2');
  });
});
