# Spec: Test Levels

## ADDED Requirements

### Requirement: E2E levels are explicit

The suite SHALL define boot smoke, daily smoke, business critical, concurrency correctness and future performance/load levels.

### Requirement: Performance is out of E2E V1

E2E SHALL NOT assert throughput, p95/p99 latency, max users, DB saturation or stress capacity.
