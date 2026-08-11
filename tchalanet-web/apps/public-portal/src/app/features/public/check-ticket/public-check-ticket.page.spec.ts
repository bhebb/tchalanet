/// <reference types="node" />

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { ProblemDetail } from '@tch/api';

import { extractPublicCodeFromQr, mapPublicTicketProblemStatus } from './public-ticket.service';
import { formatPublicCode, verificationCopy } from './public-check-ticket.utils';

describe('PublicCheckTicketPage helpers', () => {
  it('formats public ticket codes as uppercase grouped values', () => {
    expect(formatPublicCode('a1b2c3d4')).toBe('A1B2-C3D4');
    expect(formatPublicCode('ab cd-123456')).toBe('ABCD-1234');
    expect(formatPublicCode('@@@@')).toBe('');
  });

  it('maps every verification status to cautious public copy keys', () => {
    expect(verificationCopy('WINNING_PAYABLE')).toEqual({
      icon: 'task_alt',
      tone: 'success',
      titleKey: 'public.check.status.WINNING_PAYABLE.title',
      bodyKey: 'public.check.status.WINNING_PAYABLE.body',
    });
    expect(verificationCopy('SERVICE_UNAVAILABLE').titleKey).toBe(
      'public.check.status.SERVICE_UNAVAILABLE.title',
    );
    expect(verificationCopy('NOT_FOUND').tone).toBe('danger');
  });

  it('maps shared ProblemDetail fixtures to cautious public ticket-check statuses', () => {
    const validation = contractProblemFixture('problem-details/validation-failed.json');
    const denied = contractProblemFixture('problem-details/access-denied.json');
    const unexpected = contractProblemFixture('problem-details/unexpected-error.json');

    expect(mapPublicTicketProblemStatus(validation)).toBe('NOT_FOUND');
    expect(mapPublicTicketProblemStatus(denied)).toBe('SERVICE_UNAVAILABLE');
    expect(mapPublicTicketProblemStatus(unexpected)).toBe('SERVICE_UNAVAILABLE');
  });

  it('extracts manually entered public codes', () => {
    expect(extractPublicCodeFromQr('75nr-hpd8')).toBe('75NR-HPD8');
  });

  it('extracts public codes from check-ticket QR URL payloads', () => {
    expect(extractPublicCodeFromQr('https://tickets.test/public/check-ticket?code=75nr-hpd8')).toBe(
      '75NR-HPD8',
    );
  });

  it('extracts public codes from path-style QR URL payloads', () => {
    expect(extractPublicCodeFromQr('https://tickets.test/public/ticket/75NR-HPD8')).toBe(
      '75NR-HPD8',
    );
  });
});

function contractProblemFixture(relativePath: string): ProblemDetail {
  return JSON.parse(
    readFileSync(
      resolve(
        process.cwd(),
        '../tchalanet-server/testing/contracts/error-contract/v1',
        relativePath,
      ),
      'utf8',
    ),
  ) as ProblemDetail;
}
