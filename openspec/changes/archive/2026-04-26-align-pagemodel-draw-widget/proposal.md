# Proposal: Align PageModel Draw Widget

**Date**: 2026-04-26  
**Status**: COMPLETED / ARCHIVED  
**Author**: Agent

## 1. Context & Goals

The `DrawsProvider` in `features.pagemodel` was violating domain isolation rules by importing `core.drawresult.domain.model.DrawResult` and using admin queries. This led to incorrect labeling ("API" instead of slot names) and inefficient data retrieval.

This change aims to:

- Enforce strict domain isolation using public BFF queries.
- Fix functional bugs in draw result display.
- Standardize widget properties (`limit_per_slot` replacing `max_items`).
- Add architectural tests to prevent future violations.

## 2. Changes

### 2.1 Refactor

- `DrawsProvider`: Switched from `ListDrawResultsQuery` (admin) to `GetLatestPublicDrawResultsQuery` (BFF).
- Unified mapping to `PublicLatestDrawResultsResponse`.
- Implemented backward compatibility for `max_items` property.

### 2.2 Configuration

- Updated `public.home.json` to use the new `limit_per_slot` property.

### 2.3 Quality & Assurance

- `PageModelArchTest`: ArchUnit rules to forbid illegal imports in `features.pagemodel`.
- `DrawsProviderTest`: Unit tests for limits, fallback, and error handling.
