import { formatPublicCode, verificationCopy } from './public-check-ticket.page';

describe('PublicCheckTicketPage helpers', () => {
  it('formats public ticket codes as uppercase grouped values', () => {
    expect(formatPublicCode('a1b2c3d4')).toBe('A1B2-C3D4');
    expect(formatPublicCode('ab cd-123456')).toBe('ABCD-123-456');
    expect(formatPublicCode('@@@@')).toBe('');
  });

  it('maps every verification status to cautious public copy keys', () => {
    expect(verificationCopy('PAYABLE')).toEqual({
      icon: 'task_alt',
      tone: 'success',
      titleKey: 'public.check.status.PAYABLE.title',
      bodyKey: 'public.check.status.PAYABLE.body',
    });
    expect(verificationCopy('SERVICE_UNAVAILABLE').titleKey).toBe(
      'public.check.status.SERVICE_UNAVAILABLE.title',
    );
    expect(verificationCopy('NOT_FOUND').tone).toBe('danger');
  });
});
