# Tasks: Flutter Terminal POS V0

## 1. Backend readiness

- [ ] Ensure SellerTerminal actor resolution works.
- [ ] Ensure `/tenant/terminal/me` returns terminal summary.
- [ ] Define sale preview contract.
- [ ] Define sale confirm contract.
- [ ] Define receipt payload contract.
- [ ] Define recent tickets contract.
- [ ] Define reprint contract.
- [ ] Ensure all endpoints require `ACTOR_SELLER_TERMINAL`.

## 2. Flutter project

- [ ] Confirm Flutter Android target.
- [ ] Add environment config for API base URL and Firebase project.
- [ ] Add Firebase Auth integration.
- [ ] Add secure storage.
- [ ] Add Dio API client.
- [ ] Add Riverpod state management.
- [ ] Add routing.

## 3. Login

- [ ] Build terminal login screen.
- [ ] Support tenant code + terminal code + PIN/password.
- [ ] Authenticate through Firebase.
- [ ] Call backend terminal `me`.
- [ ] Store token/session safely.
- [ ] Handle blocked/disabled state.

## 4. Sale

- [ ] Build simple sale screen.
- [ ] Fetch active draws/games if available.
- [ ] Enter selection and stake.
- [ ] Preview sale.
- [ ] Confirm sale.
- [ ] Display result and print.

## 5. Printing

- [ ] Create `ReceiptPrinter` abstraction.
- [ ] Implement text receipt rendering.
- [ ] Integrate Android printer SDK/plugin for target device after device choice.
- [ ] Support reprint.
- [ ] Mark duplicate receipt on reprint if required.

## 6. Recent tickets

- [ ] Show recent tickets for current terminal only.
- [ ] Allow reprint own tickets.
- [ ] Do not show global admin sales.

## 7. Tests / validation

- [ ] Login success.
- [ ] Login invalid credentials.
- [ ] Blocked terminal cannot sell.
- [ ] Limit exceeded shown clearly.
- [ ] Cutoff passed shown clearly.
- [ ] Ticket prints after confirm.
- [ ] Reprint works.
- [ ] Token expiry returns to login.
