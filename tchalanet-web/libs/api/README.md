# API

Common backend/Web transport boundary.

## Owns

- transverse HTTP contracts such as `ApiResponse`, `ProblemDetail`, and `TchPage`;
- common response helpers and error mapping;
- common HTTP interceptors.

Surface-specific clients remain with their owning library until a stable shared client boundary
exists. This library does not own PageModel rendering, settings state, auth state, or UI.
