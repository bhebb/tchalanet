/**
 * Limit Policy Domain.
 *
 * This domain handles the evaluation and enforcement of betting limits to ensure responsible gambling.
 * It provides mechanisms to check stake limits, payout limits, and other financial constraints
 * before allowing transactions to proceed.
 *
 * Key components:
 * - LimitPolicy: Core domain entity representing a limit rule
 * - LimitEvaluator: Service for evaluating limits against transactions
 * - OperationType: Enumeration of transaction types (SALE, PAYOUT, CANCEL, etc.)
 * - BreachOutcome: Result of limit evaluation (ALLOW, WARN, BLOCK)
 */
package com.tchalanet.server.core.limitpolicy.domain;

