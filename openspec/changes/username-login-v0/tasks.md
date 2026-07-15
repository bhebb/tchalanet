# Tasks: Username login V0

- [x] Document current auth, support mode, and admin POS selling semantics.
- [x] Add OpenSpec proposal and behavior spec.
- [x] Align docs with SellerTerminal POS exception and current reset PIN behavior.
- [x] Product decision: temporary SellerTerminal PIN has no application TTL in V0.
- [x] Backend: add public username-to-resolved-identifier endpoint.
- [x] Backend: normalize username, avoid account enumeration, add local abuse protection, and add tests.
- [x] Web: change login field to "identifier or email" and lookup only on submit.
- [x] Web: keep email direct Firebase login and session restore without lookup.
- [ ] Validation: backend tests + focused web auth tests.
